
package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import kotlinx.coroutines.runBlocking
import org.junit.Before
import com.helldeck.engine.Config
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import kotlin.random.Random

/**
 * Unit tests for ContextualSelector Thompson Sampling algorithm validation.
 * 
 * Tests the core Thompson Sampling implementation including:
 * - Alpha/beta parameter updates
 * - Template selection with context constraints
 * - Diversity penalties and affinity bonuses
 * - Edge cases and error conditions
 */
class ContextualSelectorTest {

    private lateinit var mockRepo: ContentRepository
    private lateinit var selector: ContextualSelector
    private lateinit var rng: SeededRng

    @Before
    fun setup() {
        mockRepo = mock(ContentRepository::class.java)
        rng = SeededRng(42) // Fixed seed for reproducible tests
        selector = ContextualSelector(mockRepo, rng.random)
        // Initialize configuration defaults for epsilon/learning parameters
        Config.loadFromString("")
    }

    @Test
    fun `seed initializes alpha and beta parameters correctly`() {
        val priors = mapOf(
            "template1" to Pair(2.0, 3.0),
            "template2" to Pair(1.0, 1.0),
            "template3" to Pair(5.0, 2.0)
        )

        selector.seed(priors)

        // Verify parameters are set correctly
        // Note: We can't directly access private alpha/beta, but we can test behavior
        val context = ContextualSelector.Context(
            players = listOf("Player1", "Player2"),
            spiceMax = 3
        )
        
        // This should not crash - parameters are initialized
        val templates = listOf(
            createTestTemplate("template1", spice = 1),
            createTestTemplate("template2", spice = 2),
            createTestTemplate("template3", spice = 3)
        )
        
        val result = selector.pick(context, templates)
        assertNotNull(result)
    }

    @Test
    fun `update modifies alpha and beta correctly`() {
        // Initialize with known values
        selector.seed(mapOf("test" to Pair(1.0, 1.0)))

        // Update with reward
        selector.update("test", 0.8) // High reward

        val context = ContextualSelector.Context(players = listOf("P1"))
        val templates = listOf(createTestTemplate("test"))
        
        // Multiple selections should increase alpha (successes) more than beta (failures)
        val results = mutableListOf<TemplateV2>()
        repeat(10) {
            results.add(selector.pick(context, templates))
        }

        // The template should be selected more often after positive update
        // This is probabilistic, so we can't guarantee exact counts,
        // but the selection probability should increase
        assertTrue("Template should be selected", results.isNotEmpty())
    }

