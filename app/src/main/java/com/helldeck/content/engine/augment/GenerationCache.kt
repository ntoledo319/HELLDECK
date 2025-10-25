package com.helldeck.content.engine.augment

import com.helldeck.content.db.HelldeckDb
import java.security.MessageDigest

class GenerationCache(private val db: HelldeckDb) {
    fun key(task: String, model: String, templateId: String, fillHash: String, seed: Int): String =
        sha1("$task|$model|$templateId|$fillHash|$seed")

    suspend fun get(key: String): String? = db.generatedTextDao().get(key)?.text

    suspend fun put(key: String, text: String) {
        db.generatedTextDao().insert(GeneratedTextEntity(key, text, System.currentTimeMillis()))
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}