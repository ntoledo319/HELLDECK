package com.helldeck.data.entities

import com.helldeck.data.TemplateEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TemplateEntity validation and behavior
 */
class TemplateEntityTest {

    @Test
    fun `TemplateEntity with valid data creates successfully`() {
        // Arrange & Act
        val template = TemplateEntity(
            id = "test_template",
            game = "ROAST_CONSENSUS",
            text = "Test template {slot}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        // Assert
        assertNotNull("Template should be created", template)
        assertEquals("ID should match", "test_template", template.id)
        assertEquals("Game should match", "ROAST_CONSENSUS", template.game)
        assertEquals("Text should match", "Test template {slot}", template.text)
        assertEquals("Family should match", "test_family", template.family)
        assertEquals("Spice should match", 2, template.spice)
        assertEquals("Locality should match", 1, template.locality)
        assertEquals("MaxWords should match", 16, template.maxWords)
    }

    @Test
    fun `TemplateEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val template = TemplateEntity(
            id = "min_template",
            game = "G",
            text = "{s}",
            family = "f",
            spice = 1,
            locality = 1,
            maxWords = 1
        )

        // Assert
        assertNotNull("Minimal template should be created", template)
        assertEquals("Minimal ID should match", "min_template", template.id)
        assertEquals("Minimal game should match", "G", template.game)
        assertEquals("Minimal text should match", "{s}", template.text)
        assertEquals("Minimal family should match", "f", template.family)
        assertEquals("Minimal spice should match", 1, template.spice)
        assertEquals("Minimal locality should match", 1, template.locality)
        assertEquals("Minimal maxWords should match", 1, template.maxWords)
    }

    @Test
    fun `TemplateEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val template = TemplateEntity(
            id = "A".repeat(100),
            game = "ROAST_CONSENSUS",
            text = "A".repeat(1000),
            family = "family_name",
            spice = 3,
            locality = 3,
            maxWords = 1000
        )

        // Assert
        assertNotNull("Maximal template should be created", template)
        assertEquals("Maximal ID should match", "A".repeat(100), template.id)
        assertEquals("Maximal text should match", "A".repeat(1000), template.text)
        assertEquals("Maximal spice should match", 3, template.spice)
        assertEquals("Maximal locality should match", 3, template.locality)
        assertEquals("Maximal maxWords should match", 1000, template.maxWords)
    }

    @Test
    fun `TemplateEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalTemplate = TemplateEntity(
            id = "original",
            game = "ROAST_CONSENSUS",
            text = "Original {slot}",
            family = "original_family",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        // Act
        val copiedTemplate = originalTemplate.copy(
            text = "Copied {slot}",
            spice = 2
        )

        // Assert
        assertNotNull("Copied template should not be null", copiedTemplate)
        assertEquals("ID should remain same", originalTemplate.id, copiedTemplate.id)
        assertEquals("Game should remain same", originalTemplate.game, copiedTemplate.game)
        assertEquals("Family should remain same", originalTemplate.family, copiedTemplate.family)
        assertEquals("Locality should remain same", originalTemplate.locality, copiedTemplate.locality)
        assertEquals("MaxWords should remain same", originalTemplate.maxWords, copiedTemplate.maxWords)

        assertEquals("Text should be updated", "Copied {slot}", copiedTemplate.text)
        assertEquals("Spice should be updated", 2, copiedTemplate.spice)

        assertNotEquals("Original and copy should not be same object", originalTemplate, copiedTemplate)
    }

    @Test
    fun `TemplateEntity handles special characters correctly`() {
        // Arrange & Act
        val specialTemplate = TemplateEntity(
            id = "spécial_tëmplate_ñ",
            game = "ROAST_CONSENSUS",
            text = "Template with spécial çharácters and émojis 🚀 {slot}!",
            family = "spécial_fâmily_ñ",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Special template should be created", specialTemplate)
        assertEquals("Special ID should be preserved", "spécial_tëmplate_ñ", specialTemplate.id)
        assertEquals("Special text should be preserved",
            "Template with spécial çharácters and émojis 🚀 {slot}!", specialTemplate.text)
        assertEquals("Special family should be preserved", "spécial_fâmily_ñ", specialTemplate.family)
    }

    @Test
    fun `TemplateEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodeTemplate = TemplateEntity(
            id = "ünícódé_tëst",
            game = "ROAST_CONSENSUS",
            text = "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯",
            family = "ünícódé_fâmíly",
            spice = 3,
            locality = 3,
            maxWords = 25
        )

