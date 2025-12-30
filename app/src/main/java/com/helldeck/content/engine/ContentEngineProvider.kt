package com.helldeck.content.engine

import android.content.Context
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.engine.augment.GenerationCache
import com.helldeck.content.engine.augment.Validator
import com.helldeck.content.generator.BlueprintRepositoryV3
import com.helldeck.content.generator.CardGeneratorV3
import com.helldeck.content.generator.CardLabBanlist
import com.helldeck.content.generator.GeneratorArtifacts
import com.helldeck.content.generator.LLMCardGeneratorV2
import com.helldeck.content.generator.LexiconRepositoryV2
import com.helldeck.content.generator.gold.GoldBank
import com.helldeck.content.util.SeededRng
import com.helldeck.content.validation.AssetValidator
import com.helldeck.engine.Config
import com.helldeck.llm.LocalLLM
import com.helldeck.llm.llamacpp.LlamaCppLLM
import com.helldeck.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.random.Random

object ContentEngineProvider {

    @Volatile
    private var INSTANCE: GameEngine? = null
    private lateinit var contentRepository: ContentRepository
    private var localLLM: LocalLLM? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var llmPrepJob: Job? = null
    private var cardGeneratorV3: CardGeneratorV3? = null
    private var llmCardGeneratorV2: LLMCardGeneratorV2? = null

    private data class ModelAsset(val filename: String, val ctxSize: Int)

    private val knownModels = listOf(
        // Prefer Qwen; include common filename variants found in assets
        ModelAsset("qwen2.5-1.5b-instruct-Q4_K_M.gguf", 2048),
        ModelAsset("qwen2.5-1.5b-instruct-q4_k_m.gguf", 2048),
        // TinyLlama variants
        ModelAsset("tinyllama-1.1b-chat-Q4_K_M.gguf", 1024),
        ModelAsset("tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf", 1024),
    )

