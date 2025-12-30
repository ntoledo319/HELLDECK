package com.helldeck.llm.llamacpp

import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM
import com.helldeck.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class LlamaCppLLM(
    private val modelFile: File,
    private val ctxSize: Int = 2048,
) : LocalLLM {
    override val modelId: String = modelFile.nameWithoutExtension

    @Volatile override var isReady: Boolean = false
        private set

    private var handle: Long = 0L

    init {
        try {
            if (LlamaNativeBridge.isAvailable()) {
                handle = LlamaNativeBridge.init(modelFile.absolutePath, ctxSize)
                if (handle != 0L) {
                    isReady = true
                    Logger.i("LlamaCppLLM ready (${modelFile.name})")
                } else {
                    Logger.w("LlamaCppLLM failed to acquire native handle for ${modelFile.name}")
                }
            } else {
                Logger.w("helldeck_llama native library unavailable; running without paraphrasing")
            }
        } catch (e: Exception) {
            Logger.e("Failed to initialise LlamaCppLLM for ${modelFile.name}", e)
        }
    }

    override suspend fun generate(system: String, user: String, cfg: GenConfig): String = withContext(
        Dispatchers.Default,
    ) {
        val prompt = buildPrompt(system, user)
        if (!isReady || handle == 0L) return@withContext user
        val seed = cfg.seed ?: Random(System.nanoTime()).nextInt()
        try {
            val text = LlamaNativeBridge.generate(
                handle = handle,
                prompt = prompt,
                maxTokens = cfg.maxTokens,
                temperature = cfg.temperature,
                topP = cfg.topP,
                seed = seed,
            )
            if (text.isBlank()) user else text
        } catch (e: Exception) {
            Logger.e("LlamaCppLLM generation failed", e)
            user
        }
    }

    override suspend fun classifyZeroShot(text: String, labels: List<String>, cfg: GenConfig): Int {
        val user = "Pick the best label index only.\nLabels:\n" + labels.mapIndexed { i, l -> "[$i] $l" }.joinToString("\n") +
            "\nText: $text\nAnswer with a single integer."
        val out =
            generate(
                system = "You are a strict classifier.",
                user = user,
                cfg = cfg.copy(maxTokens = 4, temperature = 0f),
            )
        return out.trim().take(2).filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    fun close() {
        if (handle != 0L) {
            runCatching { LlamaNativeBridge.free(handle) }
            handle = 0L
            isReady = false
        }
    }

    private fun buildPrompt(system: String, user: String): String = """
        <|system|>
        $system
        <|user|>
        $user
        <|assistant|>
    """.trimIndent()

    private object LlamaNativeBridge {
        @Volatile
        private var triedLoad = false

        @Volatile
        private var available = false

        fun isAvailable(): Boolean {
            if (!triedLoad) {
                synchronized(this) {
                    if (!triedLoad) {
                        triedLoad = true
                        available = try {
                            System.loadLibrary("helldeck_llama")
                            true
                        } catch (e: UnsatisfiedLinkError) {
                            Logger.w("helldeck_llama native library not present", e)
                            false
                        }
                    }
                }
            }
            return available
        }

        fun init(modelPath: String, contextSize: Int): Long {
            if (!isAvailable()) return 0L
            return try {
                nativeInit(modelPath, contextSize)
            } catch (e: UnsatisfiedLinkError) {
                available = false
                Logger.e("helldeck_llama native init missing", e)
                0L
            }
        }

        fun generate(handle: Long, prompt: String, maxTokens: Int, temperature: Float, topP: Float, seed: Int): String {
            if (!isAvailable() || handle == 0L) return ""
            return try {
                nativeGenerate(handle, prompt, maxTokens, temperature, topP, seed)
            } catch (e: UnsatisfiedLinkError) {
                available = false
                Logger.e("helldeck_llama native generate missing", e)
                ""
            }
        }

        fun free(handle: Long) {
            if (!isAvailable() || handle == 0L) return
            runCatching { nativeFree(handle) }
        }

        @JvmStatic
        private external fun nativeInit(modelPath: String, contextSize: Int): Long

        @JvmStatic
        private external fun nativeGenerate(
            handle: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            seed: Int,
        ): String

        @JvmStatic
        private external fun nativeFree(handle: Long)
    }
}