        // Assert
        assertNotNull("Unicode template should be created", unicodeTemplate)
        assertEquals("Unicode ID should be preserved", "ünícódé_tëst", unicodeTemplate.id)
        assertEquals("Unicode text should be preserved",
            "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯", unicodeTemplate.text)
        assertEquals("Unicode family should be preserved", "ünícódé_fâmíly", unicodeTemplate.family)
    }

    @Test
    fun `TemplateEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptyTemplate = TemplateEntity(
            id = "",
            game = "",
            text = "",
            family = "",
            spice = 1,
            locality = 1,
            maxWords = 1
        )

        // Assert
        assertNotNull("Empty template should be created", emptyTemplate)
        assertEquals("Empty ID should be preserved", "", emptyTemplate.id)
        assertEquals("Empty game should be preserved", "", emptyTemplate.game)
        assertEquals("Empty text should be preserved", "", emptyTemplate.text)
        assertEquals("Empty family should be preserved", "", emptyTemplate.family)
    }

    @Test
    fun `TemplateEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespaceTemplate = TemplateEntity(
            id = "   ",
            game = "   ",
            text = "   ",
            family = "   ",
            spice = 1,
            locality = 1,
            maxWords = 1
        )

        // Assert
        assertNotNull("Whitespace template should be created", whitespaceTemplate)
        assertEquals("Whitespace ID should be preserved", "   ", whitespaceTemplate.id)
        assertEquals("Whitespace game should be preserved", "   ", whitespaceTemplate.game)
        assertEquals("Whitespace text should be preserved", "   ", whitespaceTemplate.text)
        assertEquals("Whitespace family should be preserved", "   ", whitespaceTemplate.family)
    }

    @Test
    fun `TemplateEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundaryTemplate = TemplateEntity(
            id = "boundary_test",
            game = "ROAST_CONSENSUS",
            text = "Boundary test {slot}",
            family = "boundary_family",
            spice = 1, // Minimum valid spice
            locality = 3, // Maximum valid locality
            maxWords = 100 // Arbitrary reasonable maximum
        )

        // Assert
        assertNotNull("Boundary template should be created", boundaryTemplate)
        assertEquals("Minimum spice should be preserved", 1, boundaryTemplate.spice)
        assertEquals("Maximum locality should be preserved", 3, boundaryTemplate.locality)
        assertEquals("MaxWords should be preserved", 100, boundaryTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity handles very long text correctly`() {
        // Arrange & Act
        val longText = "A".repeat(10000) + " Very long template text " + "B".repeat(10000)
        val longTemplate = TemplateEntity(
            id = "long_text_template",
            game = "ROAST_CONSENSUS",
            text = longText,
            family = "long_family",
            spice = 2,
            locality = 2,
            maxWords = 20000
        )

        // Assert
        assertNotNull("Long template should be created", longTemplate)
        assertEquals("Long text should be preserved", longText, longTemplate.text)
        assertEquals("Long text length should match", 40003, longTemplate.text.length)
    }

    @Test
    fun `TemplateEntity handles very long family name correctly`() {
        // Arrange & Act
        val longFamily = "A".repeat(1000) + "_very_long_family_name_" + "B".repeat(1000)
        val longFamilyTemplate = TemplateEntity(
            id = "long_family_template",
            game = "ROAST_CONSENSUS",
            text = "Long family template {slot}",
            family = longFamily,
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Long family template should be created", longFamilyTemplate)
        assertEquals("Long family should be preserved", longFamily, longFamilyTemplate.family)
        assertEquals("Long family length should match", 2025, longFamilyTemplate.family.length)
    }

    @Test
    fun `TemplateEntity handles very long ID correctly`() {
        // Arrange & Act
        val longId = "A".repeat(1000) + "_very_long_template_id_" + "B".repeat(1000)
        val longIdTemplate = TemplateEntity(
            id = longId,
            game = "ROAST_CONSENSUS",
            text = "Long ID template {slot}",
            family = "long_id_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Long ID template should be created", longIdTemplate)
        assertEquals("Long ID should be preserved", longId, longIdTemplate.id)
        assertEquals("Long ID length should match", 2023, longIdTemplate.id.length)
    }

    @Test
    fun `TemplateEntity handles templates with many slots correctly`() {
        // Arrange & Act
        val manySlotsText = (1..50).joinToString(" ") { "{slot$it}" }
        val manySlotsTemplate = TemplateEntity(
            id = "many_slots_template",
            game = "ROAST_CONSENSUS",
            text = manySlotsText,
            family = "many_slots_family",
            spice = 3,
            locality = 3,
            maxWords = 100
        )

        // Assert
        assertNotNull("Many slots template should be created", manySlotsTemplate)
        assertEquals("Many slots text should be preserved", manySlotsText, manySlotsTemplate.text)
        assertEquals("Slot count should be 50", 50, manySlotsTemplate.text.split("{slot").size - 1)
    }

    @Test
    fun `TemplateEntity handles templates with complex slot names correctly`() {
        // Arrange & Act
        val complexSlotsTemplate = TemplateEntity(
            id = "complex_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Complex {very_long_slot_name_with_underscores} and {another_complex_slot_name} test",
            family = "complex_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 25
        )

        // Assert
        assertNotNull("Complex slots template should be created", complexSlotsTemplate)
        assertTrue("Should contain first complex slot",
            complexSlotsTemplate.text.contains("{very_long_slot_name_with_underscores}"))
        assertTrue("Should contain second complex slot",
            complexSlotsTemplate.text.contains("{another_complex_slot_name}"))
    }

    @Test
    fun `TemplateEntity handles templates with repeated slots correctly`() {
        // Arrange & Act
        val repeatedSlotsTemplate = TemplateEntity(
            id = "repeated_slots_template",
            game = "ROAST_CONSENSUS",
            text = "{name} is {name} and {name} does {action} with {name}",
            family = "repeated_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Repeated slots template should be created", repeatedSlotsTemplate)
        val nameSlotCount = repeatedSlotsTemplate.text.split("{name}").size - 1
        assertEquals("Should contain name slot 4 times", 4, nameSlotCount)
        assertTrue("Should contain action slot", repeatedSlotsTemplate.text.contains("{action}"))
    }

    @Test
    fun `TemplateEntity handles templates with nested braces correctly`() {
        // Arrange & Act
        val nestedBracesTemplate = TemplateEntity(
            id = "nested_braces_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {{double braces}} and {single} braces",
            family = "nested_braces_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Nested braces template should be created", nestedBracesTemplate)
        assertTrue("Should contain double braces", nestedBracesTemplate.text.contains("{{double braces}}"))
        assertTrue("Should contain single braces", nestedBracesTemplate.text.contains("{single}"))
    }

    @Test
    fun `TemplateEntity handles templates with no slots correctly`() {
        // Arrange & Act
        val noSlotsTemplate = TemplateEntity(
            id = "no_slots_template",
            game = "ROAST_CONSENSUS",
            text = "This template has no slots at all",
            family = "no_slots_family",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        // Assert
        assertNotNull("No slots template should be created", noSlotsTemplate)
        assertEquals("No slots text should be preserved",
            "This template has no slots at all", noSlotsTemplate.text)
        assertFalse("Should not contain braces", noSlotsTemplate.text.contains("{") && noSlotsTemplate.text.contains("}"))
    }

    @Test
    fun `TemplateEntity handles templates with only slots correctly`() {
        // Arrange & Act
        val onlySlotsTemplate = TemplateEntity(
            id = "only_slots_template",
            game = "ROAST_CONSENSUS",
            text = "{slot1}{slot2}{slot3}",
            family = "only_slots_family",
            spice = 1,
            locality = 1,
            maxWords = 10
        )

        // Assert
        assertNotNull("Only slots template should be created", onlySlotsTemplate)
        assertEquals("Only slots text should be preserved", "{slot1}{slot2}{slot3}", onlySlotsTemplate.text)
        assertEquals("Should contain 3 slots", 3, onlySlotsTemplate.text.split("{slot").size - 1)
    }

    @Test
    fun `TemplateEntity handles templates with malformed slots correctly`() {
        // Arrange & Act
        val malformedSlotsTemplate = TemplateEntity(
            id = "malformed_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {unclosed slot and {nested} slots} and more",
            family = "malformed_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Malformed slots template should be created", malformedSlotsTemplate)
        assertTrue("Should contain malformed slot text",
            malformedSlotsTemplate.text.contains("{unclosed slot and {nested} slots}"))
    }

    @Test
    fun `TemplateEntity handles templates with emojis correctly`() {
        // Arrange & Act
        val emojiTemplate = TemplateEntity(
            id = "emoji_template",
            game = "ROAST_CONSENSUS",
            text = "Emoji template 🚀 with {slot} and 🌟 more emojis 🎯",
            family = "emoji_family",
            spice = 3,
            locality = 3,
            maxWords = 15
        )

        // Assert
        assertNotNull("Emoji template should be created", emojiTemplate)
        assertTrue("Should contain rocket emoji", emojiTemplate.text.contains("🚀"))
        assertTrue("Should contain star emoji", emojiTemplate.text.contains("🌟"))
        assertTrue("Should contain dart emoji", emojiTemplate.text.contains("🎯"))
        assertTrue("Should contain slot", emojiTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with numbers in text correctly`() {
        // Arrange & Act
        val numbersTemplate = TemplateEntity(
            id = "numbers_template",
            game = "ROAST_CONSENSUS",
            text = "Template with numbers 123 and {slot} with 456.789",
            family = "numbers_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Numbers template should be created", numbersTemplate)
        assertTrue("Should contain numbers", numbersTemplate.text.contains("123"))
        assertTrue("Should contain decimal", numbersTemplate.text.contains("456.789"))
        assertTrue("Should contain slot", numbersTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with special regex characters correctly`() {
        // Arrange & Act
        val regexCharsTemplate = TemplateEntity(
            id = "regex_chars_template",
            game = "ROAST_CONSENSUS",
            text = "Template with regex chars ^$*+?{}[]\\|() and {slot}",
            family = "regex_chars_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Regex chars template should be created", regexCharsTemplate)
        assertTrue("Should contain regex characters",
            regexCharsTemplate.text.contains("^$*+?{}[]\\|()"))
        assertTrue("Should contain slot", regexCharsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with HTML-like content correctly`() {
        // Arrange & Act
        val htmlLikeTemplate = TemplateEntity(
            id = "html_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with <html> like <content> and {slot} elements",
            family = "html_like_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("HTML-like template should be created", htmlLikeTemplate)
        assertTrue("Should contain angle brackets", htmlLikeTemplate.text.contains("<html>"))
        assertTrue("Should contain slot", htmlLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with quotes correctly`() {
        // Arrange & Act
        val quotesTemplate = TemplateEntity(
            id = "quotes_template",
            game = "ROAST_CONSENSUS",
            text = "Template with \"double quotes\" and 'single quotes' and {slot}",
            family = "quotes_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Quotes template should be created", quotesTemplate)
        assertTrue("Should contain double quotes", quotesTemplate.text.contains("\"double quotes\""))
        assertTrue("Should contain single quotes", quotesTemplate.text.contains("'single quotes'"))
        assertTrue("Should contain slot", quotesTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with newlines correctly`() {
        // Arrange & Act
        val newlinesTemplate = TemplateEntity(
            id = "newlines_template",
            game = "ROAST_CONSENSUS",
            text = "Template with\nnewlines\r\nand {slot}\twith tabs",
            family = "newlines_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Newlines template should be created", newlinesTemplate)
        assertTrue("Should contain newlines", newlinesTemplate.text.contains("\n"))
        assertTrue("Should contain carriage returns", newlinesTemplate.text.contains("\r"))
        assertTrue("Should contain tabs", newlinesTemplate.text.contains("\t"))
        assertTrue("Should contain slot", newlinesTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with zero values correctly`() {
        // Arrange & Act
        val zeroValuesTemplate = TemplateEntity(
            id = "zero_values_template",
            game = "ROAST_CONSENSUS",
            text = "Zero values template {slot}",
            family = "zero_values_family",
            spice = 1, // Minimum is 1, can't be 0
            locality = 1, // Minimum is 1, can't be 0
            maxWords = 0 // This might be allowed
        )

        // Assert
        assertNotNull("Zero values template should be created", zeroValuesTemplate)
        assertEquals("Spice should be minimum valid value", 1, zeroValuesTemplate.spice)
        assertEquals("Locality should be minimum valid value", 1, zeroValuesTemplate.locality)
        assertEquals("MaxWords should be zero", 0, zeroValuesTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity handles templates with negative values correctly`() {
        // Arrange & Act
        val negativeValuesTemplate = TemplateEntity(
            id = "negative_values_template",
            game = "ROAST_CONSENSUS",
            text = "Negative values template {slot}",
            family = "negative_values_family",
            spice = 1, // Minimum is 1, can't be negative
            locality = 1, // Minimum is 1, can't be negative
            maxWords = -1 // This might be problematic
        )

        // Assert
        assertNotNull("Negative values template should be created", negativeValuesTemplate)
        assertEquals("Spice should be minimum valid value", 1, negativeValuesTemplate.spice)
        assertEquals("Locality should be minimum valid value", 1, negativeValuesTemplate.locality)
        assertEquals("MaxWords should be negative", -1, negativeValuesTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity handles templates with extremely large maxWords correctly`() {
        // Arrange & Act
        val largeMaxWordsTemplate = TemplateEntity(
            id = "large_maxwords_template",
            game = "ROAST_CONSENSUS",
            text = "Large maxWords template {slot}",
            family = "large_maxwords_family",
            spice = 2,
            locality = 2,
            maxWords = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Large maxWords template should be created", largeMaxWordsTemplate)
        assertEquals("MaxWords should be Int.MAX_VALUE", Int.MAX_VALUE, largeMaxWordsTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity handles templates with extremely small maxWords correctly`() {
        // Arrange & Act
        val smallMaxWordsTemplate = TemplateEntity(
            id = "small_maxwords_template",
            game = "ROAST_CONSENSUS",
            text = "Small maxWords template {slot}",
            family = "small_maxwords_family",
            spice = 2,
            locality = 2,
            maxWords = 1
        )

        // Assert
        assertNotNull("Small maxWords template should be created", smallMaxWordsTemplate)
        assertEquals("MaxWords should be 1", 1, smallMaxWordsTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity handles templates with mixed content correctly`() {
        // Arrange & Act
        val mixedContentTemplate = TemplateEntity(
            id = "mixed_content_template",
            game = "ROAST_CONSENSUS",
            text = "Mixed content: números 123, emojis 🚀🌟, spécial çharácters, {slot}, and móre!",
            family = "mixed_content_family",
            spice = 3,
            locality = 3,
            maxWords = 30
        )

        // Assert
        assertNotNull("Mixed content template should be created", mixedContentTemplate)
        assertTrue("Should contain numbers", mixedContentTemplate.text.contains("123"))
        assertTrue("Should contain emojis", mixedContentTemplate.text.contains("🚀") && mixedContentTemplate.text.contains("🌟"))
        assertTrue("Should contain special characters", mixedContentTemplate.text.contains("çharácters"))
        assertTrue("Should contain slot", mixedContentTemplate.text.contains("{slot}"))
        assertTrue("Should contain more accents", mixedContentTemplate.text.contains("móre"))
    }

    @Test
    fun `TemplateEntity handles templates with only whitespace in slots correctly`() {
        // Arrange & Act
        val whitespaceSlotsTemplate = TemplateEntity(
            id = "whitespace_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with { } and {  } slots",
            family = "whitespace_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Whitespace slots template should be created", whitespaceSlotsTemplate)
        assertTrue("Should contain empty slot", whitespaceSlotsTemplate.text.contains("{ }"))
        assertTrue("Should contain whitespace slot", whitespaceSlotsTemplate.text.contains("{  }"))
    }

    @Test
    fun `TemplateEntity handles templates with tabs and spaces in slots correctly`() {
        // Arrange & Act
        val tabSpaceSlotsTemplate = TemplateEntity(
            id = "tab_space_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {\t} and { } slots",
            family = "tab_space_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Tab space slots template should be created", tabSpaceSlotsTemplate)
        assertTrue("Should contain tab slot", tabSpaceSlotsTemplate.text.contains("{\t}"))
        assertTrue("Should contain space slot", tabSpaceSlotsTemplate.text.contains("{ }"))
    }

    @Test
    fun `TemplateEntity handles templates with unicode slot names correctly`() {
        // Arrange & Act
        val unicodeSlotsTemplate = TemplateEntity(
            id = "unicode_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {ünícódé_slöt} and {ñémé_2} slots",
            family = "unicode_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Unicode slots template should be created", unicodeSlotsTemplate)
        assertTrue("Should contain unicode slot", unicodeSlotsTemplate.text.contains("{ünícódé_slöt}"))
        assertTrue("Should contain another unicode slot", unicodeSlotsTemplate.text.contains("{ñémé_2}"))
    }

    @Test
    fun `TemplateEntity handles templates with very deep nesting correctly`() {
        // Arrange & Act
        val deepNestingTemplate = TemplateEntity(
            id = "deep_nesting_template",
            game = "ROAST_CONSENSUS",
            text = "Template {{{{{with}}}}} deep {{{nesting}}} and {slot}",
            family = "deep_nesting_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Deep nesting template should be created", deepNestingTemplate)
        assertTrue("Should contain deep nesting", deepNestingTemplate.text.contains("{{{{{with}}}}}"))
        assertTrue("Should contain another nesting", deepNestingTemplate.text.contains("{{{nesting}}}"))
        assertTrue("Should contain slot", deepNestingTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with binary data correctly`() {
        // Arrange & Act
        val binaryLikeTemplate = TemplateEntity(
            id = "binary_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with binary-like content 01010101 and {slot} 11001100",
            family = "binary_like_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Binary-like template should be created", binaryLikeTemplate)
        assertTrue("Should contain binary-like content", binaryLikeTemplate.text.contains("01010101"))
        assertTrue("Should contain more binary-like content", binaryLikeTemplate.text.contains("11001100"))
        assertTrue("Should contain slot", binaryLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with mathematical symbols correctly`() {
        // Arrange & Act
        val mathSymbolsTemplate = TemplateEntity(
            id = "math_symbols_template",
            game = "ROAST_CONSENSUS",
            text = "Template with math ∑∆π∞ and {slot} symbols √∫∂",
            family = "math_symbols_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Math symbols template should be created", mathSymbolsTemplate)
        assertTrue("Should contain sum symbol", mathSymbolsTemplate.text.contains("∑"))
        assertTrue("Should contain delta symbol", mathSymbolsTemplate.text.contains("∆"))
        assertTrue("Should contain pi symbol", mathSymbolsTemplate.text.contains("π"))
        assertTrue("Should contain infinity symbol", mathSymbolsTemplate.text.contains("∞"))
        assertTrue("Should contain square root symbol", mathSymbolsTemplate.text.contains("√"))
        assertTrue("Should contain integral symbol", mathSymbolsTemplate.text.contains("∫"))
        assertTrue("Should contain partial symbol", mathSymbolsTemplate.text.contains("∂"))
        assertTrue("Should contain slot", mathSymbolsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with programming code correctly`() {
        // Arrange & Act
        val codeLikeTemplate = TemplateEntity(
            id = "code_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with code-like if (condition) { doSomething(); } and {slot}",
            family = "code_like_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Code-like template should be created", codeLikeTemplate)
        assertTrue("Should contain if statement", codeLikeTemplate.text.contains("if (condition)"))
        assertTrue("Should contain braces", codeLikeTemplate.text.contains("{ doSomething(); }"))
        assertTrue("Should contain slot", codeLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with URLs correctly`() {
        // Arrange & Act
        val urlTemplate = TemplateEntity(
            id = "url_template",
            game = "ROAST_CONSENSUS",
            text = "Template with https://example.com and {slot} URLs ftp://test.org",
            family = "url_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("URL template should be created", urlTemplate)
        assertTrue("Should contain HTTPS URL", urlTemplate.text.contains("https://example.com"))
        assertTrue("Should contain FTP URL", urlTemplate.text.contains("ftp://test.org"))
        assertTrue("Should contain slot", urlTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with email addresses correctly`() {
        // Arrange & Act
        val emailTemplate = TemplateEntity(
            id = "email_template",
            game = "ROAST_CONSENSUS",
            text = "Template with user@example.com and {slot} emails test.email+tag@domain.co.uk",
            family = "email_family",
            spice = 2,
            locality = 2,
            maxWords = 25
        )

        // Assert
        assertNotNull("Email template should be created", emailTemplate)
        assertTrue("Should contain email", emailTemplate.text.contains("user@example.com"))
        assertTrue("Should contain complex email", emailTemplate.text.contains("test.email+tag@domain.co.uk"))
        assertTrue("Should contain slot", emailTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with file paths correctly`() {
        // Arrange & Act
        val filePathTemplate = TemplateEntity(
            id = "filepath_template",
            game = "ROAST_CONSENSUS",
            text = "Template with /usr/local/bin and {slot} paths C:\\Windows\\System32",
            family = "filepath_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("File path template should be created", filePathTemplate)
        assertTrue("Should contain Unix path", filePathTemplate.text.contains("/usr/local/bin"))
        assertTrue("Should contain Windows path", filePathTemplate.text.contains("C:\\Windows\\System32"))
        assertTrue("Should contain slot", filePathTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with JSON-like content correctly`() {
        // Arrange & Act
        val jsonLikeTemplate = TemplateEntity(
            id = "json_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {\"key\": \"value\", \"number\": 123} and {slot} objects",
            family = "json_like_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("JSON-like template should be created", jsonLikeTemplate)
        assertTrue("Should contain JSON object", jsonLikeTemplate.text.contains("{\"key\": \"value\", \"number\": 123}"))
        assertTrue("Should contain slot", jsonLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with XML-like content correctly`() {
        // Arrange & Act
        val xmlLikeTemplate = TemplateEntity(
            id = "xml_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with <tag attribute=\"value\">content</tag> and {slot} elements",
            family = "xml_like_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("XML-like template should be created", xmlLikeTemplate)
        assertTrue("Should contain XML tag", xmlLikeTemplate.text.contains("<tag attribute=\"value\">content</tag>"))
        assertTrue("Should contain slot", xmlLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with SQL-like content correctly`() {
        // Arrange & Act
        val sqlLikeTemplate = TemplateEntity(
            id = "sql_like_template",
            game = "ROAST_CONSENSUS",
            text = "Template with SELECT * FROM table WHERE condition = 'value' and {slot}",
            family = "sql_like_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("SQL-like template should be created", sqlLikeTemplate)
        assertTrue("Should contain SQL query", sqlLikeTemplate.text.contains("SELECT * FROM table WHERE condition = 'value'"))
        assertTrue("Should contain slot", sqlLikeTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with extremely nested braces correctly`() {
        // Arrange & Act
        val extremelyNestedTemplate = TemplateEntity(
            id = "extremely_nested_template",
            game = "ROAST_CONSENSUS",
            text = "Template {{{{{{{{{with}}}}}}}}}} extremely {{{{{{{nested}}}}}}}} braces and {slot}",
            family = "extremely_nested_family",
            spice = 3,
            locality = 3,
            maxWords = 25
        )

        // Assert
        assertNotNull("Extremely nested template should be created", extremelyNestedTemplate)
        assertTrue("Should contain extremely nested braces",
            extremelyNestedTemplate.text.contains("{{{{{{{{{with}}}}}}}}}"))
        assertTrue("Should contain another extremely nested braces",
            extremelyNestedTemplate.text.contains("{{{{{{{nested}}}}}}}}"))
        assertTrue("Should contain slot", extremelyNestedTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with mixed quote types correctly`() {
        // Arrange & Act
        val mixedQuotesTemplate = TemplateEntity(
            id = "mixed_quotes_template",
            game = "ROAST_CONSENSUS",
            text = "Template with \"double quotes\" and 'single quotes' and `backticks` and {slot}",
            family = "mixed_quotes_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Mixed quotes template should be created", mixedQuotesTemplate)
        assertTrue("Should contain double quotes", mixedQuotesTemplate.text.contains("\"double quotes\""))
        assertTrue("Should contain single quotes", mixedQuotesTemplate.text.contains("'single quotes'"))
        assertTrue("Should contain backticks", mixedQuotesTemplate.text.contains("`backticks`"))
        assertTrue("Should contain slot", mixedQuotesTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with invisible characters correctly`() {
        // Arrange & Act
        val invisibleCharsTemplate = TemplateEntity(
            id = "invisible_chars_template",
            game = "ROAST_CONSENSUS",
            text = "Template\u0000with\u0001invisible\u0002chars\u0003and {slot}",
            family = "invisible_chars_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Invisible chars template should be created", invisibleCharsTemplate)
        assertTrue("Should contain invisible characters", invisibleCharsTemplate.text.contains("\u0000"))
        assertTrue("Should contain slot", invisibleCharsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with right-to-left text correctly`() {
        // Arrange & Act
        val rtlTemplate = TemplateEntity(
            id = "rtl_template",
            game = "ROAST_CONSENSUS",
            text = "Template with RTL العربية ועברית and {slot} text",
            family = "rtl_family",
            spice = 2,
            locality = 3,
            maxWords = 15
        )

        // Assert
        assertNotNull("RTL template should be created", rtlTemplate)
        assertTrue("Should contain Arabic text", rtlTemplate.text.contains("العربية"))
        assertTrue("Should contain Hebrew text", rtlTemplate.text.contains("עברית"))
        assertTrue("Should contain slot", rtlTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with combining characters correctly`() {
        // Arrange & Act
        val combiningCharsTemplate = TemplateEntity(
            id = "combining_chars_template",
            game = "ROAST_CONSENSUS",
            text = "Template with combining áéíóú and {slot} characters",
            family = "combining_chars_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Combining chars template should be created", combiningCharsTemplate)
        assertTrue("Should contain combining characters", combiningCharsTemplate.text.contains("á"))
        assertTrue("Should contain more combining characters", combiningCharsTemplate.text.contains("é"))
        assertTrue("Should contain slot", combiningCharsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with zero-width characters correctly`() {
        // Arrange & Act
        val zeroWidthTemplate = TemplateEntity(
            id = "zero_width_template",
            game = "ROAST_CONSENSUS",
            text = "Template\u200Bwith\u200Czero\u200Dwidth\u200Echaracters\u200Fand {slot}",
            family = "zero_width_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Zero width template should be created", zeroWidthTemplate)
        assertTrue("Should contain zero-width characters", zeroWidthTemplate.text.contains("\u200B"))
        assertTrue("Should contain slot", zeroWidthTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with extremely long slot names correctly`() {
        // Arrange & Act
        val longSlotName = "a".repeat(1000) + "_extremely_long_slot_name_" + "b".repeat(1000)
        val longSlotTemplate = TemplateEntity(
            id = "long_slot_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {$longSlotName} and {slot} slots",
            family = "long_slot_family",
            spice = 2,
            locality = 2,
            maxWords = 50
        )

        // Assert
        assertNotNull("Long slot template should be created", longSlotTemplate)
        assertTrue("Should contain extremely long slot name",
            longSlotTemplate.text.contains("{$longSlotName}"))
        assertTrue("Should contain normal slot", longSlotTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with many different slot types correctly`() {
        // Arrange & Act
        val manySlotTypesTemplate = TemplateEntity(
            id = "many_slot_types_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {noun}, {verb}, {adjective}, {place}, {action}, {object}, {emotion}, {slot}",
            family = "many_slot_types_family",
            spice = 3,
            locality = 3,
            maxWords = 20
        )

        // Assert
        assertNotNull("Many slot types template should be created", manySlotTypesTemplate)
        assertTrue("Should contain noun slot", manySlotTypesTemplate.text.contains("{noun}"))
        assertTrue("Should contain verb slot", manySlotTypesTemplate.text.contains("{verb}"))
        assertTrue("Should contain adjective slot", manySlotTypesTemplate.text.contains("{adjective}"))
        assertTrue("Should contain place slot", manySlotTypesTemplate.text.contains("{place}"))
        assertTrue("Should contain action slot", manySlotTypesTemplate.text.contains("{action}"))
        assertTrue("Should contain object slot", manySlotTypesTemplate.text.contains("{object}"))
        assertTrue("Should contain emotion slot", manySlotTypesTemplate.text.contains("{emotion}"))
        assertTrue("Should contain generic slot", manySlotTypesTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with slots at boundaries correctly`() {
        // Arrange & Act
        val boundarySlotsTemplate = TemplateEntity(
            id = "boundary_slots_template",
            game = "ROAST_CONSENSUS",
            text = "{slot} at start, middle {slot} and end {slot}",
            family = "boundary_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Boundary slots template should be created", boundarySlotsTemplate)
        assertTrue("Should start with slot", boundarySlotsTemplate.text.startsWith("{slot}"))
        assertTrue("Should contain middle slot", boundarySlotsTemplate.text.contains("middle {slot}"))
        assertTrue("Should end with slot", boundarySlotsTemplate.text.endsWith("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with consecutive slots correctly`() {
        // Arrange & Act
        val consecutiveSlotsTemplate = TemplateEntity(
            id = "consecutive_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {slot1}{slot2}{slot3}{slot4}{slot5} consecutive slots",
            family = "consecutive_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Consecutive slots template should be created", consecutiveSlotsTemplate)
        assertTrue("Should contain consecutive slots",
            consecutiveSlotsTemplate.text.contains("{slot1}{slot2}{slot3}{slot4}{slot5}"))
    }

    @Test
    fun `TemplateEntity handles templates with slots containing special slot names correctly`() {
        // Arrange & Act
        val specialSlotNamesTemplate = TemplateEntity(
            id = "special_slot_names_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {slot-with-dashes}, {slot_with_underscores}, {slot.with.dots}, and {slot}",
            family = "special_slot_names_family",
            spice = 2,
            locality = 2,
            maxWords = 20
        )

        // Assert
        assertNotNull("Special slot names template should be created", specialSlotNamesTemplate)
        assertTrue("Should contain dash slot", specialSlotNamesTemplate.text.contains("{slot-with-dashes}"))
        assertTrue("Should contain underscore slot", specialSlotNamesTemplate.text.contains("{slot_with_underscores}"))
        assertTrue("Should contain dot slot", specialSlotNamesTemplate.text.contains("{slot.with.dots}"))
        assertTrue("Should contain normal slot", specialSlotNamesTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with slots containing numbers correctly`() {
        // Arrange & Act
        val numericSlotsTemplate = TemplateEntity(
            id = "numeric_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {slot1}, {slot2}, {slot123}, {slot}, and {slot0}",
            family = "numeric_slots_family",
            spice = 2,
            locality = 2,
            maxWords = 15
        )

        // Assert
        assertNotNull("Numeric slots template should be created", numericSlotsTemplate)
        assertTrue("Should contain slot1", numericSlotsTemplate.text.contains("{slot1}"))
        assertTrue("Should contain slot2", numericSlotsTemplate.text.contains("{slot2}"))
        assertTrue("Should contain slot123", numericSlotsTemplate.text.contains("{slot123}"))
        assertTrue("Should contain slot0", numericSlotsTemplate.text.contains("{slot0}"))
        assertTrue("Should contain normal slot", numericSlotsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with slots containing unicode correctly`() {
        // Arrange & Act
        val unicodeSlotsTemplate = TemplateEntity(
            id = "unicode_slots_template",
            game = "ROAST_CONSENSUS",
            text = "Template with {ünícódé_slöt}, {ñémé_2}, {слот}, and {slot}",
            family = "unicode_slots_family",
            spice = 2,
            locality = 3,
            maxWords = 15
        )

        // Assert
        assertNotNull("Unicode slots template should be created", unicodeSlotsTemplate)
        assertTrue("Should contain unicode slot 1", unicodeSlotsTemplate.text.contains("{ünícódé_slöt}"))
        assertTrue("Should contain unicode slot 2", unicodeSlotsTemplate.text.contains("{ñémé_2}"))
        assertTrue("Should contain cyrillic slot", unicodeSlotsTemplate.text.contains("{слот}"))
        assertTrue("Should contain normal slot", unicodeSlotsTemplate.text.contains("{slot}"))
    }

    @Test
    fun `TemplateEntity handles templates with extremely large spice values correctly`() {
        // Arrange & Act
        val largeSpiceTemplate = TemplateEntity(
            id = "large_spice_template",
            game = "ROAST_CONSENSUS",
            text = "Large spice template {slot}",
            family = "large_spice_family",
            spice = Int.MAX_VALUE,
            locality = 2,
            maxWords = 10
        )

        // Assert
        assertNotNull("Large spice template should be created", largeSpiceTemplate)
        assertEquals("Spice should be Int.MAX_VALUE", Int.MAX_VALUE, largeSpiceTemplate.spice)
    }

    @Test
    fun `TemplateEntity handles templates with extremely large locality values correctly`() {
        // Arrange & Act
        val largeLocalityTemplate = TemplateEntity(
            id = "large_locality_template",
            game = "ROAST_CONSENSUS",
            text = "Large locality template {slot}",
            family = "large_locality_family",
            spice = 2,
            locality = Int.MAX_VALUE,
            maxWords = 10
        )

        // Assert
        assertNotNull("Large locality template should be created", largeLocalityTemplate)
        assertEquals("Locality should be Int.MAX_VALUE", Int.MAX_VALUE, largeLocalityTemplate.locality)
    }

    @Test
    fun `TemplateEntity handles templates with all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseTemplate = TemplateEntity(
            id = "A".repeat(1000),
            game = "ROAST_CONSENSUS",
            text = "Edge case template with {ünícódé_slöt_ñ}, emojis 🚀🌟, numbers 123.456, quotes \"'\", and {{{nested}}} braces",
            family = "B".repeat(1000),
            spice = Int.MAX_VALUE,
            locality = Int.MAX_VALUE,
            maxWords = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Edge case template should be created", edgeCaseTemplate)
        assertEquals("ID should be extremely long", 1000, edgeCaseTemplate.id.length)
        assertEquals("Family should be extremely long", 1000, edgeCaseTemplate.family.length)
        assertTrue("Should contain unicode slot", edgeCaseTemplate.text.contains("{ünícódé_slöt_ñ}"))
        assertTrue("Should contain emojis", edgeCaseTemplate.text.contains("🚀") && edgeCaseTemplate.text.contains("🌟"))
        assertTrue("Should contain numbers", edgeCaseTemplate.text.contains("123.456"))
        assertTrue("Should contain quotes", edgeCaseTemplate.text.contains("\"'\""))
        assertTrue("Should contain nested braces", edgeCaseTemplate.text.contains("{{{nested}}}"))
        assertEquals("Spice should be maximum", Int.MAX_VALUE, edgeCaseTemplate.spice)
        assertEquals("Locality should be maximum", Int.MAX_VALUE, edgeCaseTemplate.locality)
        assertEquals("MaxWords should be maximum", Int.MAX_VALUE, edgeCaseTemplate.maxWords)
    }

    @Test
    fun `TemplateEntity equality works correctly`() {
        // Arrange
        val template1 = TemplateEntity(
            id = "test_id",
            game = "ROAST_CONSENSUS",
            text = "Test {slot}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        val template2 = TemplateEntity(
            id = "test_id",
            game = "ROAST_CONSENSUS",
            text = "Test {slot}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        val template3 = TemplateEntity(
            id = "different_id",
            game = "ROAST_CONSENSUS",
            text = "Test {slot}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        // Assert
        assertEquals("Identical templates should be equal", template1, template2)
        assertNotEquals("Different templates should not be equal", template1, template3)
        assertEquals("Hash codes should be equal for identical templates", template1.hashCode(), template2.hashCode())
        assertTrue("Hash codes should be different for different templates", template1.hashCode() != template3.hashCode())
    }

    @Test
    fun `TemplateEntity toString contains relevant information`() {
        // Arrange
        val template = TemplateEntity(
            id = "test_id",
            game = "ROAST_CONSENSUS",
            text = "Test {slot}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        // Act
        val toString = template.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("test_id"))
        assertTrue("toString should contain game", toString.contains("ROAST_CONSENSUS"))
        assertTrue("toString should contain text", toString.contains("Test {slot}"))
        assertTrue("toString should contain family", toString.contains("test_family"))
        assertTrue("toString should contain spice", toString.contains("spice=2"))
        assertTrue("toString should contain locality", toString.contains("locality=1"))
        assertTrue("toString should contain maxWords", toString.contains("maxWords=16"))
    }
}