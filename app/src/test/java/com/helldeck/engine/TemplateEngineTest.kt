package com.helldeck.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.BaseTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for TemplateEngine
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TemplateEngineTest : BaseTest() {

    private lateinit var context: Context
    private lateinit var templateEngine: TemplateEngine

    @Before
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        templateEngine = TemplateEngine(context)
    }

    @Test
    fun `fill template with valid slots returns filled text`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test1",
            game = "ROAST_CONSENSUS",
            text = "Hello {name}, you are {adjective}",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("name") } returns "World"
        every { slotProvider.provideSlot("adjective") } returns "awesome"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("Hello World, you are awesome", result)
        assertTrue("Result should contain provided words", result.contains("World"))
        assertTrue("Result should contain provided words", result.contains("awesome"))
    }

    @Test
    fun `fill template with multiple occurrences of same slot works correctly`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test2",
            game = "ROAST_CONSENSUS",
            text = "{name} loves {name} and {name} is great",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 12
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("name") } returns "John"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("John loves John and John is great", result)
        assertEquals("Should contain name 3 times", 3, result.split("John").size - 1)
    }

    @Test
    fun `fill template with special characters in slots works correctly`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test3",
            game = "ROAST_CONSENSUS",
            text = "Player {name} has {action}!",
            family = "test",
            spice = 2,
            locality = 1,
            maxWords = 8
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("name") } returns "José"
        every { slotProvider.provideSlot("action") } returns "embarrassed-himself"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("Player José has embarrassed-himself!", result)
        assertTrue("Should contain accented character", result.contains("José"))
        assertTrue("Should contain hyphenated word", result.contains("embarrassed-himself"))
    }

    @Test
    fun `fill template with empty slot provider throws exception`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test4",
            game = "ROAST_CONSENSUS",
            text = "Hello {name}",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 5
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("name") } returns null

        // Act & Assert
        try {
            templateEngine.fill(template, slotProvider::provideSlot)
            fail("Should have thrown exception for null slot value")
        } catch (e: TemplateEngineException) {
            assertTrue("Exception message should mention slot",
                e.message?.contains("name") == true)
        }
    }

    @Test
    fun `fill template with missing slot in provider throws exception`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test5",
            game = "ROAST_CONSENSUS",
            text = "Hello {name} and {missing_slot}",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 8
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("name") } returns "World"
        every { slotProvider.provideSlot("missing_slot") } returns null

        // Act & Assert
        try {
            templateEngine.fill(template, slotProvider::provideSlot)
            fail("Should have thrown exception for missing slot")
        } catch (e: TemplateEngineException) {
            assertTrue("Exception message should mention missing slot",
                e.message?.contains("missing_slot") == true)
        }
    }

    @Test
    fun `fill template with complex slot names works correctly`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test6",
            game = "ROAST_CONSENSUS",
            text = "The {very_long_slot_name} is {another_complex_slot_name}",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("very_long_slot_name") } returns "quick brown fox"
        every { slotProvider.provideSlot("another_complex_slot_name") } returns "jumping over"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("The quick brown fox is jumping over", result)
        assertTrue("Should contain complex slot values", result.contains("quick brown fox"))
        assertTrue("Should contain complex slot values", result.contains("jumping over"))
    }

    @Test
    fun `fill template with numbers in slots works correctly`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test7",
            game = "ROAST_CONSENSUS",
            text = "Player {number} scored {score} points",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 8
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot("number") } returns "42"
        every { slotProvider.provideSlot("score") } returns "1337"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("Player 42 scored 1337 points", result)
        assertTrue("Should contain numbers", result.contains("42"))
        assertTrue("Should contain numbers", result.contains("1337"))
    }

    @Test
    fun `fill template with empty template text throws exception`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test8",
            game = "ROAST_CONSENSUS",
            text = "",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 5
        )

        val slotProvider = mockk<SlotProvider>()

        // Act & Assert
        try {
            templateEngine.fill(template, slotProvider::provideSlot)
            fail("Should have thrown exception for empty template")
        } catch (e: TemplateEngineException) {
            assertTrue("Exception message should mention empty template",
                e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `fill template with no slots returns original text`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test9",
            game = "ROAST_CONSENSUS",
            text = "This is a template without any slots",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        val slotProvider = mockk<SlotProvider>()

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("This is a template without any slots", result)
    }

    @Test
    fun `fill template with malformed slot brackets throws exception`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test10",
            game = "ROAST_CONSENSUS",
            text = "Hello {name and {another slot}", // Malformed brackets
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 8
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot(any()) } returns "test"

        // Act & Assert
        try {
            templateEngine.fill(template, slotProvider::provideSlot)
            fail("Should have thrown exception for malformed brackets")
        } catch (e: TemplateEngineException) {
            assertTrue("Exception message should mention malformed brackets",
                e.message?.contains("malformed") == true ||
                e.message?.contains("bracket") == true)
        }
    }

    @Test
    fun `fill template with whitespace in slot names works correctly`() = runTest {
        // Arrange
        val template = TemplateDef(
            id = "test11",
            game = "ROAST_CONSENSUS",
            text = "The { slot with spaces } is here",
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 8
        )

        val slotProvider = mockk<SlotProvider>()
        every { slotProvider.provideSlot(" slot with spaces ") } returns "special content"

        // Act
        val result = templateEngine.fill(template, slotProvider::provideSlot)

        // Assert
        assertEquals("The special content is here", result)
        assertTrue("Should contain slot with spaces", result.contains("special content"))
    }

    @Test
    fun `fill template performance with many slots is acceptable`() = runTest {
        // Arrange
        val slots = (1..20).associate { "slot$it" to "value$it" }
        val templateText = slots.keys.joinToString(" ") { "{$it}" }

        val template = TemplateDef(
            id = "test12",
            game = "ROAST_CONSENSUS",
            text = templateText,
            family = "test",
            spice = 1,
            locality = 1,
            maxWords = 50
        )

        val slotProvider = mockk<SlotProvider>()
        slots.forEach { (key, value) ->
            every { slotProvider.provideSlot(key) } returns value
        }

        // Act
        val startTime = System.currentTimeMillis()
        val result = templateEngine.fill(template, slotProvider::provideSlot)
        val endTime = System.currentTimeMillis()

        // Assert
        assertTrue("Result should contain all slot values",
            slots.values.all { result.contains(it) })
        assertTrue("Filling should complete within reasonable time",
            endTime - startTime < 1000) // Less than 1 second
    }
}