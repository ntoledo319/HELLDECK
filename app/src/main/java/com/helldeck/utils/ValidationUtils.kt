package com.helldeck.utils

import com.helldeck.content.model.Player

/**
 * Centralized validation utilities for HELLDECK.
 * Ensures consistent validation rules across all input fields.
 * 
 * @ai_prompt Use ValidationUtils for all user input validation
 * @context_boundary Single source of truth for validation rules
 * 
 * ORIGINAL_INTENT: Prevent duplicate validation logic across 3+ player creation sites
 */
object ValidationUtils {
    
    // Player validation constants
    const val MIN_PLAYER_NAME_LENGTH = 1
    const val MAX_PLAYER_NAME_LENGTH = 32
    const val MIN_PLAYERS_FOR_GAME = 2
    const val MAX_PLAYERS_RECOMMENDED = 16
    const val MAX_PLAYERS_ABSOLUTE = 25
    const val TEAM_MODE_THRESHOLD = 8
    
    // Crew brain validation
    const val MIN_CREW_BRAIN_NAME_LENGTH = 1
    const val MAX_CREW_BRAIN_NAME_LENGTH = 40
    const val MAX_CREW_BRAINS = 10
    
    /**
     * Validation result with success flag and error message
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
    ) {
        companion object {
            fun success() = ValidationResult(true, null)
            fun failure(message: String) = ValidationResult(false, message)
        }
    }
    
    /**
     * Validate player name for creation or editing.
     * 
     * Rules:
     * - Must not be blank
     * - Length between 1-32 characters
     * - Must not be duplicate of existing player (case-insensitive)
     * - Must not contain only whitespace
     */
    fun validatePlayerName(
        name: String,
        existingPlayers: List<Player>,
        excludePlayerId: String? = null,
    ): ValidationResult {
        val trimmed = name.trim()
        
        if (trimmed.isEmpty()) {
            return ValidationResult.failure("Player name cannot be empty")
        }
        
        if (trimmed.length < MIN_PLAYER_NAME_LENGTH) {
            return ValidationResult.failure("Player name is too short")
        }
        
        if (trimmed.length > MAX_PLAYER_NAME_LENGTH) {
            return ValidationResult.failure("Player name is too long (max $MAX_PLAYER_NAME_LENGTH characters)")
        }
        
        // Check for duplicate names (case-insensitive)
        val duplicate = existingPlayers.find { player ->
            player.name.trim().equals(trimmed, ignoreCase = true) && 
            player.id != excludePlayerId
        }
        
        if (duplicate != null) {
            return ValidationResult.failure("Player \"${duplicate.name}\" already exists")
        }
        
        return ValidationResult.success()
    }
    
    /**
     * Validate emoji selection for player avatar.
     * 
     * Rules:
     * - Must be a valid emoji or emoji sequence
     * - Length should be reasonable (1-4 characters to handle combined emojis)
     */
    fun validatePlayerEmoji(emoji: String): ValidationResult {
        val trimmed = emoji.trim()
        
        if (trimmed.isEmpty()) {
            return ValidationResult.failure("Please select an emoji")
        }
        
        if (trimmed.length > 4) {
            return ValidationResult.failure("Emoji is too long")
        }
        
        return ValidationResult.success()
    }
    
    /**
     * Check if emoji is already used by another player.
     * This is a warning, not a hard validation failure.
     */
    fun isEmojiDuplicate(
        emoji: String,
        existingPlayers: List<Player>,
        excludePlayerId: String? = null,
    ): Boolean {
        return existingPlayers.any { player ->
            player.avatar.trim() == emoji.trim() && player.id != excludePlayerId
        }
    }
    
    /**
     * Validate player count for starting a game.
     * 
     * Rules:
     * - Minimum 2 players
     * - Recommended max 16 players
     * - Absolute max 25 players
     * - Team mode suggested at 8+ players
     */
    fun validatePlayerCount(count: Int): ValidationResult {
        when {
            count < MIN_PLAYERS_FOR_GAME -> {
                return ValidationResult.failure(
                    "Need at least $MIN_PLAYERS_FOR_GAME players to start a game"
                )
            }
            count == 0 -> {
                return ValidationResult.failure("No players added yet. Add players to start!")
            }
            count > MAX_PLAYERS_ABSOLUTE -> {
                return ValidationResult.failure(
                    "Too many players! Maximum is $MAX_PLAYERS_ABSOLUTE"
                )
            }
        }
        
        return ValidationResult.success()
    }
    
    /**
     * Get warning message for player count if applicable.
     * Returns null if no warning needed.
     */
    fun getPlayerCountWarning(count: Int): String? {
        return when {
            count >= TEAM_MODE_THRESHOLD && count < 11 -> 
                "💡 Tip: With $count players, you can enable team mode for faster voting"
            count >= 11 -> 
                "⚠️ With $count players, team mode is recommended (1 vote per team)"
            count > MAX_PLAYERS_RECOMMENDED -> 
                "⚠️ Performance may degrade with ${count} players"
            else -> null
        }
    }
    
    /**
     * Validate crew brain name.
     */
    fun validateCrewBrainName(name: String): ValidationResult {
        val trimmed = name.trim()
        
        if (trimmed.isEmpty()) {
            return ValidationResult.failure("Crew brain name cannot be empty")
        }
        
        if (trimmed.length < MIN_CREW_BRAIN_NAME_LENGTH) {
            return ValidationResult.failure("Crew brain name is too short")
        }
        
        if (trimmed.length > MAX_CREW_BRAIN_NAME_LENGTH) {
            return ValidationResult.failure("Crew brain name is too long (max $MAX_CREW_BRAIN_NAME_LENGTH characters)")
        }
        
        return ValidationResult.success()
    }
    
    /**
     * Validate crew brain emoji.
     */
    fun validateCrewBrainEmoji(emoji: String): ValidationResult {
        val trimmed = emoji.trim()
        
        if (trimmed.isEmpty()) {
            return ValidationResult.failure("Please select an emoji")
        }
        
        if (trimmed.length > 4) {
            return ValidationResult.failure("Emoji is too long")
        }
        
        return ValidationResult.success()
    }
    
    /**
     * Generate unique player ID with collision prevention.
     * Uses timestamp + random component for better uniqueness.
     */
    fun generateUniquePlayerId(existingPlayers: List<Player>): String {
        val existingIds = existingPlayers.map { it.id }.toSet()
        var attempts = 0
        val maxAttempts = 100
        
        while (attempts < maxAttempts) {
            // Use timestamp + random for better uniqueness
            val timestamp = System.currentTimeMillis() % 100000
            val random = kotlin.random.Random.nextInt(10000, 99999)
            val id = "p${timestamp}_${random}"
            
            if (id !in existingIds) {
                return id
            }
            
            attempts++
        }
        
        // Fallback: UUID-based (should never reach here)
        return "p_${java.util.UUID.randomUUID().toString().take(12)}"
    }
}
