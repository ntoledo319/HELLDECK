package com.helldeck.llm

import android.content.Context
import com.helldeck.llm.llamacpp.LlamaCppLLM
import java.io.File

object ModelManager {

    fun initialize(context: Context): LocalLLM? {
        val filesDir = context.filesDir
        val modelsDir = File(filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        // Prioritize Qwen2.5 if available (check common filename variants)
        val qwenCandidates = listOf(
            "qwen2.5-1.5b-instruct-Q4_K_M.gguf",
            "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        )
        val defaultModelFile = qwenCandidates.map { File(modelsDir, it) }.firstOrDefault()
        if (defaultModelFile != null && defaultModelFile.exists()) {
            return try {
                LlamaCppLLM(defaultModelFile, ctxSize = 2048)
            } catch (e: Exception) {
                null
            }
        }

        // Fallback to TinyLlama (several variants)
        val tinyCandidates = listOf(
            "tinyllama-1.1b-chat-Q4_K_M.gguf",
            "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        )
        val liteModelFile = tinyCandidates.map { File(modelsDir, it) }.firstOrDefault()
        if (liteModelFile != null && liteModelFile.exists()) {
            return try {
                LlamaCppLLM(liteModelFile, ctxSize = 1024)
            } catch (e: Exception) {
                null
            }
        }

        return null
    }
}

private fun List<File>.firstOrDefault(): File? = this.firstOrNull { it.exists() }
