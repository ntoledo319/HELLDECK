package com.helldeck.data.entities

import com.helldeck.data.RoundEntity
import com.helldeck.engine.Feedback
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RoundEntity validation and behavior
 */
class RoundEntityTest {

    @Test
    fun `RoundEntity with valid data creates successfully`() {
        // Arrange & Act
        val round = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test filled text",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Round should be created", round)
        assertEquals("ID should match", 1L, round.id)
        assertEquals("Session ID should match", 1L, round.sessionId)
        assertEquals("Template ID should match", "test_template", round.templateId)
        assertEquals("Game should match", "ROAST_CONSENSUS", round.game)
        assertEquals("Filled text should match", "Test filled text", round.filledText)
        assertEquals("Points should match", 2, round.points)
        assertEquals("Timestamp should match", 1000L, round.timestamp)
    }

    @Test
    fun `RoundEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val round = RoundEntity(
            id = 0L,
            sessionId = 0L,
            templateId = "",
            game = "",
            filledText = "",
            feedbackJson = "{}",
            points = 0,
            timestamp = 0L
        )

        // Assert
        assertNotNull("Minimal round should be created", round)
        assertEquals("Minimal ID should match", 0L, round.id)
        assertEquals("Minimal session ID should match", 0L, round.sessionId)
        assertEquals("Minimal template ID should match", "", round.templateId)
        assertEquals("Minimal game should match", "", round.game)
        assertEquals("Minimal filled text should match", "", round.filledText)
        assertEquals("Minimal points should match", 0, round.points)
        assertEquals("Minimal timestamp should match", 0L, round.timestamp)
    }

    @Test
    fun `RoundEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val round = RoundEntity(
            id = Long.MAX_VALUE,
            sessionId = Long.MAX_VALUE,
            templateId = "A".repeat(100),
            game = "ROAST_CONSENSUS",
            filledText = "B".repeat(1000),
            feedbackJson = Gson().toJson(Feedback(lol = 10, meh = 10, trash = 10, latencyMs = 30000)),
            points = Int.MAX_VALUE,
            timestamp = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximal round should be created", round)
        assertEquals("Maximal ID should match", Long.MAX_VALUE, round.id)
        assertEquals("Maximal session ID should match", Long.MAX_VALUE, round.sessionId)
        assertEquals("Maximal template ID should match", "A".repeat(100), round.templateId)
        assertEquals("Maximal filled text should match", "B".repeat(1000), round.filledText)
        assertEquals("Maximal points should match", Int.MAX_VALUE, round.points)
        assertEquals("Maximal timestamp should match", Long.MAX_VALUE, round.timestamp)
    }

    @Test
    fun `RoundEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "original_template",
            game = "ROAST_CONSENSUS",
            filledText = "Original text",
            feedbackJson = Gson().toJson(Feedback(lol = 1, meh = 0, trash = 0, latencyMs = 1000)),
            points = 1,
            timestamp = 1000L
        )

        // Act
        val copiedRound = originalRound.copy(
            filledText = "Copied text",
            points = 3
        )

        // Assert
        assertNotNull("Copied round should not be null", copiedRound)
        assertEquals("ID should remain same", originalRound.id, copiedRound.id)
        assertEquals("Session ID should remain same", originalRound.sessionId, copiedRound.sessionId)
        assertEquals("Template ID should remain same", originalRound.templateId, copiedRound.templateId)
        assertEquals("Game should remain same", originalRound.game, copiedRound.game)
        assertEquals("Feedback JSON should remain same", originalRound.feedbackJson, copiedRound.feedbackJson)
        assertEquals("Timestamp should remain same", originalRound.timestamp, copiedRound.timestamp)

        assertEquals("Filled text should be updated", "Copied text", copiedRound.filledText)
        assertEquals("Points should be updated", 3, copiedRound.points)

        assertNotEquals("Original and copy should not be same object", originalRound, copiedRound)
    }

    @Test
    fun `RoundEntity handles special characters correctly`() {
        // Arrange & Act
        val specialRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "spécial_tëmplate_ñ",
            game = "ROAST_CONSENSUS",
            filledText = "Filled text with spécial çharácters and émojis 🚀!",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1500)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Special round should be created", specialRound)
        assertEquals("Special template ID should be preserved", "spécial_tëmplate_ñ", specialRound.templateId)
        assertEquals("Special filled text should be preserved",
            "Filled text with spécial çharácters and émojis 🚀!", specialRound.filledText)
    }

    @Test
    fun `RoundEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "ünícódé_tëst",
            game = "ROAST_CONSENSUS",
            filledText = "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯",
            feedbackJson = Gson().toJson(Feedback(lol = 3, meh = 0, trash = 0, latencyMs = 800)),
            points = 3,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Unicode round should be created", unicodeRound)
        assertEquals("Unicode template ID should be preserved", "ünícódé_tëst", unicodeRound.templateId)
        assertEquals("Unicode filled text should be preserved",
            "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯", unicodeRound.filledText)
    }

    @Test
    fun `RoundEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptyRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "",
            game = "",
            filledText = "",
            feedbackJson = "{}",
            points = 0,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Empty round should be created", emptyRound)
        assertEquals("Empty template ID should be preserved", "", emptyRound.templateId)
        assertEquals("Empty game should be preserved", "", emptyRound.game)
        assertEquals("Empty filled text should be preserved", "", emptyRound.filledText)
    }

    @Test
    fun `RoundEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespaceRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "   ",
            game = "   ",
            filledText = "   ",
            feedbackJson = "{}",
            points = 0,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Whitespace round should be created", whitespaceRound)
        assertEquals("Whitespace template ID should be preserved", "   ", whitespaceRound.templateId)
        assertEquals("Whitespace game should be preserved", "   ", whitespaceRound.game)
        assertEquals("Whitespace filled text should be preserved", "   ", whitespaceRound.filledText)
    }

    @Test
    fun `RoundEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundaryRound = RoundEntity(
            id = 0L,
            sessionId = 0L,
            templateId = "boundary_test",
            game = "ROAST_CONSENSUS",
            filledText = "Boundary test",
            feedbackJson = "{}",
            points = 0,
            timestamp = 0L
        )

        // Assert
        assertNotNull("Boundary round should be created", boundaryRound)
        assertEquals("Zero ID should be preserved", 0L, boundaryRound.id)
        assertEquals("Zero session ID should be preserved", 0L, boundaryRound.sessionId)
        assertEquals("Zero points should be preserved", 0, boundaryRound.points)
        assertEquals("Zero timestamp should be preserved", 0L, boundaryRound.timestamp)
    }

    @Test
    fun `RoundEntity handles very long filled text correctly`() {
        // Arrange & Act
        val longText = "A".repeat(1000) + " Very Long Filled Text " + "B".repeat(1000)
        val longTextRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "long_text_template",
            game = "ROAST_CONSENSUS",
            filledText = longText,
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Long text round should be created", longTextRound)
        assertEquals("Long text should be preserved", longText, longTextRound.filledText)
        assertEquals("Long text length should match", 2027, longTextRound.filledText.length)
    }

    @Test
    fun `RoundEntity handles very long template ID correctly`() {
        // Arrange & Act
        val longTemplateId = "A".repeat(1000) + "_very_long_template_id_" + "B".repeat(1000)
        val longTemplateIdRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = longTemplateId,
            game = "ROAST_CONSENSUS",
            filledText = "Long template ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Long template ID round should be created", longTemplateIdRound)
        assertEquals("Long template ID should be preserved", longTemplateId, longTemplateIdRound.templateId)
        assertEquals("Long template ID length should match", 2023, longTemplateIdRound.templateId.length)
    }

    @Test
    fun `RoundEntity handles negative points correctly`() {
        // Arrange & Act
        val negativePointsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "negative_points_template",
            game = "ROAST_CONSENSUS",
            filledText = "Negative points round",
            feedbackJson = Gson().toJson(Feedback(lol = 0, meh = 1, trash = 3, latencyMs = 5000)),
            points = -5,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Negative points round should be created", negativePointsRound)
        assertEquals("Negative points should be preserved", -5, negativePointsRound.points)
    }

    @Test
    fun `RoundEntity handles zero points correctly`() {
        // Arrange & Act
        val zeroPointsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "zero_points_template",
            game = "ROAST_CONSENSUS",
            filledText = "Zero points round",
            feedbackJson = Gson().toJson(Feedback(lol = 0, meh = 0, trash = 0, latencyMs = 1000)),
            points = 0,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Zero points round should be created", zeroPointsRound)
        assertEquals("Zero points should be preserved", 0, zeroPointsRound.points)
    }

    @Test
    fun `RoundEntity handles high points correctly`() {
        // Arrange & Act
        val highPointsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "high_points_template",
            game = "ROAST_CONSENSUS",
            filledText = "High points round",
            feedbackJson = Gson().toJson(Feedback(lol = 5, meh = 0, trash = 0, latencyMs = 800)),
            points = 9999,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("High points round should be created", highPointsRound)
        assertEquals("High points should be preserved", 9999, highPointsRound.points)
    }

    @Test
    fun `RoundEntity handles extremely high points correctly`() {
        // Arrange & Act
        val extremePointsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "extreme_points_template",
            game = "ROAST_CONSENSUS",
            filledText = "Extreme points round",
            feedbackJson = Gson().toJson(Feedback(lol = 10, meh = 0, trash = 0, latencyMs = 500)),
            points = Int.MAX_VALUE,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Extreme points round should be created", extremePointsRound)
        assertEquals("Extreme points should be preserved", Int.MAX_VALUE, extremePointsRound.points)
    }

    @Test
    fun `RoundEntity handles extremely low points correctly`() {
        // Arrange & Act
        val extremeLowPointsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "extreme_low_points_template",
            game = "ROAST_CONSENSUS",
            filledText = "Extreme low points round",
            feedbackJson = Gson().toJson(Feedback(lol = 0, meh = 0, trash = 10, latencyMs = 10000)),
            points = Int.MIN_VALUE,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Extreme low points round should be created", extremeLowPointsRound)
        assertEquals("Extreme low points should be preserved", Int.MIN_VALUE, extremeLowPointsRound.points)
    }

    @Test
    fun `RoundEntity handles emojis in filled text correctly`() {
        // Arrange & Act
        val emojiRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "emoji_template",
            game = "ROAST_CONSENSUS",
            filledText = "Emoji round 🚀 with 🌟 emojis ⭐ in text",
            feedbackJson = Gson().toJson(Feedback(lol = 3, meh = 0, trash = 0, latencyMs = 600)),
            points = 3,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Emoji round should be created", emojiRound)
        assertTrue("Filled text should contain rocket emoji", emojiRound.filledText.contains("🚀"))
        assertTrue("Filled text should contain star emoji", emojiRound.filledText.contains("🌟"))
        assertTrue("Filled text should contain star emoji", emojiRound.filledText.contains("⭐"))
    }

    @Test
    fun `RoundEntity handles numbers in filled text correctly`() {
        // Arrange & Act
        val numbersRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "numbers_template",
            game = "ROAST_CONSENSUS",
            filledText = "Numbers round 123 with 456.789 decimals",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1200)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Numbers round should be created", numbersRound)
        assertTrue("Filled text should contain numbers", numbersRound.filledText.contains("123"))
        assertTrue("Filled text should contain decimal", numbersRound.filledText.contains("456.789"))
    }

    @Test
    fun `RoundEntity handles special regex characters in filled text correctly`() {
        // Arrange & Act
        val regexCharsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "regex_chars_template",
            game = "ROAST_CONSENSUS",
            filledText = "Regex chars round ^$*+?{}[]\\|() content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1100)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Regex chars round should be created", regexCharsRound)
        assertTrue("Filled text should contain regex characters", regexCharsRound.filledText.contains("^$*+?{}[]\\|()"))
    }

    @Test
    fun `RoundEntity handles quotes in filled text correctly`() {
        // Arrange & Act
        val quotesRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "quotes_template",
            game = "ROAST_CONSENSUS",
            filledText = "Quotes round \"with double\" and 'single quotes'",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1300)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Quotes round should be created", quotesRound)
        assertTrue("Filled text should contain double quotes", quotesRound.filledText.contains("\"with double\""))
        assertTrue("Filled text should contain single quotes", quotesRound.filledText.contains("'single quotes'"))
    }

    @Test
    fun `RoundEntity handles newlines in filled text correctly`() {
        // Arrange & Act
        val newlinesRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "newlines_template",
            game = "ROAST_CONSENSUS",
            filledText = "Newlines round\nwith\r\nnewlines",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1400)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Newlines round should be created", newlinesRound)
        assertTrue("Filled text should contain newlines", newlinesRound.filledText.contains("\n"))
        assertTrue("Filled text should contain carriage returns", newlinesRound.filledText.contains("\r"))
    }

    @Test
    fun `RoundEntity handles tabs in filled text correctly`() {
        // Arrange & Act
        val tabsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "tabs_template",
            game = "ROAST_CONSENSUS",
            filledText = "Tabs round\twith\ttabs",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1500)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Tabs round should be created", tabsRound)
        assertTrue("Filled text should contain tabs", tabsRound.filledText.contains("\t"))
    }

    @Test
    fun `RoundEntity handles HTML-like content in filled text correctly`() {
        // Arrange & Act
        val htmlLikeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "html_like_template",
            game = "ROAST_CONSENSUS",
            filledText = "HTML-like round <with> <html> like & content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1600)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("HTML-like round should be created", htmlLikeRound)
        assertTrue("Filled text should contain angle brackets", htmlLikeRound.filledText.contains("<with>"))
        assertTrue("Filled text should contain HTML tag", htmlLikeRound.filledText.contains("<html>"))
        assertTrue("Filled text should contain HTML entity", htmlLikeRound.filledText.contains("&"))
    }

    @Test
    fun `RoundEntity handles URLs in filled text correctly`() {
        // Arrange & Act
        val urlRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "url_template",
            game = "ROAST_CONSENSUS",
            filledText = "URL round https://example.com with ftp://test.org URLs",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1700)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("URL round should be created", urlRound)
        assertTrue("Filled text should contain HTTPS URL", urlRound.filledText.contains("https://example.com"))
        assertTrue("Filled text should contain FTP URL", urlRound.filledText.contains("ftp://test.org"))
    }

    @Test
    fun `RoundEntity handles email addresses in filled text correctly`() {
        // Arrange & Act
        val emailRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "email_template",
            game = "ROAST_CONSENSUS",
            filledText = "Email round user@example.com and test.email+tag@domain.co.uk",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1800)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Email round should be created", emailRound)
        assertTrue("Filled text should contain email", emailRound.filledText.contains("user@example.com"))
        assertTrue("Filled text should contain complex email", emailRound.filledText.contains("test.email+tag@domain.co.uk"))
    }

    @Test
    fun `RoundEntity handles file paths in filled text correctly`() {
        // Arrange & Act
        val filePathRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "filepath_template",
            game = "ROAST_CONSENSUS",
            filledText = "File path round /usr/local/bin and C:\\Windows\\System32 paths",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1900)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("File path round should be created", filePathRound)
        assertTrue("Filled text should contain Unix path", filePathRound.filledText.contains("/usr/local/bin"))
        assertTrue("Filled text should contain Windows path", filePathRound.filledText.contains("C:\\Windows\\System32"))
    }

    @Test
    fun `RoundEntity handles JSON-like content in filled text correctly`() {
        // Arrange & Act
        val jsonLikeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "json_like_template",
            game = "ROAST_CONSENSUS",
            filledText = "JSON-like round {\"key\": \"value\", \"number\": 123} content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("JSON-like round should be created", jsonLikeRound)
        assertTrue("Filled text should contain JSON object", jsonLikeRound.filledText.contains("{\"key\": \"value\", \"number\": 123}"))
    }

    @Test
    fun `RoundEntity handles SQL-like content in filled text correctly`() {
        // Arrange & Act
        val sqlLikeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "sql_like_template",
            game = "ROAST_CONSENSUS",
            filledText = "SQL-like round SELECT * FROM table WHERE condition = 'value'",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2100)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("SQL-like round should be created", sqlLikeRound)
        assertTrue("Filled text should contain SQL query", sqlLikeRound.filledText.contains("SELECT * FROM table WHERE condition = 'value'"))
    }

    @Test
    fun `RoundEntity handles programming code in filled text correctly`() {
        // Arrange & Act
        val codeLikeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "code_like_template",
            game = "ROAST_CONSENSUS",
            filledText = "Code-like round if (points > 100) { celebrate(); } content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2200)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Code-like round should be created", codeLikeRound)
        assertTrue("Filled text should contain if statement", codeLikeRound.filledText.contains("if (points > 100)"))
        assertTrue("Filled text should contain braces", codeLikeRound.filledText.contains("{ celebrate(); }"))
    }

    @Test
    fun `RoundEntity handles mathematical symbols in filled text correctly`() {
        // Arrange & Act
        val mathSymbolsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "math_symbols_template",
            game = "ROAST_CONSENSUS",
            filledText = "Math symbols round ∑∆π∞ with √∫∂ symbols",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2300)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Math symbols round should be created", mathSymbolsRound)
        assertTrue("Filled text should contain sum symbol", mathSymbolsRound.filledText.contains("∑"))
        assertTrue("Filled text should contain delta symbol", mathSymbolsRound.filledText.contains("∆"))
        assertTrue("Filled text should contain pi symbol", mathSymbolsRound.filledText.contains("π"))
        assertTrue("Filled text should contain infinity symbol", mathSymbolsRound.filledText.contains("∞"))
        assertTrue("Filled text should contain square root symbol", mathSymbolsRound.filledText.contains("√"))
        assertTrue("Filled text should contain integral symbol", mathSymbolsRound.filledText.contains("∫"))
        assertTrue("Filled text should contain partial symbol", mathSymbolsRound.filledText.contains("∂"))
    }

    @Test
    fun `RoundEntity handles binary-like content in filled text correctly`() {
        // Arrange & Act
        val binaryLikeRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "binary_like_template",
            game = "ROAST_CONSENSUS",
            filledText = "Binary-like round 01010101 binary 11001100 content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2400)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Binary-like round should be created", binaryLikeRound)
        assertTrue("Filled text should contain binary-like content", binaryLikeRound.filledText.contains("01010101"))
        assertTrue("Filled text should contain more binary-like content", binaryLikeRound.filledText.contains("11001100"))
    }

    @Test
    fun `RoundEntity handles right-to-left text in filled text correctly`() {
        // Arrange & Act
        val rtlRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "rtl_template",
            game = "ROAST_CONSENSUS",
            filledText = "RTL round العربية ועברית RTL content",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2500)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("RTL round should be created", rtlRound)
        assertTrue("Filled text should contain Arabic text", rtlRound.filledText.contains("العربية"))
        assertTrue("Filled text should contain Hebrew text", rtlRound.filledText.contains("עברית"))
    }

    @Test
    fun `RoundEntity handles combining characters in filled text correctly`() {
        // Arrange & Act
        val combiningCharsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "combining_chars_template",
            game = "ROAST_CONSENSUS",
            filledText = "Combining chars round áéíóú with combining characters",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2600)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Combining chars round should be created", combiningCharsRound)
        assertTrue("Filled text should contain combining characters", combiningCharsRound.filledText.contains("á"))
        assertTrue("Filled text should contain more combining characters", combiningCharsRound.filledText.contains("é"))
    }

    @Test
    fun `RoundEntity handles zero-width characters in filled text correctly`() {
        // Arrange & Act
        val zeroWidthRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "zero_width_template",
            game = "ROAST_CONSENSUS",
            filledText = "Zero width round\u200Bwith\u200Czero\u200Dwidth\u200Echaracters\u200F",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2700)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Zero width round should be created", zeroWidthRound)
        assertTrue("Filled text should contain zero-width characters", zeroWidthRound.filledText.contains("\u200B"))
    }

    @Test
    fun `RoundEntity handles invisible characters in filled text correctly`() {
        // Arrange & Act
        val invisibleCharsRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "invisible_chars_template",
            game = "ROAST_CONSENSUS",
            filledText = "Invisible chars round\u0000with\u0001invisible\u0002chars\u0003",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 2800)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Invisible chars round should be created", invisibleCharsRound)
        assertTrue("Filled text should contain invisible characters", invisibleCharsRound.filledText.contains("\u0000"))
    }

    @Test
    fun `RoundEntity handles mixed content in filled text correctly`() {
        // Arrange & Act
        val mixedContentRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "mixed_content_template",
            game = "ROAST_CONSENSUS",
            filledText = "Mixed números 123, emojis 🚀🌟, spécial çharácters, \"quotes\", and móre!",
            feedbackJson = Gson().toJson(Feedback(lol = 3, meh = 0, trash = 0, latencyMs = 900)),
            points = 3,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Mixed content round should be created", mixedContentRound)
        assertTrue("Filled text should contain numbers", mixedContentRound.filledText.contains("123"))
        assertTrue("Filled text should contain emojis", mixedContentRound.filledText.contains("🚀") && mixedContentRound.filledText.contains("🌟"))
        assertTrue("Filled text should contain special characters", mixedContentRound.filledText.contains("çharácters"))
        assertTrue("Filled text should contain quotes", mixedContentRound.filledText.contains("\"quotes\""))
        assertTrue("Filled text should contain more accents", mixedContentRound.filledText.contains("móre"))
    }

    @Test
    fun `RoundEntity handles extremely long filled text correctly`() {
        // Arrange & Act
        val extremelyLongText = "A".repeat(10000) + " Extremely Long Filled Text " + "B".repeat(10000)
        val extremelyLongTextRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "extremely_long_text_template",
            game = "ROAST_CONSENSUS",
            filledText = extremelyLongText,
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Extremely long text round should be created", extremelyLongTextRound)
        assertEquals("Extremely long text should be preserved", extremelyLongText, extremelyLongTextRound.filledText)
        assertEquals("Extremely long text length should match", 40005, extremelyLongTextRound.filledText.length)
    }

    @Test
    fun `RoundEntity handles extremely long template ID correctly`() {
        // Arrange & Act
        val extremelyLongTemplateId = "A".repeat(10000) + "_extremely_long_template_id_" + "B".repeat(10000)
        val extremelyLongTemplateIdRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = extremelyLongTemplateId,
            game = "ROAST_CONSENSUS",
            filledText = "Extremely long template ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Extremely long template ID round should be created", extremelyLongTemplateIdRound)
        assertEquals("Extremely long template ID should be preserved", extremelyLongTemplateId, extremelyLongTemplateIdRound.templateId)
        assertEquals("Extremely long template ID length should match", 40023, extremelyLongTemplateIdRound.templateId.length)
    }

    @Test
    fun `RoundEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseRound = RoundEntity(
            id = Long.MAX_VALUE,
            sessionId = Long.MAX_VALUE,
            templateId = "A".repeat(1000),
            game = "ROAST_CONSENSUS",
            filledText = "Edge case: ünícódé 🚀🌟, ñúméros 123.456, \"'quotes'\", {{{nested}}}, \\commands, <tags>, ∑∆π∞, and móre!",
            feedbackJson = Gson().toJson(Feedback(lol = 10, meh = 10, trash = 10, latencyMs = 30000)),
            points = Int.MAX_VALUE,
            timestamp = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Edge case round should be created", edgeCaseRound)
        assertEquals("ID should be maximum", Long.MAX_VALUE, edgeCaseRound.id)
        assertEquals("Session ID should be maximum", Long.MAX_VALUE, edgeCaseRound.sessionId)
        assertEquals("Template ID should be extremely long", 1000, edgeCaseRound.templateId.length)
        assertEquals("Filled text should be extremely long", 124, edgeCaseRound.filledText.length)
        assertTrue("Filled text should contain unicode", edgeCaseRound.filledText.contains("ünícódé"))
        assertTrue("Filled text should contain emojis", edgeCaseRound.filledText.contains("🚀") && edgeCaseRound.filledText.contains("🌟"))
        assertTrue("Filled text should contain numbers", edgeCaseRound.filledText.contains("123.456"))
        assertTrue("Filled text should contain quotes", edgeCaseRound.filledText.contains("\"'quotes'\""))
        assertTrue("Filled text should contain nested braces", edgeCaseRound.filledText.contains("{{{nested}}}"))
        assertTrue("Filled text should contain commands", edgeCaseRound.filledText.contains("\\commands"))
        assertTrue("Filled text should contain tags", edgeCaseRound.filledText.contains("<tags>"))
        assertTrue("Filled text should contain math symbols", edgeCaseRound.filledText.contains("∑∆π∞"))
        assertTrue("Filled text should contain more accents", edgeCaseRound.filledText.contains("móre"))
        assertEquals("Points should be maximum", Int.MAX_VALUE, edgeCaseRound.points)
        assertEquals("Timestamp should be maximum", Long.MAX_VALUE, edgeCaseRound.timestamp)
    }

    @Test
    fun `RoundEntity equality works correctly`() {
        // Arrange
        val round1 = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test text",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        val round2 = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test text",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        val round3 = RoundEntity(
            id = 2L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test text",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertEquals("Identical rounds should be equal", round1, round2)
        assertNotEquals("Different rounds should not be equal", round1, round3)
        assertEquals("Hash codes should be equal for identical rounds", round1.hashCode(), round2.hashCode())
        assertTrue("Hash codes should be different for different rounds", round1.hashCode() != round3.hashCode())
    }

    @Test
    fun `RoundEntity toString contains relevant information`() {
        // Arrange
        val round = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test text",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Act
        val toString = round.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("id=1"))
        assertTrue("toString should contain session ID", toString.contains("sessionId=1"))
        assertTrue("toString should contain template ID", toString.contains("test_template"))
        assertTrue("toString should contain game", toString.contains("ROAST_CONSENSUS"))
        assertTrue("toString should contain filled text", toString.contains("Test text"))
        assertTrue("toString should contain points", toString.contains("points=2"))
        assertTrue("toString should contain timestamp", toString.contains("timestamp=1000"))
    }

    @Test
    fun `RoundEntity handles feedback JSON with special characters correctly`() {
        // Arrange & Act
        val specialFeedbackJson = Gson().toJson(Feedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000))
        val specialFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "special_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Special feedback round",
            feedbackJson = specialFeedbackJson,
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Special feedback round should be created", specialFeedbackRound)
        assertEquals("Special feedback JSON should be preserved", specialFeedbackJson, specialFeedbackRound.feedbackJson)
    }

    @Test
    fun `RoundEntity handles empty feedback JSON correctly`() {
        // Arrange & Act
        val emptyFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "empty_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Empty feedback round",
            feedbackJson = "{}",
            points = 0,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Empty feedback round should be created", emptyFeedbackRound)
        assertEquals("Empty feedback JSON should be preserved", "{}", emptyFeedbackRound.feedbackJson)
    }

    @Test
    fun `RoundEntity handles malformed feedback JSON correctly`() {
        // Arrange & Act
        val malformedFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "malformed_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Malformed feedback round",
            feedbackJson = "{\"lol\": 2, \"meh\": 1,}", // Trailing comma
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Malformed feedback round should be created", malformedFeedbackRound)
        assertEquals("Malformed feedback JSON should be preserved", "{\"lol\": 2, \"meh\": 1,}", malformedFeedbackRound.feedbackJson)
    }

    @Test
    fun `RoundEntity handles future timestamp correctly`() {
        // Arrange & Act
        val futureTimestamp = System.currentTimeMillis() + 1000000
        val futureTimestampRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "future_timestamp_template",
            game = "ROAST_CONSENSUS",
            filledText = "Future timestamp round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = futureTimestamp
        )

        // Assert
        assertNotNull("Future timestamp round should be created", futureTimestampRound)
        assertEquals("Future timestamp should be preserved", futureTimestamp, futureTimestampRound.timestamp)
    }

    @Test
    fun `RoundEntity handles zero timestamp correctly`() {
        // Arrange & Act
        val zeroTimestampRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "zero_timestamp_template",
            game = "ROAST_CONSENSUS",
            filledText = "Zero timestamp round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 0L
        )

        // Assert
        assertNotNull("Zero timestamp round should be created", zeroTimestampRound)
        assertEquals("Zero timestamp should be preserved", 0L, zeroTimestampRound.timestamp)
    }

    @Test
    fun `RoundEntity handles extremely large session ID correctly`() {
        // Arrange & Act
        val largeSessionIdRound = RoundEntity(
            id = 1L,
            sessionId = Long.MAX_VALUE,
            templateId = "large_session_id_template",
            game = "ROAST_CONSENSUS",
            filledText = "Large session ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Large session ID round should be created", largeSessionIdRound)
        assertEquals("Large session ID should be preserved", Long.MAX_VALUE, largeSessionIdRound.sessionId)
    }

    @Test
    fun `RoundEntity handles extremely small session ID correctly`() {
        // Arrange & Act
        val smallSessionIdRound = RoundEntity(
            id = 1L,
            sessionId = Long.MIN_VALUE,
            templateId = "small_session_id_template",
            game = "ROAST_CONSENSUS",
            filledText = "Small session ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Small session ID round should be created", smallSessionIdRound)
        assertEquals("Small session ID should be preserved", Long.MIN_VALUE, smallSessionIdRound.sessionId)
    }

    @Test
    fun `RoundEntity handles extremely large ID correctly`() {
        // Arrange & Act
        val largeIdRound = RoundEntity(
            id = Long.MAX_VALUE,
            sessionId = 1L,
            templateId = "large_id_template",
            game = "ROAST_CONSENSUS",
            filledText = "Large ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Large ID round should be created", largeIdRound)
        assertEquals("Large ID should be preserved", Long.MAX_VALUE, largeIdRound.id)
    }

    @Test
    fun `RoundEntity handles extremely small ID correctly`() {
        // Arrange & Act
        val smallIdRound = RoundEntity(
            id = Long.MIN_VALUE,
            sessionId = 1L,
            templateId = "small_id_template",
            game = "ROAST_CONSENSUS",
            filledText = "Small ID round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Small ID round should be created", smallIdRound)
        assertEquals("Small ID should be preserved", Long.MIN_VALUE, smallIdRound.id)
    }

    @Test
    fun `RoundEntity handles extremely large timestamp correctly`() {
        // Arrange & Act
        val largeTimestampRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "large_timestamp_template",
            game = "ROAST_CONSENSUS",
            filledText = "Large timestamp round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Large timestamp round should be created", largeTimestampRound)
        assertEquals("Large timestamp should be preserved", Long.MAX_VALUE, largeTimestampRound.timestamp)
    }

    @Test
    fun `RoundEntity handles extremely small timestamp correctly`() {
        // Arrange & Act
        val smallTimestampRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "small_timestamp_template",
            game = "ROAST_CONSENSUS",
            filledText = "Small timestamp round",
            feedbackJson = Gson().toJson(Feedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)),
            points = 2,
            timestamp = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Small timestamp round should be created", smallTimestampRound)
        assertEquals("Small timestamp should be preserved", Long.MIN_VALUE, smallTimestampRound.timestamp)
    }

    @Test
    fun `RoundEntity handles complex feedback JSON correctly`() {
        // Arrange & Act
        val complexFeedback = Feedback(lol = 5, meh = 3, trash = 1, latencyMs = 2500)
        val complexFeedbackJson = Gson().toJson(complexFeedback)
        val complexFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "complex_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Complex feedback round",
            feedbackJson = complexFeedbackJson,
            points = 4,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Complex feedback round should be created", complexFeedbackRound)
        assertEquals("Complex feedback JSON should be preserved", complexFeedbackJson, complexFeedbackRound.feedbackJson)
        assertEquals("Points should reflect complex feedback", 4, complexFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles negative feedback JSON correctly`() {
        // Arrange & Act
        val negativeFeedback = Feedback(lol = 0, meh = 1, trash = 5, latencyMs = 8000)
        val negativeFeedbackJson = Gson().toJson(negativeFeedback)
        val negativeFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "negative_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Negative feedback round",
            feedbackJson = negativeFeedbackJson,
            points = -3,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Negative feedback round should be created", negativeFeedbackRound)
        assertEquals("Negative feedback JSON should be preserved", negativeFeedbackJson, negativeFeedbackRound.feedbackJson)
        assertEquals("Points should reflect negative feedback", -3, negativeFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles zero feedback JSON correctly`() {
        // Arrange & Act
        val zeroFeedback = Feedback(lol = 0, meh = 0, trash = 0, latencyMs = 1000)
        val zeroFeedbackJson = Gson().toJson(zeroFeedback)
        val zeroFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "zero_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Zero feedback round",
            feedbackJson = zeroFeedbackJson,
            points = 0,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Zero feedback round should be created", zeroFeedbackRound)
        assertEquals("Zero feedback JSON should be preserved", zeroFeedbackJson, zeroFeedbackRound.feedbackJson)
        assertEquals("Points should reflect zero feedback", 0, zeroFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles maximum feedback JSON correctly`() {
        // Arrange & Act
        val maxFeedback = Feedback(lol = 10, meh = 10, trash = 10, latencyMs = 30000)
        val maxFeedbackJson = Gson().toJson(maxFeedback)
        val maxFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "max_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Maximum feedback round",
            feedbackJson = maxFeedbackJson,
            points = 10,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Maximum feedback round should be created", maxFeedbackRound)
        assertEquals("Maximum feedback JSON should be preserved", maxFeedbackJson, maxFeedbackRound.feedbackJson)
        assertEquals("Points should reflect maximum feedback", 10, maxFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles mixed feedback JSON correctly`() {
        // Arrange & Act
        val mixedFeedback = Feedback(lol = 3, meh = 2, trash = 1, latencyMs = 2000)
        val mixedFeedbackJson = Gson().toJson(mixedFeedback)
        val mixedFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "mixed_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Mixed feedback round",
            feedbackJson = mixedFeedbackJson,
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Mixed feedback round should be created", mixedFeedbackRound)
        assertEquals("Mixed feedback JSON should be preserved", mixedFeedbackJson, mixedFeedbackRound.feedbackJson)
        assertEquals("Points should reflect mixed feedback", 2, mixedFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles very fast latency feedback correctly`() {
        // Arrange & Act
        val fastFeedback = Feedback(lol = 5, meh = 0, trash = 0, latencyMs = 100)
        val fastFeedbackJson = Gson().toJson(fastFeedback)
        val fastFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "fast_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Fast feedback round",
            feedbackJson = fastFeedbackJson,
            points = 5,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Fast feedback round should be created", fastFeedbackRound)
        assertEquals("Fast feedback JSON should be preserved", fastFeedbackJson, fastFeedbackRound.feedbackJson)
        assertEquals("Points should reflect fast feedback", 5, fastFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles very slow latency feedback correctly`() {
        // Arrange & Act
        val slowFeedback = Feedback(lol = 1, meh = 0, trash = 0, latencyMs = 25000)
        val slowFeedbackJson = Gson().toJson(slowFeedback)
        val slowFeedbackRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "slow_feedback_template",
            game = "ROAST_CONSENSUS",
            filledText = "Slow feedback round",
            feedbackJson = slowFeedbackJson,
            points = 1,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Slow feedback round should be created", slowFeedbackRound)
        assertEquals("Slow feedback JSON should be preserved", slowFeedbackJson, slowFeedbackRound.feedbackJson)
        assertEquals("Points should reflect slow feedback", 1, slowFeedbackRound.points)
    }

    @Test
    fun `RoundEntity handles feedback with extreme latency values correctly`() {
        // Arrange & Act
        val extremeLatencyFeedback = Feedback(lol = 2, meh = 0, trash = 0, latencyMs = Int.MAX_VALUE)
        val extremeLatencyFeedbackJson = Gson().toJson(extremeLatencyFeedback)
        val extremeLatencyRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "extreme_latency_template",
            game = "ROAST_CONSENSUS",
            filledText = "Extreme latency round",
            feedbackJson = extremeLatencyFeedbackJson,
            points = 2,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Extreme latency round should be created", extremeLatencyRound)
        assertEquals("Extreme latency feedback JSON should be preserved", extremeLatencyFeedbackJson, extremeLatencyRound.feedbackJson)
        assertEquals("Points should reflect extreme latency feedback", 2, extremeLatencyRound.points)
    }

    @Test
    fun `RoundEntity handles feedback with zero latency correctly`() {
        // Arrange & Act
        val zeroLatencyFeedback = Feedback(lol = 3, meh = 0, trash = 0, latencyMs = 0)
        val zeroLatencyFeedbackJson = Gson().toJson(zeroLatencyFeedback)
        val zeroLatencyRound = RoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "zero_latency_template",
            game = "ROAST_CONSENSUS",
            filledText = "Zero latency round",
            feedbackJson = zeroLatencyFeedbackJson,
            points = 3,
            timestamp = 1000L
        )

        // Assert
        assertNotNull("Zero latency round should be created", zeroLatencyRound)
        assertEquals("Zero latency feedback JSON should be preserved", zeroLatencyFeedbackJson, zeroLatencyRound.feedbackJson)
        assertEquals("Points should reflect zero latency feedback", 3, zeroLatencyRound.points)
    }

    @Test
    fun `RoundEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundaryRound = RoundEntity(
            id = 0L,
            sessionId = 0L,
            templateId = "",
            game = "",
            filledText = "",
            feedbackJson = "{}",
            points = 0,
            timestamp = 0L
        )

        // Assert
        assertNotNull("Boundary round should be created", boundaryRound)
        assertEquals("Zero ID should be preserved", 0L, boundaryRound.id)
        assertEquals("Zero session ID should be preserved", 0L, boundaryRound.sessionId)
        assertEquals("Empty template ID should be preserved", "", boundaryRound.templateId)
        assertEquals("Empty game should be preserved", "", boundaryRound.game)
        assertEquals("Empty filled text should be preserved", "", boundaryRound.filledText)
        assertEquals("Empty feedback JSON should be preserved", "{}", boundaryRound.feedbackJson)
        assertEquals("Zero points should be preserved", 0, boundaryRound.points)
        assertEquals("Zero timestamp should be preserved", 0L, boundaryRound.timestamp)
    }

    @Test
    fun `RoundEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundaryRound = RoundEntity(
            id = Long.MAX_VALUE,
            sessionId = Long.MAX_VALUE,
            templateId = "A".repeat(1000),
            game = "ROAST_CONSENSUS",
            filledText = "B".repeat(1000),
            feedbackJson = Gson().toJson(Feedback(lol = 10, meh = 10, trash = 10, latencyMs = 30000)),
            points = Int.MAX_VALUE,
            timestamp = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximum boundary round should be created", maxBoundaryRound)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundaryRound.id)
        assertEquals("Maximum session ID should be preserved", Long.MAX_VALUE, maxBoundaryRound.sessionId)
        assertEquals("Maximum template ID length should be preserved", 1000, maxBoundaryRound.templateId.length)
        assertEquals("Maximum filled text length should be preserved", 1000, maxBoundaryRound.filledText.length)
        assertEquals("Maximum points should be preserved", Int.MAX_VALUE, maxBoundaryRound.points)
        assertEquals("Maximum timestamp should be preserved", Long.MAX_VALUE, maxBoundaryRound.timestamp)
    }

    @Test
    fun `RoundEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundaryRound = RoundEntity(
            id = Long.MIN_VALUE,
            sessionId = Long.MIN_VALUE,
            templateId = "",
            game = "",
            filledText = "",
            feedbackJson = "{}",
            points = Int.MIN_VALUE,
            timestamp = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Minimum boundary round should be created", minBoundaryRound)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundaryRound.id)
        assertEquals("Minimum session ID should be preserved", Long.MIN_VALUE, minBoundaryRound.sessionId)
        assertEquals("Minimum points should be preserved", Int.MIN_VALUE, minBoundaryRound.points)
        assertEquals("Minimum timestamp should be preserved", Long.MIN_VALUE, minBoundaryRound.timestamp)
    }
}