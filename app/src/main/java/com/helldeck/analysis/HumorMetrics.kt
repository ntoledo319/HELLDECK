package com.helldeck.analysis

data class HumorMetrics(
    val humorScore: Double? = null,
    val absurdity: Double? = null,
    val shockValue: Double? = null,
    val relatability: Double? = null,
    val cringeFactor: Double? = null,
    val benignViolation: Double? = null
)

object HumorMetricsUtils {
    fun fromMetadata(meta: Map<String, Any?>): HumorMetrics = HumorMetrics(
        humorScore = (meta["humorScore"] as? Number)?.toDouble(),
        absurdity = (meta["absurdity"] as? Number)?.toDouble(),
        shockValue = (meta["shockValue"] as? Number)?.toDouble(),
        relatability = (meta["relatability"] as? Number)?.toDouble(),
        cringeFactor = (meta["cringeFactor"] as? Number)?.toDouble(),
        benignViolation = (meta["benignViolation"] as? Number)?.toDouble()
    )
}

