package com.helldeck.llm

data class GenConfig(
    val maxTokens: Int = 64,
    val temperature: Float = 0.4f,
    val topP: Float = 0.9f,
    val seed: Int? = null,
    val stop: List<String> = emptyList(),
    val grammar: String? = null,
)

interface LocalLLM {
    val modelId: String
    val isReady: Boolean
    suspend fun generate(system: String, user: String, cfg: GenConfig = GenConfig()): String
    suspend fun classifyZeroShot(text: String, labels: List<String>, cfg: GenConfig = GenConfig()): Int
}
