package com.helldeck.content.generator

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CardLabBanlist(
    val bannedBlueprints: Set<String> = emptySet(),
    val bannedLexiconItems: Map<String, Set<String>> = emptyMap(), // slotType -> set of text values
) {
    fun isBlueprintBanned(blueprintId: String): Boolean = blueprintId in bannedBlueprints

    fun isLexiconItemBanned(slotType: String, text: String): Boolean =
        bannedLexiconItems[slotType]?.contains(text.lowercase()) ?: false

    fun withBannedBlueprint(blueprintId: String): CardLabBanlist =
        copy(bannedBlueprints = bannedBlueprints + blueprintId)

    fun withBannedLexiconItem(slotType: String, text: String): CardLabBanlist {
        val currentSet = bannedLexiconItems[slotType] ?: emptySet()
        return copy(bannedLexiconItems = bannedLexiconItems + (slotType to (currentSet + text.lowercase())))
    }

    fun withoutBannedBlueprint(blueprintId: String): CardLabBanlist =
        copy(bannedBlueprints = bannedBlueprints - blueprintId)

    fun withoutBannedLexiconItem(slotType: String, text: String): CardLabBanlist {
        val currentSet = bannedLexiconItems[slotType] ?: return this
        val newSet = currentSet - text.lowercase()
        return if (newSet.isEmpty()) {
            copy(bannedLexiconItems = bannedLexiconItems - slotType)
        } else {
            copy(bannedLexiconItems = bannedLexiconItems + (slotType to newSet))
        }
    }

    companion object {
        private const val FILENAME = "cardlab_banlist.json"
        private val json = Json { prettyPrint = true }

        fun load(context: Context): CardLabBanlist {
            return try {
                val file = File(context.cacheDir, FILENAME)
                if (file.exists()) {
                    val content = file.readText()
                    json.decodeFromString<CardLabBanlist>(content)
                } else {
                    CardLabBanlist()
                }
            } catch (e: Exception) {
                // If loading fails, return empty banlist
                CardLabBanlist()
            }
        }

        fun save(context: Context, banlist: CardLabBanlist) {
            try {
                val file = File(context.cacheDir, FILENAME)
                val content = json.encodeToString(banlist)
                file.writeText(content)
            } catch (e: Exception) {
                // Silently fail - banlist is session-only feature
            }
        }

        fun clear(context: Context) {
            try {
                val file = File(context.cacheDir, FILENAME)
                file.delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
