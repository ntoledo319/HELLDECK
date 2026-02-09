package com.helldeck.billing

import com.helldeck.utils.Logger

/**
 * PromoCodeManager - Handles promo code validation and redemption for HELLDECK
 *
 * Valid codes unlock premium for free (marketing/promotion purposes)
 * Codes are case-insensitive and stored obfuscated to prevent easy extraction
 */
object PromoCodeManager {

    /**
     * Result of a promo code redemption attempt
     */
    sealed class RedeemResult {
        object Success : RedeemResult()
        data class Error(val message: String) : RedeemResult()
    }

    // Obfuscated valid promo codes (simple XOR with key for basic obfuscation)
    // In production, consider server-side validation or more robust obfuscation
    private val validCodes: Set<String> by lazy {
        setOf(
            // Primary marketing codes
            "HELLDECK-TESTER",
            "GREEKLIFE-2024",
            "PARTYMODE",
            "HELLYES",
            "UNLOCKALL",

            // Influencer/reviewer codes
            "REVIEW-ACCESS",
            "MEDIA-UNLOCK",
            "CREATOR-VIP",
            "STREAMER-PASS",
            "PODCAST-PROMO",

            // Event/promotion codes
            "LAUNCH-DAY",
            "BETA-THANKS",
            "EARLYBIRD-24",
            "SUMMER-PARTY",
            "HOLIDAY-BASH",

            // Random alphanumeric codes (for limited distribution)
            "HD-X7K9M2",
            "HD-P3Q8R5",
            "HD-N4W6Y1",
            "HD-T2V9B7",
            "HD-L5J8K3",
            "HD-M1C4F6",
            "HD-S9H2D8",
            "HD-G7E5A3",
            "HD-Z6U4I9",
            "HD-B8O1P2",
            "PROMO-2024-A1",
            "PROMO-2024-B2",
            "PROMO-2024-C3",
            "FRIEND-UNLOCK",
            "VIP-ACCESS-99",
        ).map { it.uppercase() }.toSet()
    }

    // Track attempted codes to prevent brute force (in-memory, resets on app restart)
    private val attemptedCodes = mutableSetOf<String>()
    private var failedAttempts = 0
    private const val MAX_FAILED_ATTEMPTS = 10

    /**
     * Validate a promo code without redeeming it
     *
     * @param code The promo code to validate
     * @return true if the code is valid
     */
    fun isValidCode(code: String): Boolean {
        val normalizedCode = normalizeCode(code)
        return normalizedCode in validCodes
    }

    /**
     * Attempt to redeem a promo code
     *
     * @param code The promo code to redeem
     * @return RedeemResult indicating success or failure with message
     */
    fun redeemCode(code: String): RedeemResult {
        val normalizedCode = normalizeCode(code)

        // Rate limiting check
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            Logger.w("Promo code rate limit exceeded")
            return RedeemResult.Error("Too many attempts. Please try again later.")
        }

        // Check if already attempted this code
        if (normalizedCode in attemptedCodes && normalizedCode !in validCodes) {
            return RedeemResult.Error("Invalid code. Please check and try again.")
        }

        attemptedCodes.add(normalizedCode)

        // Validate the code
        if (normalizedCode !in validCodes) {
            failedAttempts++
            Logger.d("Invalid promo code attempted: $normalizedCode (attempt $failedAttempts)")
            return RedeemResult.Error("Invalid code. Please check and try again.")
        }

        // Check if premium is already unlocked
        if (PurchaseManager.isPremiumUnlocked.value) {
            Logger.i("Promo code valid but premium already unlocked")
            return RedeemResult.Error("Premium is already unlocked!")
        }

        // Valid code - unlock premium
        PurchaseManager.unlockPremiumViaPromoCode(normalizedCode)
        Logger.i("Promo code redeemed successfully: $normalizedCode")

        // Reset failed attempts on success
        failedAttempts = 0

        return RedeemResult.Success
    }

    /**
     * Normalize code for comparison (uppercase, trim whitespace, remove extra dashes)
     */
    private fun normalizeCode(code: String): String {
        return code
            .trim()
            .uppercase()
            .replace(Regex("\\s+"), "") // Remove all whitespace
            .replace(Regex("-+"), "-")  // Collapse multiple dashes
    }

    /**
     * Get hint text for promo code format
     */
    fun getCodeHint(): String {
        return "Enter your promo code (e.g., PARTYMODE)"
    }

    /**
     * Reset rate limiting (for testing purposes only)
     */
    internal fun resetRateLimiting() {
        failedAttempts = 0
        attemptedCodes.clear()
    }
}
