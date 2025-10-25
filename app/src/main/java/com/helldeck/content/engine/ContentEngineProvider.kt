package com.helldeck.content.engine

import android.content.Context
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.engine.augment.GenerationCache
import com.helldeck.content.engine.augment.Validator
import com.helldeck.content.util.SeededRng
import com.helldeck.llm.LocalLLM
import com.helldeck.llm.llamacpp.LlamaCppLLM
import java.io.File
import kotlin.random.Random

object ContentEngineProvider {

    @Volatile
    private var INSTANCE: GameEngine? = null
    private lateinit var contentRepository: ContentRepository
    private var localLLM: LocalLLM? = null

    fun get(context: Context, sessionSeed: Long = System.currentTimeMillis()): GameEngine =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildGameEngine(context, sessionSeed).also { INSTANCE = it }
        }

    private fun buildGameEngine(context: Context, sessionSeed: Long): GameEngine {
        contentRepository = ContentRepository(context.applicationContext)
        contentRepository.initialize()

        val rng = SeededRng(sessionSeed)
        val selector = ContextualSelector(contentRepository, Random(sessionSeed))

        localLLM = initializeLocalLLM(context)

        val profanityList = setOf("example") 
        val validator = Validator(profanityList, maxSpice = 3)
        val cache = GenerationCache(contentRepository.db)
        val augmentor = if (localLLM != null) {
            Augmentor(localLLM, cache, validator)
        } else {
            null
        }

        return GameEngine(
            repo = contentRepository,
            rng = rng,
            selector = selector,
            augmentor = augmentor,
            modelId = localLLM?.modelId ?: "none"
        )
    }

    private fun initializeLocalLLM(context: Context): LocalLLM? {
        val filesDir = context.filesDir
        val modelsDir = File(filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val defaultModelFile = File(modelsDir, "qwen2.5-1.5b-instruct-Q4_K_M.gguf")
        if (defaultModelFile.exists()) {
            return try {
                LlamaCppLLM(defaultModelFile, ctxSize = 2048)
            } catch (e: Exception) {
                null
            }
        }

        val liteModelFile = File(modelsDir, "tinyllama-1.1b-chat-Q4_K_M.gguf")
        if (liteModelFile.exists()) {
            return try {
                LlamaCppLLM(liteModelFile, ctxSize = 1024)
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    fun getLocalLLM(): LocalLLM? = localLLM

    fun isAIEnhancementAvailable(): Boolean = localLLM?.isReady == true

    fun reset() {
        synchronized(this) {
            INSTANCE = null
            localLLM = null
        }
    }
}