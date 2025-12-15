package com.helldeck.content.tools

import android.content.Context
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.validation.GameContractValidator
import com.helldeck.engine.GameMetadata
import com.helldeck.utils.Logger
import kotlinx.coroutines.runBlocking

/**
 * Debug harness to test all games systematically.
 * Generates cards for each game and validates them.
 */
object PlayAllGamesHarness {

    data class GameTestResult(
        val gameId: String,
        val gameName: String,
        val attempts: Int,
        val successes: Int,
        val failures: List<FailureInfo>
    ) {
        val successRate: Double get() = if (attempts > 0) successes.toDouble() / attempts else 0.0
        val isPassing: Boolean get() = successes > 0
    }

    data class FailureInfo(
        val attempt: Int,
        val reason: String,
        val cardText: String? = null
    )

    data class HarnessReport(
        val results: List<GameTestResult>,
        val totalAttempts: Int,
        val totalSuccesses: Int,
        val totalFailures: Int
    ) {
        val overallSuccessRate: Double get() =
            if (totalAttempts > 0) totalSuccesses.toDouble() / totalAttempts else 0.0
        val passingGames: Int get() = results.count { it.isPassing }
        val failingGames: Int get() = results.count { !it.isPassing }
    }

    /**
     * Run the full harness: test all games with N attempts each
     */
    fun runAll(context: Context, attemptsPerGame: Int = 25): HarnessReport {
        val allGames = GameMetadata.getAllGames()
        val results = mutableListOf<GameTestResult>()
        var totalAttempts = 0
        var totalSuccesses = 0
        var totalFailures = 0

        allGames.forEach { game ->
            val result = testGame(context, game.id, game.title, attemptsPerGame)
            results.add(result)
            totalAttempts += result.attempts
            totalSuccesses += result.successes
            totalFailures += result.failures.size
        }

        return HarnessReport(results, totalAttempts, totalSuccesses, totalFailures)
    }

    /**
     * Test a single game with N attempts
     */
    fun testGame(
        context: Context,
        gameId: String,
        gameName: String,
        attempts: Int
    ): GameTestResult = runBlocking {
        val failures = mutableListOf<FailureInfo>()
        var successes = 0

        val engine = com.helldeck.content.engine.ContentEngineProvider.get(context)

        repeat(attempts) { attempt ->
            try {
                val request = GameEngine.Request(
                    gameId = gameId,
                    sessionId = "harness_${System.currentTimeMillis()}_$attempt",
                    players = listOf("TestPlayer1", "TestPlayer2", "TestPlayer3", "TestPlayer4"),
                    spiceMax = 2
                )

                val result = engine.next(request)

                // Validate contract
                val contractResult = GameContractValidator.validate(
                    gameId = result.filledCard.game,
                    interactionType = result.interactionType,
                    options = result.options,
                    filledCard = result.filledCard,
                    playersCount = 4
                )

                if (!contractResult.isValid) {
                    failures.add(FailureInfo(
                        attempt = attempt + 1,
                        reason = "Contract validation failed: ${contractResult.reasons.joinToString(", ")}",
                        cardText = result.filledCard.text
                    ))
                } else if (result.filledCard.text.contains("{") || result.filledCard.text.contains("}")) {
                    failures.add(FailureInfo(
                        attempt = attempt + 1,
                        reason = "Card contains unresolved placeholders",
                        cardText = result.filledCard.text
                    ))
                } else if (result.filledCard.text.contains("null", ignoreCase = true)) {
                    failures.add(FailureInfo(
                        attempt = attempt + 1,
                        reason = "Card contains 'null' text",
                        cardText = result.filledCard.text
                    ))
                } else {
                    successes++
                }

            } catch (e: Exception) {
                failures.add(FailureInfo(
                    attempt = attempt + 1,
                    reason = "Exception: ${e.message}",
                    cardText = null
                ))
            }
        }

        GameTestResult(gameId, gameName, attempts, successes, failures)
    }

    /**
     * Generate human-readable report
     */
    fun generateReport(report: HarnessReport): String {
        val sb = StringBuilder()
        sb.appendLine("=== Play All Games Harness Report ===")
        sb.appendLine()

        sb.appendLine("Overall Summary:")
        sb.appendLine("  Total attempts: ${report.totalAttempts}")
        sb.appendLine("  Successes: ${report.totalSuccesses}")
        sb.appendLine("  Failures: ${report.totalFailures}")
        sb.appendLine("  Success rate: ${"%.1f".format(report.overallSuccessRate * 100)}%")
        sb.appendLine("  Passing games: ${report.passingGames}/${report.results.size}")
        sb.appendLine("  Failing games: ${report.failingGames}/${report.results.size}")
        sb.appendLine()

        // List passing games
        if (report.passingGames > 0) {
            sb.appendLine("✅ Passing Games:")
            report.results.filter { it.isPassing }.forEach { result ->
                sb.appendLine("  ${result.gameName} (${result.gameId})")
                sb.appendLine("    Success rate: ${"%.1f".format(result.successRate * 100)}% (${result.successes}/${result.attempts})")
            }
            sb.appendLine()
        }

        // List failing games with details
        if (report.failingGames > 0) {
            sb.appendLine("❌ Failing Games:")
            report.results.filter { !it.isPassing }.forEach { result ->
                sb.appendLine("  ${result.gameName} (${result.gameId})")
                sb.appendLine("    Success rate: ${"%.1f".format(result.successRate * 100)}% (${result.successes}/${result.attempts})")
                sb.appendLine("    Failures:")
                result.failures.take(5).forEach { failure ->
                    sb.appendLine("      Attempt ${failure.attempt}: ${failure.reason}")
                    if (failure.cardText != null) {
                        sb.appendLine("        Card: ${failure.cardText.take(100)}...")
                    }
                }
                if (result.failures.size > 5) {
                    sb.appendLine("      ... and ${result.failures.size - 5} more failures")
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    /**
     * Generate CSV report for analysis
     */
    fun generateCsvReport(report: HarnessReport): String {
        val sb = StringBuilder()
        sb.appendLine("Game ID,Game Name,Attempts,Successes,Failures,Success Rate")

        report.results.forEach { result ->
            sb.appendLine("${result.gameId},${result.gameName},${result.attempts},${result.successes},${result.failures.size},${"%.3f".format(result.successRate)}")
        }

        return sb.toString()
    }
}
