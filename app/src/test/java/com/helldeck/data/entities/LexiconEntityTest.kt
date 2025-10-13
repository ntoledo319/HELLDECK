package com.helldeck.data.entities

import com.helldeck.data.LexiconEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LexiconEntity validation and behavior
 */
class LexiconEntityTest {

    @Test
    fun `LexiconEntity with valid data creates successfully`() {
        // Arrange & Act
        val lexicon = LexiconEntity(
            id = 1L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\",\"word3\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Lexicon should be created", lexicon)
        assertEquals("ID should match", 1L, lexicon.id)
        assertEquals("Name should match", "test_lexicon", lexicon.name)
        assertEquals("Words should match", "[\"word1\",\"word2\",\"word3\"]", lexicon.words)
        assertEquals("UpdatedTs should match", 1000L, lexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val lexicon = LexiconEntity(
            id = 0L,
            name = "",
            words = "[]",
            updatedTs = 0L
        )

        // Assert
        assertNotNull("Minimal lexicon should be created", lexicon)
        assertEquals("Minimal ID should match", 0L, lexicon.id)
        assertEquals("Minimal name should match", "", lexicon.name)
        assertEquals("Minimal words should match", "[]", lexicon.words)
        assertEquals("Minimal updatedTs should match", 0L, lexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val lexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(100),
            words = "[\"${"A".repeat(1000)}\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximal lexicon should be created", lexicon)
        assertEquals("Maximal ID should match", Long.MAX_VALUE, lexicon.id)
        assertEquals("Maximal name should match", "A".repeat(100), lexicon.name)
        assertEquals("Maximal words should match", "[\"${"A".repeat(1000)}\"]", lexicon.words)
        assertEquals("Maximal updatedTs should match", Long.MAX_VALUE, lexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalLexicon = LexiconEntity(
            id = 1L,
            name = "original_name",
            words = "[\"original\"]",
            updatedTs = 1000L
        )

        // Act
        val copiedLexicon = originalLexicon.copy(
            name = "copied_name",
            words = "[\"copied\"]",
            updatedTs = 2000L
        )

        // Assert
        assertNotNull("Copied lexicon should not be null", copiedLexicon)
        assertEquals("ID should remain same", originalLexicon.id, copiedLexicon.id)
        assertEquals("Name should be updated", "copied_name", copiedLexicon.name)
        assertEquals("Words should be updated", "[\"copied\"]", copiedLexicon.words)
        assertEquals("UpdatedTs should be updated", 2000L, copiedLexicon.updatedTs)

        assertNotEquals("Original and copy should not be same object", originalLexicon, copiedLexicon)
    }

    @Test
    fun `LexiconEntity handles special characters correctly`() {
        // Arrange & Act
        val specialLexicon = LexiconEntity(
            id = 1L,
            name = "spécial_léxicon_ñ",
            words = "[\"wörd1\",\"wörd2\",\"émojis 🚀\"]"
        )

        // Assert
        assertNotNull("Special lexicon should be created", specialLexicon)
        assertEquals("Special name should be preserved", "spécial_léxicon_ñ", specialLexicon.name)
        assertEquals("Special words should be preserved", "[\"wörd1\",\"wörd2\",\"émojis 🚀\"]", specialLexicon.words)
    }

    @Test
    fun `LexiconEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodeLexicon = LexiconEntity(
            id = 1L,
            name = "ünícódé_léxicon",
            words = "[\"ünícódé_wörd1\",\"ünícódé_wörd2\",\"ünícódé_émojis 🚀\"]"
        )

        // Assert
        assertNotNull("Unicode lexicon should be created", unicodeLexicon)
        assertEquals("Unicode name should be preserved", "ünícódé_léxicon", unicodeLexicon.name)
        assertEquals("Unicode words should be preserved", "[\"ünícódé_wörd1\",\"ünícódé_wörd2\",\"ünícódé_émojis 🚀\"]", unicodeLexicon.words)
    }

    @Test
    fun `LexiconEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptyLexicon = LexiconEntity(
            id = 1L,
            name = "",
            words = "[]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Empty lexicon should be created", emptyLexicon)
        assertEquals("Empty name should be preserved", "", emptyLexicon.name)
    }

    @Test
    fun `LexiconEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespaceLexicon = LexiconEntity(
            id = 1L,
            name = "   ",
            words = "[]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Whitespace lexicon should be created", whitespaceLexicon)
        assertEquals("Whitespace name should be preserved", "   ", whitespaceLexicon.name)
    }

    @Test
    fun `LexiconEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundaryLexicon = LexiconEntity(
            id = 0L,
            name = "boundary_lexicon",
            words = "[]",
            updatedTs = 0L
        )

        // Assert
        assertNotNull("Boundary lexicon should be created", boundaryLexicon)
        assertEquals("Zero ID should be preserved", 0L, boundaryLexicon.id)
        assertEquals("Zero updatedTs should be preserved", 0L, boundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles very long name correctly`() {
        // Arrange & Act
        val longName = "A".repeat(1000) + " Very Long Lexicon Name " + "B".repeat(1000)
        val longNameLexicon = LexiconEntity(
            id = 1L,
            name = longName,
            words = "[\"test\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Long name lexicon should be created", longNameLexicon)
        assertEquals("Long name should be preserved", longName, longNameLexicon.name)
        assertEquals("Long name length should match", 2027, longNameLexicon.name.length)
    }

    @Test
    fun `LexiconEntity handles very long words correctly`() {
        // Arrange & Act
        val longWords = List(1000) { i -> "\"word$i\"" }.joinToString(",", prefix = "[", postfix = "]")
        val longWordsLexicon = LexiconEntity(
            id = 1L,
            name = "long_words_lexicon",
            words = longWords,
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Long words lexicon should be created", longWordsLexicon)
        assertEquals("Long words should be preserved", longWords, longWordsLexicon.words)
        assertEquals("Long words length should match", 30003, longWordsLexicon.words.length)
    }

    @Test
    fun `LexiconEntity handles emojis in words correctly`() {
        // Arrange & Act
        val emojiWordsLexicon = LexiconEntity(
            id = 1L,
            name = "emoji_words_lexicon",
            words = "[\"🚀\",\"🌟\",\"🎯\",\"🎲\",\"word\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Emoji words lexicon should be created", emojiWordsLexicon)
        assertTrue("Words should contain rocket emoji", emojiWordsLexicon.words.contains("🚀"))
        assertTrue("Words should contain star emoji", emojiWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain dart emoji", emojiWordsLexicon.words.contains("🎯"))
        assertTrue("Words should contain dice emoji", emojiWordsLexicon.words.contains("🎲"))
        assertTrue("Words should contain normal word", emojiWordsLexicon.words.contains("word"))
    }

    @Test
    fun `LexiconEntity handles numbers in words correctly`() {
        // Arrange & Act
        val numbersWordsLexicon = LexiconEntity(
            id = 1L,
            name = "numbers_words_lexicon",
            words = "[\"word1\",\"word2\",\"word123\",\"word456.789\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Numbers words lexicon should be created", numbersWordsLexicon)
        assertTrue("Words should contain word1", numbersWordsLexicon.words.contains("word1"))
        assertTrue("Words should contain word2", numbersWordsLexicon.words.contains("word2"))
        assertTrue("Words should contain word123", numbersWordsLexicon.words.contains("word123"))
        assertTrue("Words should contain word456.789", numbersWordsLexicon.words.contains("word456.789"))
    }

    @Test
    fun `LexiconEntity handles special regex characters in words correctly`() {
        // Arrange & Act
        val regexCharsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "regex_chars_words_lexicon",
            words = "[\"word^\",\"word$\",\"word*\",\"word+\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Regex chars words lexicon should be created", regexCharsWordsLexicon)
        assertTrue("Words should contain regex characters", regexCharsWordsLexicon.words.contains("word^"))
        assertTrue("Words should contain more regex characters", regexCharsWordsLexicon.words.contains("word$"))
        assertTrue("Words should contain more regex characters", regexCharsWordsLexicon.words.contains("word*"))
        assertTrue("Words should contain more regex characters", regexCharsWordsLexicon.words.contains("word+"))
    }

    @Test
    fun `LexiconEntity handles quotes in words correctly`() {
        // Arrange & Act
        val quotesWordsLexicon = LexiconEntity(
            id = 1L,
            name = "quotes_words_lexicon",
            words = "[\"\\\"quoted word\\\"\"]\",\"'single quoted word'\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Quotes words lexicon should be created", quotesWordsLexicon)
        assertTrue("Words should contain double quotes", quotesWordsLexicon.words.contains("\\\"quoted word\\\""))
        assertTrue("Words should contain single quotes", quotesWordsLexicon.words.contains("'single quoted word'"))
    }

    @Test
    fun `LexiconEntity handles newlines in words correctly`() {
        // Arrange & Act
        val newlinesWordsLexicon = LexiconEntity(
            id = 1L,
            name = "newlines_words_lexicon",
            words = "[\"word\\nwith\\nnewlines\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Newlines words lexicon should be created", newlinesWordsLexicon)
        assertTrue("Words should contain newlines", newlinesWordsLexicon.words.contains("\\n"))
    }

    @Test
    fun `LexiconEntity handles tabs in words correctly`() {
        // Arrange & Act
        val tabsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "tabs_words_lexicon",
            words = "[\"word\\twith\\ttabs\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Tabs words lexicon should be created", tabsWordsLexicon)
        assertTrue("Words should contain tabs", tabsWordsLexicon.words.contains("\\t"))
    }

    @Test
    fun `LexiconEntity handles HTML-like content in words correctly`() {
        // Arrange & Act
        val htmlLikeWordsLexicon = LexiconEntity(
            id = 1L,
            name = "html_like_words_lexicon",
            words = "[\"<html>word</html>\",\"&entity;\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("HTML-like words lexicon should be created", htmlLikeWordsLexicon)
        assertTrue("Words should contain HTML tag", htmlLikeWordsLexicon.words.contains("<html>word</html>"))
        assertTrue("Words should contain HTML entity", htmlLikeWordsLexicon.words.contains("&entity;"))
    }

    @Test
    fun `LexiconEntity handles URLs in words correctly`() {
        // Arrange & Act
        val urlWordsLexicon = LexiconEntity(
            id = 1L,
            name = "url_words_lexicon",
            words = "[\"https://example.com\",\"ftp://test.org\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("URL words lexicon should be created", urlWordsLexicon)
        assertTrue("Words should contain HTTPS URL", urlWordsLexicon.words.contains("https://example.com"))
        assertTrue("Words should contain FTP URL", urlWordsLexicon.words.contains("ftp://test.org"))
    }

    @Test
    fun `LexiconEntity handles email addresses in words correctly`() {
        // Arrange & Act
        val emailWordsLexicon = LexiconEntity(
            id = 1L,
            name = "email_words_lexicon",
            words = "[\"user@example.com\",\"test.email+tag@domain.co.uk\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Email words lexicon should be created", emailWordsLexicon)
        assertTrue("Words should contain email", emailWordsLexicon.words.contains("user@example.com"))
        assertTrue("Words should contain complex email", emailWordsLexicon.words.contains("test.email+tag@domain.co.uk"))
    }

    @Test
    fun `LexiconEntity handles file paths in words correctly`() {
        // Arrange & Act
        val filePathWordsLexicon = LexiconEntity(
            id = 1L,
            name = "filepath_words_lexicon",
            words = "[\"/usr/local/bin\",\"C:\\\\Windows\\\\System32\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("File path words lexicon should be created", filePathWordsLexicon)
        assertTrue("Words should contain Unix path", filePathWordsLexicon.words.contains("/usr/local/bin"))
        assertTrue("Words should contain Windows path", filePathWordsLexicon.words.contains("C:\\\\Windows\\\\System32"))
    }

    @Test
    fun `LexiconEntity handles JSON-like content in words correctly`() {
        // Arrange & Act
        val jsonLikeWordsLexicon = LexiconEntity(
            id = 1L,
            name = "json_like_words_lexicon",
            words = "[\"{\\\\"key\\\": \\\\"value\\\"}\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("JSON-like words lexicon should be created", jsonLikeWordsLexicon)
        assertTrue("Words should contain JSON object", jsonLikeWordsLexicon.words.contains("{\\"key\\\": \\"value\\"}"))
    }

    @Test
    fun `LexiconEntity handles SQL-like content in words correctly`() {
        // Arrange & Act
        val sqlLikeWordsLexicon = LexiconEntity(
            id = 1L,
            name = "sql_like_words_lexicon",
            words = "[\"SELECT * FROM table\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("SQL-like words lexicon should be created", sqlLikeWordsLexicon)
        assertTrue("Words should contain SQL query", sqlLikeWordsLexicon.words.contains("SELECT * FROM table"))
    }

    @Test
    fun `LexiconEntity handles programming code in words correctly`() {
        // Arrange & Act
        val codeLikeWordsLexicon = LexiconEntity(
            id = 1L,
            name = "code_like_words_lexicon",
            words = "[\"if (condition)\",\"{ doSomething(); }\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Code-like words lexicon should be created", codeLikeWordsLexicon)
        assertTrue("Words should contain if statement", codeLikeWordsLexicon.words.contains("if (condition)"))
        assertTrue("Words should contain braces", codeLikeWordsLexicon.words.contains("{ doSomething(); }"))
    }

    @Test
    fun `LexiconEntity handles mathematical symbols in words correctly`() {
        // Arrange & Act
        val mathSymbolsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "math_symbols_words_lexicon",
            words = "[\"∑\",\"∆\",\"π\",\"∞\",\"√\",\"∫\",\"∂\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Math symbols words lexicon should be created", mathSymbolsWordsLexicon)
        assertTrue("Words should contain sum symbol", mathSymbolsWordsLexicon.words.contains("∑"))
        assertTrue("Words should contain delta symbol", mathSymbolsWordsLexicon.words.contains("∆"))
        assertTrue("Words should contain pi symbol", mathSymbolsWordsLexicon.words.contains("π"))
        assertTrue("Words should contain infinity symbol", mathSymbolsWordsLexicon.words.contains("∞"))
        assertTrue("Words should contain square root symbol", mathSymbolsWordsLexicon.words.contains("√"))
        assertTrue("Words should contain integral symbol", mathSymbolsWordsLexicon.words.contains("∫"))
        assertTrue("Words should contain partial symbol", mathSymbolsWordsLexicon.words.contains("∂"))
    }

    @Test
    fun `LexiconEntity handles binary-like content in words correctly`() {
        // Arrange & Act
        val binaryLikeWordsLexicon = LexiconEntity(
            id = 1L,
            name = "binary_like_words_lexicon",
            words = "[\"01010101\",\"11001100\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Binary-like words lexicon should be created", binaryLikeWordsLexicon)
        assertTrue("Words should contain binary-like content", binaryLikeWordsLexicon.words.contains("01010101"))
        assertTrue("Words should contain more binary-like content", binaryLikeWordsLexicon.words.contains("11001100"))
    }

    @Test
    fun `LexiconEntity handles right-to-left text in words correctly`() {
        // Arrange & Act
        val rtlWordsLexicon = LexiconEntity(
            id = 1L,
            name = "rtl_words_lexicon",
            words = "[\"العربية\",\"עברית\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("RTL words lexicon should be created", rtlWordsLexicon)
        assertTrue("Words should contain Arabic text", rtlWordsLexicon.words.contains("العربية"))
        assertTrue("Words should contain Hebrew text", rtlWordsLexicon.words.contains("עברית"))
    }

    @Test
    fun `LexiconEntity handles combining characters in words correctly`() {
        // Arrange & Act
        val combiningCharsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "combining_chars_words_lexicon",
            words = "[\"á\",\"é\",\"í\",\"ó\",\"ú\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Combining chars words lexicon should be created", combiningCharsWordsLexicon)
        assertTrue("Words should contain combining characters", combiningCharsWordsLexicon.words.contains("á"))
        assertTrue("Words should contain more combining characters", combiningCharsWordsLexicon.words.contains("é"))
    }

    @Test
    fun `LexiconEntity handles zero-width characters in words correctly`() {
        // Arrange & Act
        val zeroWidthWordsLexicon = LexiconEntity(
            id = 1L,
            name = "zero_width_words_lexicon",
            words = "[\"\u200B\",\"\u200C\",\"\u200D\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Zero width words lexicon should be created", zeroWidthWordsLexicon)
        assertTrue("Words should contain zero-width characters", zeroWidthWordsLexicon.words.contains("\u200B"))
    }

    @Test
    fun `LexiconEntity handles invisible characters in words correctly`() {
        // Arrange & Act
        val invisibleCharsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "invisible_chars_words_lexicon",
            words = "[\"\u0000\",\"\u0001\",\"\u0002\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Invisible chars words lexicon should be created", invisibleCharsWordsLexicon)
        assertTrue("Words should contain invisible characters", invisibleCharsWordsLexicon.words.contains("\u0000"))
    }

    @Test
    fun `LexiconEntity handles mixed content in words correctly`() {
        // Arrange & Act
        val mixedContentWordsLexicon = LexiconEntity(
            id = 1L,
            name = "mixed_content_words_lexicon",
            words = "[\"núméros 123\",\"émojis 🚀🌟\",\"çharácters\",\"\\\"quotes\\\"\",\"móre!\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Mixed content words lexicon should be created", mixedContentWordsLexicon)
        assertTrue("Words should contain numbers", mixedContentWordsLexicon.words.contains("núméros 123"))
        assertTrue("Words should contain emojis", mixedContentWordsLexicon.words.contains("🚀") && mixedContentWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain special characters", mixedContentWordsLexicon.words.contains("çharácters"))
        assertTrue("Words should contain quotes", mixedContentWordsLexicon.words.contains("\\\"quotes\\\""))
        assertTrue("Words should contain more accents", mixedContentWordsLexicon.words.contains("móre!"))
    }

    @Test
    fun `LexiconEntity handles extremely long words correctly`() {
        // Arrange & Act
        val extremelyLongWords = "[\"${"A".repeat(10000)} Extremely Long Word ${"B".repeat(10000)}\"]"
        val extremelyLongWordsLexicon = LexiconEntity(
            id = 1L,
            name = "extremely_long_words_lexicon",
            words = extremelyLongWords,
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Extremely long words lexicon should be created", extremelyLongWordsLexicon)
        assertEquals("Extremely long words should be preserved", extremelyLongWords, extremelyLongWordsLexicon.words)
        assertEquals("Extremely long words length should match", 40007, extremelyLongWordsLexicon.words.length)
    }

    @Test
    fun `LexiconEntity handles extremely long name correctly`() {
        // Arrange & Act
        val extremelyLongName = "A".repeat(10000) + " Extremely Long Lexicon Name " + "B".repeat(10000)
        val extremelyLongNameLexicon = LexiconEntity(
            id = 1L,
            name = extremelyLongName,
            words = "[\"test\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Extremely long name lexicon should be created", extremelyLongNameLexicon)
        assertEquals("Extremely long name should be preserved", extremelyLongName, extremelyLongNameLexicon.name)
        assertEquals("Extremely long name length should match", 40027, extremelyLongNameLexicon.name.length)
    }

    @Test
    fun `LexiconEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(1000),
            words = "[\"Edge case: ünícódé 🚀🌟, ñúméros 123.456, \\\"quotes\\\", {{{nested}}}, ∑∆π∞, and móre!\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Edge case lexicon should be created", edgeCaseLexicon)
        assertEquals("ID should be maximum", Long.MAX_VALUE, edgeCaseLexicon.id)
        assertEquals("Name should be extremely long", 1000, edgeCaseLexicon.name.length)
        assertEquals("Words should be extremely long", 124, edgeCaseLexicon.words.length)
        assertEquals("UpdatedTs should be maximum", Long.MAX_VALUE, edgeCaseLexicon.updatedTs)

        assertTrue("Words should contain unicode", edgeCaseLexicon.words.contains("ünícódé"))
        assertTrue("Words should contain emojis", edgeCaseLexicon.words.contains("🚀") && edgeCaseLexicon.words.contains("🌟"))
        assertTrue("Words should contain numbers", edgeCaseLexicon.words.contains("123.456"))
        assertTrue("Words should contain quotes", edgeCaseLexicon.words.contains("\\\"quotes\\\""))
        assertTrue("Words should contain nested braces", edgeCaseLexicon.words.contains("{{{nested}}}"))
        assertTrue("Words should contain math symbols", edgeCaseLexicon.words.contains("∑∆π∞"))
        assertTrue("Words should contain more accents", edgeCaseLexicon.words.contains("móre!"))
    }