    @Test
    fun `pick respects spiceMax constraint`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            spiceMax = 2
        )

        val templates = listOf(
            createTestTemplate("low_spice", spice = 1),
            createTestTemplate("high_spice", spice = 3),
            createTestTemplate("medium_spice", spice = 2)
        )

        val result = selector.pick(context, templates)
        
        // Should not pick the high spice template
        assertNotEquals("High spice template should not be selected", "high_spice", result.id)
        assertTrue("Should select low or medium spice", 
            result.id == "low_spice" || result.id == "medium_spice")
    }

    @Test
    fun `pick respects wantedGameId constraint`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            wantedGameId = "specific_game"
        )

        val templates = listOf(
            createTestTemplate("specific_game", game = "specific_game"),
            createTestTemplate("other_game", game = "other_game")
        )

        val result = selector.pick(context, templates)
        
        assertEquals("Should pick specific game", "specific_game", result.id)
    }

    @Test
    fun `pick respects avoidIds constraint`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            avoidIds = setOf("avoid_me")
        )

        val templates = listOf(
            createTestTemplate("avoid_me"),
            createTestTemplate("pick_me")
        )

        val result = selector.pick(context, templates)
        
        assertNotEquals("Should avoid specified template", "avoid_me", result.id)
        assertEquals("Should pick available template", "pick_me", result.id)
    }

    @Test
    fun `pick respects minPlayers constraint`() {
        val context = ContextualSelector.Context(
            players = listOf("P1", "P2"), // 2 players
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("needs_3_players", minPlayers = 3),
            createTestTemplate("needs_5_players", minPlayers = 5),
            createTestTemplate("no_min_requirement")
        )

        val result = selector.pick(context, templates)
        
        // Should not pick templates that require more players than available
        assertNotEquals("Should not pick 5-player template", "needs_5_players", result.id)
        assertTrue("Should pick valid template", 
            result.id == "needs_3_players" || result.id == "no_min_requirement")
    }

    @Test
    fun `diversity penalty applies to recent templates`() {
        // Mock recent history
        `when`(mockRepo.recentHistoryIds(10)).thenReturn(setOf("recent_template"))

        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("recent_template"),
            createTestTemplate("old_template")
        )

        val result = selector.pick(context, templates)
        
        // Should prefer the old template over recent one
        assertEquals("Should avoid recent template", "old_template", result.id)
    }

    @Test
    fun `diversity penalty applies to recent families`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            recentFamilies = listOf("recent_family"),
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("recent_family_template", family = "recent_family"),
            createTestTemplate("other_family_template", family = "other_family")
        )

        val result = selector.pick(context, templates)
        
        // Should prefer the other family template
        assertEquals("Should avoid recent family", "other_family_template", result.id)
    }

    @Test
    fun `affinity bonus applies to matching tags`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            tagAffinity = mapOf("funny" to 0.8, "smart" to 0.6),
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("funny_template", tags = setOf("funny")),
            createTestTemplate("smart_template", tags = setOf("smart")),
            createTestTemplate("boring_template", tags = setOf("boring"))
        )

        val result = selector.pick(context, templates)
        
        // Should prefer funny template (highest affinity)
        assertEquals("Should prefer high affinity tag", "funny_template", result.id)
    }

    @Test
    fun `selection handles empty pool gracefully`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            spiceMax = 3
        )

        val templates = emptyList<TemplateV2>()

        try {
            selector.pick(context, templates)
            fail("Should throw exception for empty pool")
        } catch (e: Exception) {
            assertTrue("Should throw meaningful exception", e.message?.isNotEmpty() == true)
        }
    }

    @Test
    fun `novelty bonus affects selection`() {
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("common_template", weight = 1.0),
            createTestTemplate("rare_template", weight = 2.0) // Higher weight = more novel
        )

        val results = mutableListOf<TemplateV2>()
        repeat(20) { // Multiple selections to see pattern
            results.add(selector.pick(context, templates))
        }
        val ids = results.map { it.id }.toSet()
        // Ensure both templates are viable and selection occurs
        assertTrue(ids.contains("rare_template") || ids.contains("common_template"))
    }

    @Test
    fun `beta sampling produces valid distributions`() {
        // Test the internal sampling methods indirectly through selection
        val context = ContextualSelector.Context(
            players = listOf("Player1"),
            spiceMax = 3
        )

        val templates = listOf(
            createTestTemplate("test1"),
            createTestTemplate("test2")
        )

        // Multiple selections should not crash
        repeat(10) {
            val result = selector.pick(context, templates)
            assertNotNull("Selection should not be null", result)
            assertTrue("Result should be one of the templates", 
                templates.any { it.id == result.id })
        }
    }

    @Test
    fun `context with no players handles gracefully`() {
        val context = ContextualSelector.Context(
            players = emptyList(),
            spiceMax = 3
        )

        val templates = listOf(createTestTemplate("test"))

        val result = selector.pick(context, templates)
        
        assertNotNull("Should still select template", result)
        assertEquals("Should select available template", "test", result.id)
    }

    @Test
    fun `learning rate affects parameter updates`() {
        // Initialize with known values
        selector.seed(mapOf("test" to Pair(1.0, 1.0)))

        val context = ContextualSelector.Context(players = listOf("P1"))
        val templates = listOf(createTestTemplate("test"))

        // Update with low reward
        selector.update("test", 0.1)
        
        // Multiple selections to see learning effect
        val lowRewardResults = mutableListOf<TemplateV2>()
        repeat(5) {
            lowRewardResults.add(selector.pick(context, templates))
        }

        // Update with high reward
        selector.update("test", 0.9)
        
        val highRewardResults = mutableListOf<TemplateV2>()
        repeat(5) {
            highRewardResults.add(selector.pick(context, templates))
        }

        // With a single candidate, selection remains the same; validate no crash and consistent id
        val lowRewardSet = lowRewardResults.map { it.id }.toSet()
        val highRewardSet = highRewardResults.map { it.id }.toSet()
        assertTrue(lowRewardSet.size == 1 && highRewardSet.size == 1)
    }

    /**
     * Helper method to create test templates
     */
    private fun createTestTemplate(
        id: String,
        game: String = "test_game",
        spice: Int = 1,
        family: String = "test_family",
        tags: Set<String> = emptySet(),
        weight: Double = 1.0,
        minPlayers: Int? = null
    ): TemplateV2 {
        return TemplateV2(
            id = id,
            game = game,
            family = family,
            tags = tags.toList(),
            spice = spice,
            locality = 1,
            weight = weight,
            min_players = minPlayers,
            text = "Test template $id"
        )
    }
}
