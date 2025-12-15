package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.InteractionType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GameContractValidator.
 * Ensures all interaction types have proper validation rules.
 */
class GameContractValidatorTest {

    private fun createTestCard(text: String = "Test card text goes here"): FilledCard {
        return FilledCard(
            id = "test_card",
            game = "TEST_GAME",
            text = text,
            family = "test_family",
            spice = 1,
            locality = 1
        )
    }

    @Test
    fun `valid card with NONE interaction passes`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.NONE,
            options = GameOptions.None,
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("NONE interaction should pass with None options", result.isValid)
    }

    @Test
    fun `A_B_CHOICE requires AB options`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.A_B_CHOICE,
            options = GameOptions.AB("Option A", "Option B"),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("A_B_CHOICE with valid AB options should pass", resultValid.isValid)

        val resultInvalid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.A_B_CHOICE,
            options = GameOptions.None,
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("A_B_CHOICE with None options should fail", resultInvalid.isValid)
        assertTrue("Should have failure reason", resultInvalid.reasons.isNotEmpty())
    }

    @Test
    fun `A_B_CHOICE rejects identical options`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.A_B_CHOICE,
            options = GameOptions.AB("Same", "Same"),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("A_B_CHOICE with identical options should fail", result.isValid)
        assertTrue("Should mention options must be different",
            result.reasons.any { it.contains("must be different") })
    }

    @Test
    fun `A_B_CHOICE rejects empty options`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.A_B_CHOICE,
            options = GameOptions.AB("", "Option B"),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("A_B_CHOICE with empty option should fail", result.isValid)
    }

    @Test
    fun `VOTE_PLAYER requires at least 2 players`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.VOTE_PLAYER,
            options = GameOptions.PlayerVote(listOf("Player1", "Player2", "Player3")),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("VOTE_PLAYER with 3 players should pass", resultValid.isValid)

        val resultInvalid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.VOTE_PLAYER,
            options = GameOptions.PlayerVote(listOf("Player1")),
            filledCard = createTestCard(),
            playersCount = 1
        )
        assertFalse("VOTE_PLAYER with 1 player should fail", resultInvalid.isValid)
    }

    @Test
    fun `TABOO_GUESS requires non-empty word and forbidden list`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.TABOO_GUESS,
            options = GameOptions.Taboo("password", listOf("computer", "login", "security")),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("TABOO with valid word and forbidden list should pass", resultValid.isValid)

        val resultNoWord = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.TABOO_GUESS,
            options = GameOptions.Taboo("", listOf("word1", "word2")),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("TABOO with empty word should fail", resultNoWord.isValid)

        val resultNoForbidden = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.TABOO_GUESS,
            options = GameOptions.Taboo("password", emptyList()),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("TABOO with empty forbidden list should fail", resultNoForbidden.isValid)
    }

    @Test
    fun `card with placeholders fails`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.NONE,
            options = GameOptions.None,
            filledCard = createTestCard("This has {placeholder} in it"),
            playersCount = 3
        )
        assertFalse("Card with {placeholder} should fail", result.isValid)
        assertTrue("Should mention placeholder",
            result.reasons.any { it.contains("placeholder") })
    }

    @Test
    fun `card with null text fails`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.NONE,
            options = GameOptions.None,
            filledCard = createTestCard("This has null in it"),
            playersCount = 3
        )
        assertFalse("Card with 'null' text should fail", result.isValid)
    }

    @Test
    fun `card too short fails`() {
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.NONE,
            options = GameOptions.None,
            filledCard = createTestCard("Too short"),
            playersCount = 3
        )
        assertFalse("Card with <4 words should fail", result.isValid)
        assertTrue("Should mention too short",
            result.reasons.any { it.contains("too short") })
    }

    @Test
    fun `card too long fails`() {
        val longText = (1..60).joinToString(" ") { "word" }
        val result = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.NONE,
            options = GameOptions.None,
            filledCard = createTestCard(longText),
            playersCount = 3
        )
        assertFalse("Card with >50 words should fail", result.isValid)
        assertTrue("Should mention too long",
            result.reasons.any { it.contains("too long") })
    }

    @Test
    fun `TRUE_FALSE requires TrueFalse options`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.TRUE_FALSE,
            options = GameOptions.TrueFalse,
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("TRUE_FALSE with TrueFalse options should pass", resultValid.isValid)

        val resultInvalid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.TRUE_FALSE,
            options = GameOptions.None,
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("TRUE_FALSE with None options should fail", resultInvalid.isValid)
    }

    @Test
    fun `ODD_EXPLAIN requires at least 3 items`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.ODD_EXPLAIN,
            options = GameOptions.OddOneOut(listOf("Item1", "Item2", "Item3")),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("ODD_EXPLAIN with 3 items should pass", resultValid.isValid)

        val resultInvalid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.ODD_EXPLAIN,
            options = GameOptions.OddOneOut(listOf("Item1", "Item2")),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("ODD_EXPLAIN with <3 items should fail", resultInvalid.isValid)
    }

    @Test
    fun `SPEED_LIST requires category and letter`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.SPEED_LIST,
            options = GameOptions.Scatter("Animals", "S"),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertTrue("SPEED_LIST with category and letter should pass", resultValid.isValid)

        val resultNoCategory = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.SPEED_LIST,
            options = GameOptions.Scatter("", "S"),
            filledCard = createTestCard(),
            playersCount = 3
        )
        assertFalse("SPEED_LIST with empty category should fail", resultNoCategory.isValid)
    }

    @Test
    fun `MINI_DUEL requires at least 2 players`() {
        val resultValid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.MINI_DUEL,
            options = GameOptions.Challenge("Duel!"),
            filledCard = createTestCard(),
            playersCount = 2
        )
        assertTrue("MINI_DUEL with 2 players should pass", resultValid.isValid)

        val resultInvalid = GameContractValidator.validate(
            gameId = "TEST",
            interactionType = InteractionType.MINI_DUEL,
            options = GameOptions.Challenge("Duel!"),
            filledCard = createTestCard(),
            playersCount = 1
        )
        assertFalse("MINI_DUEL with 1 player should fail", resultInvalid.isValid)
    }
}
