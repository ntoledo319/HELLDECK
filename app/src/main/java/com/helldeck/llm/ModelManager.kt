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

        // Prioritize Qwen2.5 if available
        val defaultModelFile = File(modelsDir, "qwen2.5-1.5b-instruct-Q4_K_M.gguf")
        if (defaultModelFile.exists()) {
            return try {
                LlamaCppLLM(defaultModelFile, ctxSize = 2048)
            } catch (e: Exception) {
                null
            }
        }

        // Fallback to TinyLlama
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
}