    @Test
    fun `LexiconEntity equality works correctly`() {
        // Arrange
        val lexicon1 = LexiconEntity(
            id = 1L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\"]",
            updatedTs = 1000L
        )

        val lexicon2 = LexiconEntity(
            id = 1L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\"]",
            updatedTs = 1000L
        )

        val lexicon3 = LexiconEntity(
            id = 2L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\"]",
            updatedTs = 1000L
        )

        // Assert
        assertEquals("Identical lexicons should be equal", lexicon1, lexicon2)
        assertNotEquals("Different lexicons should not be equal", lexicon1, lexicon3)
        assertEquals("Hash codes should be equal for identical lexicons", lexicon1.hashCode(), lexicon2.hashCode())
        assertTrue("Hash codes should be different for different lexicons", lexicon1.hashCode() != lexicon3.hashCode())
    }

    @Test
    fun `LexiconEntity toString contains relevant information`() {
        // Arrange
        val lexicon = LexiconEntity(
            id = 1L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\"]",
            updatedTs = 1000L
        )

        // Act
        val toString = lexicon.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("id=1"))
        assertTrue("toString should contain name", toString.contains("test_lexicon"))
        assertTrue("toString should contain words", toString.contains("[\"word1\",\"word2\"]"))
        assertTrue("toString should contain updatedTs", toString.contains("updatedTs=1000"))
    }

    @Test
    fun `LexiconEntity handles single word correctly`() {
        // Arrange & Act
        val singleWordLexicon = LexiconEntity(
            id = 1L,
            name = "single_word_lexicon",
            words = "[\"single\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Single word lexicon should be created", singleWordLexicon)
        assertEquals("Single word should be preserved", "[\"single\"]", singleWordLexicon.words)
    }

    @Test
    fun `LexiconEntity handles many words correctly`() {
        // Arrange & Act
        val manyWords = (1..1000).map { "\"word$it\"" }.joinToString(",", prefix = "[", postfix = "]")
        val manyWordsLexicon = LexiconEntity(
            id = 1L,
            name = "many_words_lexicon",
            words = manyWords,
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Many words lexicon should be created", manyWordsLexicon)
        assertEquals("Many words should be preserved", manyWords, manyWordsLexicon.words)
        assertEquals("Word count should be 1000", 1000, manyWordsLexicon.words.split(",").size - 1)
    }

    @Test
    fun `LexiconEntity handles words with spaces correctly`() {
        // Arrange & Act
        val spaceWordsLexicon = LexiconEntity(
            id = 1L,
            name = "space_words_lexicon",
            words = "[\"word with spaces\",\"another word\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Space words lexicon should be created", spaceWordsLexicon)
        assertTrue("Words should contain spaces", spaceWordsLexicon.words.contains("word with spaces"))
        assertTrue("Words should contain another word", spaceWordsLexicon.words.contains("another word"))
    }

    @Test
    fun `LexiconEntity handles words with special characters correctly`() {
        // Arrange & Act
        val specialWordsLexicon = LexiconEntity(
            id = 1L,
            name = "special_words_lexicon",
            words = "[\"spécial\",\"émojis\",\"tëst\",\"ünícódé\",\"ñormal\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Special words lexicon should be created", specialWordsLexicon)
        assertTrue("Words should contain spécial", specialWordsLexicon.words.contains("spécial"))
        assertTrue("Words should contain émojis", specialWordsLexicon.words.contains("émojis"))
        assertTrue("Words should contain tëst", specialWordsLexicon.words.contains("tëst"))
        assertTrue("Words should contain ünícódé", specialWordsLexicon.words.contains("ünícódé"))
        assertTrue("Words should contain ñormal", specialWordsLexicon.words.contains("ñormal"))
    }

    @Test
    fun `LexiconEntity handles words with emojis correctly`() {
        // Arrange & Act
        val emojiWordsLexicon = LexiconEntity(
            id = 1L,
            name = "emoji_words_lexicon",
            words = "[\"🚀\",\"🌟\",\"🎯\",\"🎲\",\"word\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Emoji words lexicon should be created", emojiWordsLexicon)
        assertTrue("Words should contain rocket emoji", emojiWordsLexicon.words.contains("🚀"))
        assertTrue("Words should contain star emoji", emojiWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain dart emoji", emojiWordsLexicon.words.contains("🎯"))
        assertTrue("Words should contain dice emoji", emojiWordsLexicon.words.contains("🎲"))
        assertTrue("Words should contain normal word", emojiWordsLexicon.words.contains("word"))
    }

    @Test
    fun `LexiconEntity handles words with numbers correctly`() {
        // Arrange & Act
        val numberWordsLexicon = LexiconEntity(
            id = 1L,
            name = "number_words_lexicon",
            words = "[\"word1\",\"word2\",\"word123\",\"word456.789\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Number words lexicon should be created", numberWordsLexicon)
        assertTrue("Words should contain word1", numberWordsLexicon.words.contains("word1"))
        assertTrue("Words should contain word2", numberWordsLexicon.words.contains("word2"))
        assertTrue("Words should contain word123", numberWordsLexicon.words.contains("word123"))
        assertTrue("Words should contain word456.789", numberWordsLexicon.words.contains("word456.789"))
    }

    @Test
    fun `LexiconEntity handles words with commas correctly`() {
        // Arrange & Act
        val commaWordsLexicon = LexiconEntity(
            id = 1L,
            name = "comma_words_lexicon",
            words = "[\"word,with,commas\",\"another,word\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Comma words lexicon should be created", commaWordsLexicon)
        assertTrue("Words should contain commas", commaWordsLexicon.words.contains("word,with,commas"))
        assertTrue("Words should contain another word", commaWordsLexicon.words.contains("another,word"))
    }

    @Test
    fun `LexiconEntity handles words with quotes correctly`() {
        // Arrange & Act
        val quoteWordsLexicon = LexiconEntity(
            id = 1L,
            name = "quote_words_lexicon",
            words = "[\"\\\"quoted word\\\"\"]\",\"'single quoted word'\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Quote words lexicon should be created", quoteWordsLexicon)
        assertTrue("Words should contain quoted word", quoteWordsLexicon.words.contains("\\\"quoted word\\\""))
        assertTrue("Words should contain single quoted word", quoteWordsLexicon.words.contains("'single quoted word'"))
    }

    @Test
    fun `LexiconEntity handles words with nested content correctly`() {
        // Arrange & Act
        val nestedWordsLexicon = LexiconEntity(
            id = 1L,
            name = "nested_words_lexicon",
            words = "[\"word{nested}\",\"word[nested]\",\"word(nested)\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Nested words lexicon should be created", nestedWordsLexicon)
        assertTrue("Words should contain nested braces", nestedWordsLexicon.words.contains("word{nested}"))
        assertTrue("Words should contain nested brackets", nestedWordsLexicon.words.contains("word[nested]"))
        assertTrue("Words should contain nested parentheses", nestedWordsLexicon.words.contains("word(nested)"))
    }

    @Test
    fun `LexiconEntity handles words with mathematical symbols correctly`() {
        // Arrange & Act
        val mathWordsLexicon = LexiconEntity(
            id = 1L,
            name = "math_words_lexicon",
            words = "[\"∑\",\"∆\",\"π\",\"∞\",\"√\",\"∫\",\"∂\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Math words lexicon should be created", mathWordsLexicon)
        assertTrue("Words should contain sum symbol", mathWordsLexicon.words.contains("∑"))
        assertTrue("Words should contain delta symbol", mathWordsLexicon.words.contains("∆"))
        assertTrue("Words should contain pi symbol", mathWordsLexicon.words.contains("π"))
        assertTrue("Words should contain infinity symbol", mathWordsLexicon.words.contains("∞"))
        assertTrue("Words should contain square root symbol", mathWordsLexicon.words.contains("√"))
        assertTrue("Words should contain integral symbol", mathWordsLexicon.words.contains("∫"))
        assertTrue("Words should contain partial symbol", mathWordsLexicon.words.contains("∂"))
    }

    @Test
    fun `LexiconEntity handles words with chess symbols correctly`() {
        // Arrange & Act
        val chessWordsLexicon = LexiconEntity(
            id = 1L,
            name = "chess_words_lexicon",
            words = "[\"♔\",\"♕\",\"♖\",\"♗\",\"♘\",\"♙\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Chess words lexicon should be created", chessWordsLexicon)
        assertTrue("Words should contain white king", chessWordsLexicon.words.contains("♔"))
        assertTrue("Words should contain white queen", chessWordsLexicon.words.contains("♕"))
        assertTrue("Words should contain white rook", chessWordsLexicon.words.contains("♖"))
        assertTrue("Words should contain white bishop", chessWordsLexicon.words.contains("♗"))
        assertTrue("Words should contain white knight", chessWordsLexicon.words.contains("♘"))
        assertTrue("Words should contain white pawn", chessWordsLexicon.words.contains("♙"))
    }

    @Test
    fun `LexiconEntity handles words with card symbols correctly`() {
        // Arrange & Act
        val cardWordsLexicon = LexiconEntity(
            id = 1L,
            name = "card_words_lexicon",
            words = "[\"♠\",\"♥\",\"♦\",\"♣\",\"🃏\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Card words lexicon should be created", cardWordsLexicon)
        assertTrue("Words should contain spade", cardWordsLexicon.words.contains("♠"))
        assertTrue("Words should contain heart", cardWordsLexicon.words.contains("♥"))
        assertTrue("Words should contain diamond", cardWordsLexicon.words.contains("♦"))
        assertTrue("Words should contain club", cardWordsLexicon.words.contains("♣"))
        assertTrue("Words should contain joker", cardWordsLexicon.words.contains("🃏"))
    }

    @Test
    fun `LexiconEntity handles words with dice and game symbols correctly`() {
        // Arrange & Act
        val gameWordsLexicon = LexiconEntity(
            id = 1L,
            name = "game_words_lexicon",
            words = "[\"🎲\",\"🎯\",\"🎮\",\"🎰\",\"🎳\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Game words lexicon should be created", gameWordsLexicon)
        assertTrue("Words should contain dice", gameWordsLexicon.words.contains("🎲"))
        assertTrue("Words should contain dart", gameWordsLexicon.words.contains("🎯"))
        assertTrue("Words should contain video game", gameWordsLexicon.words.contains("🎮"))
        assertTrue("Words should contain slot machine", gameWordsLexicon.words.contains("🎰"))
        assertTrue("Words should contain bowling", gameWordsLexicon.words.contains("🎳"))
    }

    @Test
    fun `LexiconEntity handles words with weather symbols correctly`() {
        // Arrange & Act
        val weatherWordsLexicon = LexiconEntity(
            id = 1L,
            name = "weather_words_lexicon",
            words = "[\"☀️\",\"🌤️\",\"⛅\",\"🌦️\",\"🌧️\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Weather words lexicon should be created", weatherWordsLexicon)
        assertTrue("Words should contain sun", weatherWordsLexicon.words.contains("☀️"))
        assertTrue("Words should contain partly cloudy", weatherWordsLexicon.words.contains("🌤️"))
        assertTrue("Words should contain cloudy", weatherWordsLexicon.words.contains("⛅"))
        assertTrue("Words should contain rainy", weatherWordsLexicon.words.contains("🌦️"))
        assertTrue("Words should contain rain", weatherWordsLexicon.words.contains("🌧️"))
    }

    @Test
    fun `LexiconEntity handles words with food and drink symbols correctly`() {
        // Arrange & Act
        val foodWordsLexicon = LexiconEntity(
            id = 1L,
            name = "food_words_lexicon",
            words = "[\"🍕\",\"🍔\",\"🍟\",\"🌭\",\"🍿\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Food words lexicon should be created", foodWordsLexicon)
        assertTrue("Words should contain pizza", foodWordsLexicon.words.contains("🍕"))
        assertTrue("Words should contain burger", foodWordsLexicon.words.contains("🍔"))
        assertTrue("Words should contain fries", foodWordsLexicon.words.contains("🍟"))
        assertTrue("Words should contain hot dog", foodWordsLexicon.words.contains("🌭"))
        assertTrue("Words should contain popcorn", foodWordsLexicon.words.contains("🍿"))
    }

    @Test
    fun `LexiconEntity handles words with activity symbols correctly`() {
        // Arrange & Act
        val activityWordsLexicon = LexiconEntity(
            id = 1L,
            name = "activity_words_lexicon",
            words = "[\"⚽\",\"🏀\",\"🏈\",\"⚾\",\"🎾\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Activity words lexicon should be created", activityWordsLexicon)
        assertTrue("Words should contain soccer ball", activityWordsLexicon.words.contains("⚽"))
        assertTrue("Words should contain basketball", activityWordsLexicon.words.contains("🏀"))
        assertTrue("Words should contain football", activityWordsLexicon.words.contains("🏈"))
        assertTrue("Words should contain baseball", activityWordsLexicon.words.contains("⚾"))
        assertTrue("Words should contain tennis ball", activityWordsLexicon.words.contains("🎾"))
    }

    @Test
    fun `LexiconEntity handles words with travel and place symbols correctly`() {
        // Arrange & Act
        val travelWordsLexicon = LexiconEntity(
            id = 1L,
            name = "travel_words_lexicon",
            words = "[\"🚗\",\"✈️\",\"🚲\",\"⛵\",\"🏠\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Travel words lexicon should be created", travelWordsLexicon)
        assertTrue("Words should contain car", travelWordsLexicon.words.contains("🚗"))
        assertTrue("Words should contain airplane", travelWordsLexicon.words.contains("✈️"))
        assertTrue("Words should contain bicycle", travelWordsLexicon.words.contains("🚲"))
        assertTrue("Words should contain boat", travelWordsLexicon.words.contains("⛵"))
        assertTrue("Words should contain house", travelWordsLexicon.words.contains("🏠"))
    }

    @Test
    fun `LexiconEntity handles words with symbol combinations correctly`() {
        // Arrange & Act
        val symbolComboWordsLexicon = LexiconEntity(
            id = 1L,
            name = "symbol_combo_words_lexicon",
            words = "[\"👨‍💻\",\"👩‍🎨\",\"🚀\",\"🌟\",\"⚽\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Symbol combo words lexicon should be created", symbolComboWordsLexicon)
        assertTrue("Words should contain programmer", symbolComboWordsLexicon.words.contains("👨‍💻"))
        assertTrue("Words should contain artist", symbolComboWordsLexicon.words.contains("👩‍🎨"))
        assertTrue("Words should contain rocket", symbolComboWordsLexicon.words.contains("🚀"))
        assertTrue("Words should contain star", symbolComboWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain soccer", symbolComboWordsLexicon.words.contains("⚽"))
    }

    @Test
    fun `LexiconEntity handles words with extremely complex content correctly`() {
        // Arrange & Act
        val extremelyComplexWords = "[\"Extremely complex: ünícódé 🚀🌟, ñúméros 123.456, \\\"quotes\\\", {{{nested}}}, ∑∆π∞, ♔♕♖♗♘♙, and móre!\"]"
        val extremelyComplexLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(1000),
            words = extremelyComplexWords,
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Extremely complex lexicon should be created", extremelyComplexLexicon)
        assertEquals("ID should be maximum", Long.MAX_VALUE, extremelyComplexLexicon.id)
        assertEquals("Name should be extremely long", 1000, extremelyComplexLexicon.name.length)
        assertEquals("Words should be extremely long", 142, extremelyComplexLexicon.words.length)
        assertEquals("UpdatedTs should be maximum", Long.MAX_VALUE, extremelyComplexLexicon.updatedTs)

        assertTrue("Words should contain unicode", extremelyComplexLexicon.words.contains("ünícódé"))
        assertTrue("Words should contain emojis", extremelyComplexLexicon.words.contains("🚀") && extremelyComplexLexicon.words.contains("🌟"))
        assertTrue("Words should contain numbers", extremelyComplexLexicon.words.contains("123.456"))
        assertTrue("Words should contain quotes", extremelyComplexLexicon.words.contains("\\\"quotes\\\""))
        assertTrue("Words should contain nested braces", extremelyComplexLexicon.words.contains("{{{nested}}}"))
        assertTrue("Words should contain math symbols", extremelyComplexLexicon.words.contains("∑∆π∞"))
        assertTrue("Words should contain chess symbols", extremelyComplexLexicon.words.contains("♔♕♖♗♘♙"))
        assertTrue("Words should contain more accents", extremelyComplexLexicon.words.contains("móre!"))
    }

    @Test
    fun `LexiconEntity handles words with zero-width joiner emojis correctly`() {
        // Arrange & Act
        val zwjEmojiWordsLexicon = LexiconEntity(
            id = 1L,
            name = "zwj_emoji_words_lexicon",
            words = "[\"👨‍💻\",\"👩‍🎨\",\"👨‍🚀\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("ZWJ emoji words lexicon should be created", zwjEmojiWordsLexicon)
        assertTrue("Words should contain programmer", zwjEmojiWordsLexicon.words.contains("👨‍💻"))
        assertTrue("Words should contain artist", zwjEmojiWordsLexicon.words.contains("👩‍🎨"))
        assertTrue("Words should contain astronaut", zwjEmojiWordsLexicon.words.contains("👨‍🚀"))
    }

    @Test
    fun `LexiconEntity handles words with skin tone modifiers correctly`() {
        // Arrange & Act
        val skinToneWordsLexicon = LexiconEntity(
            id = 1L,
            name = "skin_tone_words_lexicon",
            words = "[\"👋🏻\",\"👋🏼\",\"👋🏽\",\"👋🏾\",\"👋🏿\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Skin tone words lexicon should be created", skinToneWordsLexicon)
        assertTrue("Words should contain light skin tone", skinToneWordsLexicon.words.contains("👋🏻"))
        assertTrue("Words should contain medium-light skin tone", skinToneWordsLexicon.words.contains("👋🏼"))
        assertTrue("Words should contain medium skin tone", skinToneWordsLexicon.words.contains("👋🏽"))
        assertTrue("Words should contain medium-dark skin tone", skinToneWordsLexicon.words.contains("👋🏾"))
        assertTrue("Words should contain dark skin tone", skinToneWordsLexicon.words.contains("👋🏿"))
    }

    @Test
    fun `LexiconEntity handles words with country flags correctly`() {
        // Arrange & Act
        val countryFlagsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "country_flags_words_lexicon",
            words = "[\"🇺🇸\",\"🇬🇧\",\"🇫🇷\",\"🇩🇪\",\"🇯🇵\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Country flags words lexicon should be created", countryFlagsWordsLexicon)
        assertTrue("Words should contain US flag", countryFlagsWordsLexicon.words.contains("🇺🇸"))
        assertTrue("Words should contain UK flag", countryFlagsWordsLexicon.words.contains("🇬🇧"))
        assertTrue("Words should contain France flag", countryFlagsWordsLexicon.words.contains("🇫🇷"))
        assertTrue("Words should contain Germany flag", countryFlagsWordsLexicon.words.contains("🇩🇪"))
        assertTrue("Words should contain Japan flag", countryFlagsWordsLexicon.words.contains("🇯🇵"))
    }

    @Test
    fun `LexiconEntity handles words with astronomical symbols correctly`() {
        // Arrange & Act
        val astronomicalWordsLexicon = LexiconEntity(
            id = 1L,
            name = "astronomical_words_lexicon",
            words = "[\"☀️\",\"🌙\",\"⭐\",\"🌟\",\"🌠\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Astronomical words lexicon should be created", astronomicalWordsLexicon)
        assertTrue("Words should contain sun", astronomicalWordsLexicon.words.contains("☀️"))
        assertTrue("Words should contain moon", astronomicalWordsLexicon.words.contains("🌙"))
        assertTrue("Words should contain star", astronomicalWordsLexicon.words.contains("⭐"))
        assertTrue("Words should contain glowing star", astronomicalWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain shooting star", astronomicalWordsLexicon.words.contains("🌠"))
    }

    @Test
    fun `LexiconEntity handles words with musical notation correctly`() {
        // Arrange & Act
        val musicalWordsLexicon = LexiconEntity(
            id = 1L,
            name = "musical_words_lexicon",
            words = "[\"♪\",\"♫\",\"♬\",\"♭\",\"♮\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Musical words lexicon should be created", musicalWordsLexicon)
        assertTrue("Words should contain eighth note", musicalWordsLexicon.words.contains("♪"))
        assertTrue("Words should contain beamed notes", musicalWordsLexicon.words.contains("♫"))
        assertTrue("Words should contain beamed sixteenth notes", musicalWordsLexicon.words.contains("♬"))
        assertTrue("Words should contain flat", musicalWordsLexicon.words.contains("♭"))
        assertTrue("Words should contain natural", musicalWordsLexicon.words.contains("♮"))
    }

    @Test
    fun `LexiconEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundaryLexicon = LexiconEntity(
            id = 0L,
            name = "",
            words = "[]",
            updatedTs = 0L
        )

        // Assert
        assertNotNull("Boundary lexicon should be created", boundaryLexicon)
        assertEquals("Zero ID should be preserved", 0L, boundaryLexicon.id)
        assertEquals("Empty name should be preserved", "", boundaryLexicon.name)
        assertEquals("Empty words should be preserved", "[]", boundaryLexicon.words)
        assertEquals("Zero updatedTs should be preserved", 0L, boundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundaryLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(1000),
            words = "[\"${"A".repeat(1000)}\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximum boundary lexicon should be created", maxBoundaryLexicon)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundaryLexicon.id)
        assertEquals("Maximum name length should be preserved", 1000, maxBoundaryLexicon.name.length)
        assertEquals("Maximum words length should be preserved", 1004, maxBoundaryLexicon.words.length)
        assertEquals("Maximum updatedTs should be preserved", Long.MAX_VALUE, maxBoundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundaryLexicon = LexiconEntity(
            id = Long.MIN_VALUE,
            name = "",
            words = "[]",
            updatedTs = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Minimum boundary lexicon should be created", minBoundaryLexicon)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundaryLexicon.id)
        assertEquals("Minimum updatedTs should be preserved", Long.MIN_VALUE, minBoundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles malformed JSON correctly`() {
        // Arrange & Act
        val malformedJsonLexicon = LexiconEntity(
            id = 1L,
            name = "malformed_json_lexicon",
            words = "[\"word1\",\"word2\",]", // Trailing comma
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Malformed JSON lexicon should be created", malformedJsonLexicon)
        assertEquals("Malformed JSON should be preserved", "[\"word1\",\"word2\",]", malformedJsonLexicon.words)
    }

    @Test
    fun `LexiconEntity handles nested JSON correctly`() {
        // Arrange & Act
        val nestedJsonLexicon = LexiconEntity(
            id = 1L,
            name = "nested_json_lexicon",
            words = "[\"{\\\\"key\\\": \\\\"value\\\"}\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Nested JSON lexicon should be created", nestedJsonLexicon)
        assertEquals("Nested JSON should be preserved", "[\"{\\\\\"key\\\": \\\\\"value\\\"}\"]", nestedJsonLexicon.words)
    }

    @Test
    fun `LexiconEntity handles future timestamp correctly`() {
        // Arrange & Act
        val futureTimestamp = System.currentTimeMillis() + 1000000
        val futureTimestampLexicon = LexiconEntity(
            id = 1L,
            name = "future_timestamp_lexicon",
            words = "[\"test\"]",
            updatedTs = futureTimestamp
        )

        // Assert
        assertNotNull("Future timestamp lexicon should be created", futureTimestampLexicon)
        assertEquals("Future timestamp should be preserved", futureTimestamp, futureTimestampLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles extremely large ID correctly`() {
        // Arrange & Act
        val largeIdLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "large_id_lexicon",
            words = "[\"test\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Large ID lexicon should be created", largeIdLexicon)
        assertEquals("Large ID should be preserved", Long.MAX_VALUE, largeIdLexicon.id)
    }

    @Test
    fun `LexiconEntity handles extremely small ID correctly`() {
        // Arrange & Act
        val smallIdLexicon = LexiconEntity(
            id = Long.MIN_VALUE,
            name = "small_id_lexicon",
            words = "[\"test\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Small ID lexicon should be created", smallIdLexicon)
        assertEquals("Small ID should be preserved", Long.MIN_VALUE, smallIdLexicon.id)
    }

    @Test
    fun `LexiconEntity handles extremely large updatedTs correctly`() {
        // Arrange & Act
        val largeUpdatedTsLexicon = LexiconEntity(
            id = 1L,
            name = "large_updated_ts_lexicon",
            words = "[\"test\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Large updatedTs lexicon should be created", largeUpdatedTsLexicon)
        assertEquals("Large updatedTs should be preserved", Long.MAX_VALUE, largeUpdatedTsLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles extremely small updatedTs correctly`() {
        // Arrange & Act
        val smallUpdatedTsLexicon = LexiconEntity(
            id = 1L,
            name = "small_updated_ts_lexicon",
            words = "[\"test\"]",
            updatedTs = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Small updatedTs lexicon should be created", smallUpdatedTsLexicon)
        assertEquals("Small updatedTs should be preserved", Long.MIN_VALUE, smallUpdatedTsLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles words with extremely complex content correctly`() {
        // Arrange & Act
        val extremelyComplexWords = "[\"Extremely complex: ünícódé 🚀🌟, ñúméros 123.456, \\\"quotes\\\", {{{nested}}}, ∑∆π∞, ♔♕♖♗♘♙, and móre!\"]"
        val extremelyComplexLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(1000),
            words = extremelyComplexWords,
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Extremely complex lexicon should be created", extremelyComplexLexicon)
        assertEquals("ID should be maximum", Long.MAX_VALUE, extremelyComplexLexicon.id)
        assertEquals("Name should be extremely long", 1000, extremelyComplexLexicon.name.length)
        assertEquals("Words should be extremely long", 142, extremelyComplexLexicon.words.length)
        assertEquals("UpdatedTs should be maximum", Long.MAX_VALUE, extremelyComplexLexicon.updatedTs)

        assertTrue("Words should contain unicode", extremelyComplexLexicon.words.contains("ünícódé"))
        assertTrue("Words should contain emojis", extremelyComplexLexicon.words.contains("🚀") && extremelyComplexLexicon.words.contains("🌟"))
        assertTrue("Words should contain numbers", extremelyComplexLexicon.words.contains("123.456"))
        assertTrue("Words should contain quotes", extremelyComplexLexicon.words.contains("\\\"quotes\\\""))
        assertTrue("Words should contain nested braces", extremelyComplexLexicon.words.contains("{{{nested}}}"))
        assertTrue("Words should contain math symbols", extremelyComplexLexicon.words.contains("∑∆π∞"))
        assertTrue("Words should contain chess symbols", extremelyComplexLexicon.words.contains("♔♕♖♗♘♙"))
        assertTrue("Words should contain more accents", extremelyComplexLexicon.words.contains("móre!"))
    }

    @Test
    fun `LexiconEntity handles words with zero-width joiner emojis correctly`() {
        // Arrange & Act
        val zwjEmojiWordsLexicon = LexiconEntity(
            id = 1L,
            name = "zwj_emoji_words_lexicon",
            words = "[\"👨‍💻\",\"👩‍🎨\",\"👨‍🚀\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("ZWJ emoji words lexicon should be created", zwjEmojiWordsLexicon)
        assertTrue("Words should contain programmer", zwjEmojiWordsLexicon.words.contains("👨‍💻"))
        assertTrue("Words should contain artist", zwjEmojiWordsLexicon.words.contains("👩‍🎨"))
        assertTrue("Words should contain astronaut", zwjEmojiWordsLexicon.words.contains("👨‍🚀"))
    }

    @Test
    fun `LexiconEntity handles words with skin tone modifiers correctly`() {
        // Arrange & Act
        val skinToneWordsLexicon = LexiconEntity(
            id = 1L,
            name = "skin_tone_words_lexicon",
            words = "[\"👋🏻\",\"👋🏼\",\"👋🏽\",\"👋🏾\",\"👋🏿\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Skin tone words lexicon should be created", skinToneWordsLexicon)
        assertTrue("Words should contain light skin tone", skinToneWordsLexicon.words.contains("👋🏻"))
        assertTrue("Words should contain medium-light skin tone", skinToneWordsLexicon.words.contains("👋🏼"))
        assertTrue("Words should contain medium skin tone", skinToneWordsLexicon.words.contains("👋🏽"))
        assertTrue("Words should contain medium-dark skin tone", skinToneWordsLexicon.words.contains("👋🏾"))
        assertTrue("Words should contain dark skin tone", skinToneWordsLexicon.words.contains("👋🏿"))
    }

    @Test
    fun `LexiconEntity handles words with country flags correctly`() {
        // Arrange & Act
        val countryFlagsWordsLexicon = LexiconEntity(
            id = 1L,
            name = "country_flags_words_lexicon",
            words = "[\"🇺🇸\",\"🇬🇧\",\"🇫🇷\",\"🇩🇪\",\"🇯🇵\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Country flags words lexicon should be created", countryFlagsWordsLexicon)
        assertTrue("Words should contain US flag", countryFlagsWordsLexicon.words.contains("🇺🇸"))
        assertTrue("Words should contain UK flag", countryFlagsWordsLexicon.words.contains("🇬🇧"))
        assertTrue("Words should contain France flag", countryFlagsWordsLexicon.words.contains("🇫🇷"))
        assertTrue("Words should contain Germany flag", countryFlagsWordsLexicon.words.contains("🇩🇪"))
        assertTrue("Words should contain Japan flag", countryFlagsWordsLexicon.words.contains("🇯🇵"))
    }

    @Test
    fun `LexiconEntity handles words with astronomical symbols correctly`() {
        // Arrange & Act
        val astronomicalWordsLexicon = LexiconEntity(
            id = 1L,
            name = "astronomical_words_lexicon",
            words = "[\"☀️\",\"🌙\",\"⭐\",\"🌟\",\"🌠\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Astronomical words lexicon should be created", astronomicalWordsLexicon)
        assertTrue("Words should contain sun", astronomicalWordsLexicon.words.contains("☀️"))
        assertTrue("Words should contain moon", astronomicalWordsLexicon.words.contains("🌙"))
        assertTrue("Words should contain star", astronomicalWordsLexicon.words.contains("⭐"))
        assertTrue("Words should contain glowing star", astronomicalWordsLexicon.words.contains("🌟"))
        assertTrue("Words should contain shooting star", astronomicalWordsLexicon.words.contains("🌠"))
    }

    @Test
    fun `LexiconEntity handles words with musical notation correctly`() {
        // Arrange & Act
        val musicalWordsLexicon = LexiconEntity(
            id = 1L,
            name = "musical_words_lexicon",
            words = "[\"♪\",\"♫\",\"♬\",\"♭\",\"♮\"]",
            updatedTs = 1000L
        )

        // Assert
        assertNotNull("Musical words lexicon should be created", musicalWordsLexicon)
        assertTrue("Words should contain eighth note", musicalWordsLexicon.words.contains("♪"))
        assertTrue("Words should contain beamed notes", musicalWordsLexicon.words.contains("♫"))
        assertTrue("Words should contain beamed sixteenth notes", musicalWordsLexicon.words.contains("♬"))
        assertTrue("Words should contain flat", musicalWordsLexicon.words.contains("♭"))
        assertTrue("Words should contain natural", musicalWordsLexicon.words.contains("♮"))
    }

    @Test
    fun `LexiconEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundaryLexicon = LexiconEntity(
            id = 0L,
            name = "",
            words = "[]",
            updatedTs = 0L
        )

        // Assert
        assertNotNull("Boundary lexicon should be created", boundaryLexicon)
        assertEquals("Zero ID should be preserved", 0L, boundaryLexicon.id)
        assertEquals("Empty name should be preserved", "", boundaryLexicon.name)
        assertEquals("Empty words should be preserved", "[]", boundaryLexicon.words)
        assertEquals("Zero updatedTs should be preserved", 0L, boundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundaryLexicon = LexiconEntity(
            id = Long.MAX_VALUE,
            name = "A".repeat(1000),
            words = "[\"${"A".repeat(1000)}\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximum boundary lexicon should be created", maxBoundaryLexicon)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundaryLexicon.id)
        assertEquals("Maximum name length should be preserved", 1000, maxBoundaryLexicon.name.length)
        assertEquals("Maximum words length should be preserved", 1004, maxBoundaryLexicon.words.length)
        assertEquals("Maximum updatedTs should be preserved", Long.MAX_VALUE, maxBoundaryLexicon.updatedTs)
    }

    @Test
    fun `LexiconEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundaryLexicon = LexiconEntity(
            id = Long.MIN_VALUE,
            name = "",
            words = "[]",
            updatedTs = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Minimum boundary lexicon should be created", minBoundaryLexicon)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundaryLexicon.id)
        assertEquals("Minimum updatedTs should be preserved", Long.MIN_VALUE, minBoundaryLexicon.updatedTs)
    }
}