    fun get(context: Context, sessionSeed: Long = System.currentTimeMillis()): GameEngine =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildGameEngine(context, sessionSeed).also { INSTANCE = it }
        }

    private fun buildGameEngine(context: Context, sessionSeed: Long): GameEngine {
        contentRepository = ContentRepository(context.applicationContext)
        contentRepository.initialize()

        // Validate V3 assets at boot
        val validationResult = AssetValidator.validateAll(context, strictMode = false)
        AssetValidator.logValidationResult(validationResult)

        // Force gold-only mode on critical validation errors
        if (!validationResult.isValid) {
            Logger.w("Asset validation failed; forcing gold-only mode for safety")
            Config.setSafeModeGoldOnly(true)
            Config.setEnableV3Generator(false)
        }

        val rng = SeededRng(sessionSeed)
        val selector = ContextualSelector(contentRepository, Random(sessionSeed))

        localLLM = initializeLocalLLM(context)

        val profanityList = runCatching {
            // Reuse generator's banned tokens/phrases for augmentation safety
            val text = context.assets.open("model/banned.json").bufferedReader().use { it.readText() }
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val node = json.parseToJsonElement(text).jsonObject
            val tokens = node["tokens"]?.jsonArray?.mapNotNull { it.toString().trim('"') } ?: emptyList()
            val phrases = node["phrases"]?.jsonArray?.mapNotNull { it.toString().trim('"') } ?: emptyList()
            (tokens + phrases).toSet()
        }.getOrDefault(emptySet())
        val validator = Validator(profanityList, maxSpice = 3)
        val cache = GenerationCache(contentRepository.db)
        val augmentor = if (localLLM != null) {
            Augmentor(localLLM, cache, validator)
        } else {
            null
        }

        cardGeneratorV3 = runCatching {
            val blueprintRepo = BlueprintRepositoryV3(context.assets)
            val lexiconRepo = LexiconRepositoryV2(context.assets)
            val artifacts = GeneratorArtifacts(context.assets)
            val goldBank = GoldBank(context.assets)
            val banlist = CardLabBanlist.load(context)
            val semanticValidator = runCatching {
                com.helldeck.content.validation.SemanticValidator(context.assets)
            }.getOrNull()
            CardGeneratorV3(blueprintRepo, lexiconRepo, artifacts, goldBank, banlist, semanticValidator)
        }.getOrElse {
            Logger.w("Card generator V3 unavailable: ${it.message}")
            null
        }

        // Initialize quality-first LLM card generator
        llmCardGeneratorV2 = if (cardGeneratorV3 != null) {
            runCatching {
                LLMCardGeneratorV2(localLLM, context.applicationContext, cardGeneratorV3!!)
            }.getOrElse {
                Logger.w("LLM Card Generator V2 unavailable: ${it.message}")
                null
            }
        } else {
            null
        }

        // Seed selector priors from persisted stats for lasting quality
        runBlocking {
            try {
                val stats = contentRepository.db.templateStatDao().getAll()
                val priors = stats.associate { s ->
                    // Alpha ~ successes, Beta ~ failures; add small prior to avoid zero
                    val a = 1.0 + s.rewardSum
                    val b = 1.0 + (s.visits - s.rewardSum).coerceAtLeast(0.0)
                    s.templateId to (a to b)
                }
                if (priors.isNotEmpty()) selector.seed(priors)
            } catch (_: Exception) { /* ignore */ }
        }

        return GameEngine(
            repo = contentRepository,
            rng = rng,
            selector = selector,
            augmentor = augmentor,
            modelId = localLLM?.modelId ?: "none",
            cardGeneratorV3 = cardGeneratorV3,
            llmCardGeneratorV2 = llmCardGeneratorV2,
        )
    }

    private fun initializeLocalLLM(context: Context): LocalLLM? {
        // Opt-in proxy LLM for JVM tests (no device/emulator needed)
        val mode = System.getProperty("HELDECK_LLM_MODE") ?: System.getenv("HELDECK_LLM_MODE")
        if (mode?.equals("proxy", ignoreCase = true) == true) {
            return try {
                com.helldeck.llm.proxy.ProxyLLM()
            } catch (_: Throwable) { null }
        }
        val filesDir = context.filesDir
        val modelsDir = File(filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val ready = localLLM?.takeIf { it.isReady }
        if (ready != null) return ready

        val existing = knownModels.firstNotNullOfOrNull { asset ->
            val file = File(modelsDir, asset.filename)
            if (file.exists()) asset to file else null
        }

        if (existing != null) {
            val (asset, file) = existing
            tryInitModel(file, asset.ctxSize)?.let { llm ->
                localLLM = llm
                return llm
            }
        }

        scheduleModelPreparation(context.applicationContext, modelsDir)
        return null
    }

    fun getLocalLLM(): LocalLLM? = localLLM

    fun isAIEnhancementAvailable(): Boolean = localLLM?.isReady == true

    fun reset() {
        synchronized(this) {
            INSTANCE = null
            (localLLM as? LlamaCppLLM)?.close()
            localLLM = null
            llmPrepJob?.cancel()
            llmPrepJob = null
            cardGeneratorV3 = null
            llmCardGeneratorV2 = null
        }
    }

    fun updateBanlist(context: Context, banlist: CardLabBanlist) {
        cardGeneratorV3?.setBanlist(banlist)
    }

    private fun tryInitModel(file: File, ctxSize: Int): LocalLLM? {
        return try {
            val llm = LlamaCppLLM(file, ctxSize)
            if (llm.isReady) llm else null
        } catch (e: Exception) {
            Logger.e("Failed to initialise local LLM from ${file.name}", e)
            null
        }
    }

    private fun scheduleModelPreparation(context: Context, modelsDir: File) {
        if (llmPrepJob?.isActive == true) return
        llmPrepJob = scope.launch {
            val assetSet = runCatching { context.assets.list("models")?.toSet() ?: emptySet() }
                .getOrDefault(emptySet())
            val candidate = knownModels.firstOrNull { assetSet.contains(it.filename) } ?: return@launch
            val target = File(modelsDir, candidate.filename)
            if (!target.exists()) {
                Logger.i("Copying ${candidate.filename} to models dir")
                copyAsset(context, candidate.filename, target)
            }
            val llm = tryInitModel(target, candidate.ctxSize)
            if (llm?.isReady == true) {
                localLLM = llm
                Logger.i("Local LLM initialised asynchronously: ${llm.modelId}")
            }
        }
    }

    private suspend fun copyAsset(context: Context, assetName: String, target: File) {
        withContext(Dispatchers.IO) {
            context.assets.open("models/$assetName").use { ins ->
                target.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            }
        }
    }
}
