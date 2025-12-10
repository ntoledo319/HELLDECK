package com.helldeck.content.generator

/**
 * ToneGate provides a simple allow/deny check for lexicon entries based on tone profiles.
 * This is a minimal placeholder; production rules can be driven by assets/model/tone_gate.yaml.
 */
class ToneGate(private val allowedTones: Set<String> = emptySet(), private val deniedTones: Set<String> = emptySet()) {
    fun allows(tone: String): Boolean {
        val t = tone.lowercase()
        if (deniedTones.contains(t)) return false
        if (allowedTones.isEmpty()) return true
        return allowedTones.contains(t)
    }
}

