package com.helldeck.llm.llamacpp

import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaCppLLM(
    private val modelFile: File,
    private val ctxSize: Int = 2048
) : LocalLLM {
    override val modelId: String = modelFile.nameWithoutExtension
    @Volatile override var isReady: Boolean = false
        private set

    init {
        // TODO: JNI bridge init; load model into memory (once per session)
        // LlamaNative.init(modelFile.absolutePath, ctxSize)
        isReady = true
    }

    override suspend fun generate(system: String, user: String, cfg: GenConfig): String = withContext(Dispatchers.Default) {
        val prompt = buildPrompt(system, user)
        // return LlamaNative.generate(prompt, cfg)
        ""
    }

    override suspend fun classifyZeroShot(text: String, labels: List<String>, cfg: GenConfig): Int {
        val user = "Pick the best label index only.\nLabels:\n" + labels.mapIndexed { i, l -> "[$i] $l" }.joinToString("\n") +
                "\nText: $text\nAnswer with a single integer."
        val out = generate(system = "You are a strict classifier.", user = user, cfg = cfg.copy(maxTokens = 4, temperature = 0f))
        return out.trim().take(2).filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun buildPrompt(system: String, user: String): String = """
        <|system|>
        $system
        <|user|>
        $user
        <|assistant|>
    """.trimIndent()
}