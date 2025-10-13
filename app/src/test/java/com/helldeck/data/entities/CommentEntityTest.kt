package com.helldeck.data.entities

import com.helldeck.data.CommentEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CommentEntity validation and behavior
 */
class CommentEntityTest {

    @Test
    fun `CommentEntity with valid data creates successfully`() {
        // Arrange & Act
        val comment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Comment should be created", comment)
        assertEquals("ID should match", 1L, comment.id)
        assertEquals("Round ID should match", 1L, comment.roundId)
        assertEquals("Text should match", "Test comment", comment.text)
        assertEquals("Tags should match", "test,comment", comment.tags)
        assertEquals("CreatedAt should match", 1000L, comment.createdAt)
    }

    @Test
    fun `CommentEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val comment = CommentEntity(
            id = 0L,
            roundId = 0L,
            text = "",
            tags = "",
            createdAt = 0L
        )

        // Assert
        assertNotNull("Minimal comment should be created", comment)
        assertEquals("Minimal ID should match", 0L, comment.id)
        assertEquals("Minimal round ID should match", 0L, comment.roundId)
        assertEquals("Minimal text should match", "", comment.text)
        assertEquals("Minimal tags should match", "", comment.tags)
        assertEquals("Minimal createdAt should match", 0L, comment.createdAt)
    }

    @Test
    fun `CommentEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val comment = CommentEntity(
            id = Long.MAX_VALUE,
            roundId = Long.MAX_VALUE,
            text = "A".repeat(1000),
            tags = List(100) { "tag$it" }.joinToString(","),
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximal comment should be created", comment)
        assertEquals("Maximal ID should match", Long.MAX_VALUE, comment.id)
        assertEquals("Maximal round ID should match", Long.MAX_VALUE, comment.roundId)
        assertEquals("Maximal text should match", "A".repeat(1000), comment.text)
        assertEquals("Maximal tags should match", List(100) { "tag$it" }.joinToString(","), comment.tags)
        assertEquals("Maximal createdAt should match", Long.MAX_VALUE, comment.createdAt)
    }

    @Test
    fun `CommentEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Original text",
            tags = "original",
            createdAt = 1000L
        )

        // Act
        val copiedComment = originalComment.copy(
            text = "Copied text",
            tags = "copied,new"
        )

        // Assert
        assertNotNull("Copied comment should not be null", copiedComment)
        assertEquals("ID should remain same", originalComment.id, copiedComment.id)
        assertEquals("Round ID should remain same", originalComment.roundId, copiedComment.roundId)
        assertEquals("CreatedAt should remain same", originalComment.createdAt, copiedComment.createdAt)

        assertEquals("Text should be updated", "Copied text", copiedComment.text)
        assertEquals("Tags should be updated", "copied,new", copiedComment.tags)

        assertNotEquals("Original and copy should not be same object", originalComment, copiedComment)
    }

    @Test
    fun `CommentEntity handles special characters correctly`() {
        // Arrange & Act
        val specialComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Comment with spécial çharácters and émojis 🚀!",
            tags = "spécial,émojis,tëst"
        )

        // Assert
        assertNotNull("Special comment should be created", specialComment)
        assertEquals("Special text should be preserved",
            "Comment with spécial çharácters and émojis 🚀!", specialComment.text)
        assertEquals("Special tags should be preserved",
            "spécial,émojis,tëst", specialComment.tags)
    }

    @Test
    fun `CommentEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯",
            tags = "ünícódé,tëst,ñëw"
        )

        // Assert
        assertNotNull("Unicode comment should be created", unicodeComment)
        assertEquals("Unicode text should be preserved",
            "Ünïcödé tëxt wíth spëcial çhâractërs 🚀🌟🎯", unicodeComment.text)
        assertEquals("Unicode tags should be preserved",
            "ünícódé,tëst,ñëw", unicodeComment.tags)
    }

    @Test
    fun `CommentEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptyComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "",
            tags = "",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Empty comment should be created", emptyComment)
        assertEquals("Empty text should be preserved", "", emptyComment.text)
        assertEquals("Empty tags should be preserved", "", emptyComment.tags)
    }

    @Test
    fun `CommentEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespaceComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "   ",
            tags = "   ",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Whitespace comment should be created", whitespaceComment)
        assertEquals("Whitespace text should be preserved", "   ", whitespaceComment.text)
        assertEquals("Whitespace tags should be preserved", "   ", whitespaceComment.tags)
    }

    @Test
    fun `CommentEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundaryComment = CommentEntity(
            id = 0L,
            roundId = 0L,
            text = "Boundary test",
            tags = "boundary,test",
            createdAt = 0L
        )

        // Assert
        assertNotNull("Boundary comment should be created", boundaryComment)
        assertEquals("Zero ID should be preserved", 0L, boundaryComment.id)
        assertEquals("Zero round ID should be preserved", 0L, boundaryComment.roundId)
        assertEquals("Zero createdAt should be preserved", 0L, boundaryComment.createdAt)
    }

    @Test
    fun `CommentEntity handles very long text correctly`() {
        // Arrange & Act
        val longText = "A".repeat(1000) + " Very Long Comment Text " + "B".repeat(1000)
        val longTextComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = longText,
            tags = "long,text,comment"
        )

        // Assert
        assertNotNull("Long text comment should be created", longTextComment)
        assertEquals("Long text should be preserved", longText, longTextComment.text)
        assertEquals("Long text length should match", 2027, longTextComment.text.length)
    }

    @Test
    fun `CommentEntity handles very long tags correctly`() {
        // Arrange & Act
        val longTags = List(1000) { i -> "verylongtagnamethatmightcauseissues$i" }.joinToString(",")
        val longTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Long tags comment",
            tags = longTags
        )

        // Assert
        assertNotNull("Long tags comment should be created", longTagsComment)
        assertEquals("Long tags should be preserved", longTags, longTagsComment.tags)
        assertEquals("Long tags length should match", 30003, longTagsComment.tags.length)
    }

    @Test
    fun `CommentEntity handles emojis in text correctly`() {
        // Arrange & Act
        val emojiComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Emoji comment 🚀 with 🌟 emojis ⭐ in text",
            tags = "emoji,comment,test"
        )

        // Assert
        assertNotNull("Emoji comment should be created", emojiComment)
        assertTrue("Text should contain rocket emoji", emojiComment.text.contains("🚀"))
        assertTrue("Text should contain star emoji", emojiComment.text.contains("🌟"))
        assertTrue("Text should contain star emoji", emojiComment.text.contains("⭐"))
    }

    @Test
    fun `CommentEntity handles numbers in text correctly`() {
        // Arrange & Act
        val numbersComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Numbers comment 123 with 456.789 decimals",
            tags = "numbers,test"
        )

        // Assert
        assertNotNull("Numbers comment should be created", numbersComment)
        assertTrue("Text should contain numbers", numbersComment.text.contains("123"))
        assertTrue("Text should contain decimal", numbersComment.text.contains("456.789"))
    }

    @Test
    fun `CommentEntity handles special regex characters in text correctly`() {
        // Arrange & Act
        val regexCharsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Regex chars comment ^$*+?{}[]\\|() content",
            tags = "regex,test"
        )

        // Assert
        assertNotNull("Regex chars comment should be created", regexCharsComment)
        assertTrue("Text should contain regex characters", regexCharsComment.text.contains("^$*+?{}[]\\|()"))
    }

    @Test
    fun `CommentEntity handles quotes in text correctly`() {
        // Arrange & Act
        val quotesComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Quotes comment \"with double\" and 'single quotes'",
            tags = "quotes,test"
        )

        // Assert
        assertNotNull("Quotes comment should be created", quotesComment)
        assertTrue("Text should contain double quotes", quotesComment.text.contains("\"with double\""))
        assertTrue("Text should contain single quotes", quotesComment.text.contains("'single quotes'"))
    }

    @Test
    fun `CommentEntity handles newlines in text correctly`() {
        // Arrange & Act
        val newlinesComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Newlines comment\nwith\r\nnewlines",
            tags = "newlines,test"
        )

        // Assert
        assertNotNull("Newlines comment should be created", newlinesComment)
        assertTrue("Text should contain newlines", newlinesComment.text.contains("\n"))
        assertTrue("Text should contain carriage returns", newlinesComment.text.contains("\r"))
    }

    @Test
    fun `CommentEntity handles tabs in text correctly`() {
        // Arrange & Act
        val tabsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Tabs comment\twith\ttabs",
            tags = "tabs,test"
        )

        // Assert
        assertNotNull("Tabs comment should be created", tabsComment)
        assertTrue("Text should contain tabs", tabsComment.text.contains("\t"))
    }

    @Test
    fun `CommentEntity handles HTML-like content in text correctly`() {
        // Arrange & Act
        val htmlLikeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "HTML-like comment <with> <html> like & content",
            tags = "html,test"
        )

        // Assert
        assertNotNull("HTML-like comment should be created", htmlLikeComment)
        assertTrue("Text should contain angle brackets", htmlLikeComment.text.contains("<with>"))
        assertTrue("Text should contain HTML tag", htmlLikeComment.text.contains("<html>"))
        assertTrue("Text should contain HTML entity", htmlLikeComment.text.contains("&"))
    }

    @Test
    fun `CommentEntity handles URLs in text correctly`() {
        // Arrange & Act
        val urlComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "URL comment https://example.com with ftp://test.org URLs",
            tags = "url,test"
        )

        // Assert
        assertNotNull("URL comment should be created", urlComment)
        assertTrue("Text should contain HTTPS URL", urlComment.text.contains("https://example.com"))
        assertTrue("Text should contain FTP URL", urlComment.text.contains("ftp://test.org"))
    }

    @Test
    fun `CommentEntity handles email addresses in text correctly`() {
        // Arrange & Act
        val emailComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Email comment user@example.com and test.email+tag@domain.co.uk",
            tags = "email,test"
        )

        // Assert
        assertNotNull("Email comment should be created", emailComment)
        assertTrue("Text should contain email", emailComment.text.contains("user@example.com"))
        assertTrue("Text should contain complex email", emailComment.text.contains("test.email+tag@domain.co.uk"))
    }

    @Test
    fun `CommentEntity handles file paths in text correctly`() {
        // Arrange & Act
        val filePathComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "File path comment /usr/local/bin and C:\\Windows\\System32 paths",
            tags = "filepath,test"
        )

        // Assert
        assertNotNull("File path comment should be created", filePathComment)
        assertTrue("Text should contain Unix path", filePathComment.text.contains("/usr/local/bin"))
        assertTrue("Text should contain Windows path", filePathComment.text.contains("C:\\Windows\\System32"))
    }

    @Test
    fun `CommentEntity handles JSON-like content in text correctly`() {
        // Arrange & Act
        val jsonLikeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "JSON-like comment {\"key\": \"value\", \"number\": 123} content",
            tags = "json,test"
        )

        // Assert
        assertNotNull("JSON-like comment should be created", jsonLikeComment)
        assertTrue("Text should contain JSON object", jsonLikeComment.text.contains("{\"key\": \"value\", \"number\": 123}"))
    }

    @Test
    fun `CommentEntity handles SQL-like content in text correctly`() {
        // Arrange & Act
        val sqlLikeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "SQL-like comment SELECT * FROM table WHERE condition = 'value'",
            tags = "sql,test"
        )

        // Assert
        assertNotNull("SQL-like comment should be created", sqlLikeComment)
        assertTrue("Text should contain SQL query", sqlLikeComment.text.contains("SELECT * FROM table WHERE condition = 'value'"))
    }

    @Test
    fun `CommentEntity handles programming code in text correctly`() {
        // Arrange & Act
        val codeLikeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Code-like comment if (points > 100) { celebrate(); } content",
            tags = "code,test"
        )

        // Assert
        assertNotNull("Code-like comment should be created", codeLikeComment)
        assertTrue("Text should contain if statement", codeLikeComment.text.contains("if (points > 100)"))
        assertTrue("Text should contain braces", codeLikeComment.text.contains("{ celebrate(); }"))
    }

    @Test
    fun `CommentEntity handles mathematical symbols in text correctly`() {
        // Arrange & Act
        val mathSymbolsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Math symbols comment ∑∆π∞ with √∫∂ symbols",
            tags = "math,test"
        )

        // Assert
        assertNotNull("Math symbols comment should be created", mathSymbolsComment)
        assertTrue("Text should contain sum symbol", mathSymbolsComment.text.contains("∑"))
        assertTrue("Text should contain delta symbol", mathSymbolsComment.text.contains("∆"))
        assertTrue("Text should contain pi symbol", mathSymbolsComment.text.contains("π"))
        assertTrue("Text should contain infinity symbol", mathSymbolsComment.text.contains("∞"))
        assertTrue("Text should contain square root symbol", mathSymbolsComment.text.contains("√"))
        assertTrue("Text should contain integral symbol", mathSymbolsComment.text.contains("∫"))
        assertTrue("Text should contain partial symbol", mathSymbolsComment.text.contains("∂"))
    }

    @Test
    fun `CommentEntity handles binary-like content in text correctly`() {
        // Arrange & Act
        val binaryLikeComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Binary-like comment 01010101 binary 11001100 content",
            tags = "binary,test"
        )

        // Assert
        assertNotNull("Binary-like comment should be created", binaryLikeComment)
        assertTrue("Text should contain binary-like content", binaryLikeComment.text.contains("01010101"))
        assertTrue("Text should contain more binary-like content", binaryLikeComment.text.contains("11001100"))
    }

    @Test
    fun `CommentEntity handles right-to-left text in text correctly`() {
        // Arrange & Act
        val rtlComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "RTL comment العربية ועברית RTL content",
            tags = "rtl,test"
        )

        // Assert
        assertNotNull("RTL comment should be created", rtlComment)
        assertTrue("Text should contain Arabic text", rtlComment.text.contains("العربية"))
        assertTrue("Text should contain Hebrew text", rtlComment.text.contains("עברית"))
    }

    @Test
    fun `CommentEntity handles combining characters in text correctly`() {
        // Arrange & Act
        val combiningCharsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Combining chars comment áéíóú with combining characters",
            tags = "combining,test"
        )

        // Assert
        assertNotNull("Combining chars comment should be created", combiningCharsComment)
        assertTrue("Text should contain combining characters", combiningCharsComment.text.contains("á"))
        assertTrue("Text should contain more combining characters", combiningCharsComment.text.contains("é"))
    }

    @Test
    fun `CommentEntity handles zero-width characters in text correctly`() {
        // Arrange & Act
        val zeroWidthComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Zero width comment\u200Bwith\u200Czero\u200Dwidth\u200Echaracters\u200F",
            tags = "zero,width,test"
        )

        // Assert
        assertNotNull("Zero width comment should be created", zeroWidthComment)
        assertTrue("Text should contain zero-width characters", zeroWidthComment.text.contains("\u200B"))
    }

    @Test
    fun `CommentEntity handles invisible characters in text correctly`() {
        // Arrange & Act
        val invisibleCharsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Invisible chars comment\u0000with\u0001invisible\u0002chars\u0003",
            tags = "invisible,test"
        )

        // Assert
        assertNotNull("Invisible chars comment should be created", invisibleCharsComment)
        assertTrue("Text should contain invisible characters", invisibleCharsComment.text.contains("\u0000"))
    }

    @Test
    fun `CommentEntity handles mixed content in text correctly`() {
        // Arrange & Act
        val mixedContentComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Mixed números 123, emojis 🚀🌟, spécial çharácters, \"quotes\", and móre!",
            tags = "mixed,content,test"
        )

        // Assert
        assertNotNull("Mixed content comment should be created", mixedContentComment)
        assertTrue("Text should contain numbers", mixedContentComment.text.contains("123"))
        assertTrue("Text should contain emojis", mixedContentComment.text.contains("🚀") && mixedContentComment.text.contains("🌟"))
        assertTrue("Text should contain special characters", mixedContentComment.text.contains("çharácters"))
        assertTrue("Text should contain quotes", mixedContentComment.text.contains("\"quotes\""))
        assertTrue("Text should contain more accents", mixedContentComment.text.contains("móre"))
    }

    @Test
    fun `CommentEntity handles extremely long text correctly`() {
        // Arrange & Act
        val extremelyLongText = "A".repeat(10000) + " Extremely Long Comment Text " + "B".repeat(10000)
        val extremelyLongTextComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = extremelyLongText,
            tags = "extremely,long,text"
        )

        // Assert
        assertNotNull("Extremely long text comment should be created", extremelyLongTextComment)
        assertEquals("Extremely long text should be preserved", extremelyLongText, extremelyLongTextComment.text)
        assertEquals("Extremely long text length should match", 40005, extremelyLongTextComment.text.length)
    }

    @Test
    fun `CommentEntity handles extremely long tags correctly`() {
        // Arrange & Act
        val extremelyLongTags = List(1000) { i -> "verylongtagnamethatmightcauseissues$i" }.joinToString(",")
        val extremelyLongTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Extremely long tags comment",
            tags = extremelyLongTags
        )

        // Assert
        assertNotNull("Extremely long tags comment should be created", extremelyLongTagsComment)
        assertEquals("Extremely long tags should be preserved", extremelyLongTags, extremelyLongTagsComment.tags)
        assertEquals("Extremely long tags length should match", 30003, extremelyLongTagsComment.tags.length)
    }

    @Test
    fun `CommentEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseComment = CommentEntity(
            id = Long.MAX_VALUE,
            roundId = Long.MAX_VALUE,
            text = "Edge case: ünícódé 🚀🌟, ñúméros 123.456, \"'quotes'\", {{{nested}}}, \\commands, <tags>, ∑∆π∞, and móre!",
            tags = "edge,case,ünícódé,🚀,ñúméros,quotes,nested,commands,tags,math,móre",
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Edge case comment should be created", edgeCaseComment)
        assertEquals("ID should be maximum", Long.MAX_VALUE, edgeCaseComment.id)
        assertEquals("Round ID should be maximum", Long.MAX_VALUE, edgeCaseComment.roundId)
        assertEquals("Text should be extremely long", 124, edgeCaseComment.text.length)
        assertEquals("Tags should be extremely long", 89, edgeCaseComment.tags.length)
        assertEquals("CreatedAt should be maximum", Long.MAX_VALUE, edgeCaseComment.createdAt)

        assertTrue("Text should contain unicode", edgeCaseComment.text.contains("ünícódé"))
        assertTrue("Text should contain emojis", edgeCaseComment.text.contains("🚀") && edgeCaseComment.text.contains("🌟"))
        assertTrue("Text should contain numbers", edgeCaseComment.text.contains("123.456"))
        assertTrue("Text should contain quotes", edgeCaseComment.text.contains("\"'quotes'\""))
        assertTrue("Text should contain nested braces", edgeCaseComment.text.contains("{{{nested}}}"))
        assertTrue("Text should contain commands", edgeCaseComment.text.contains("\\commands"))
        assertTrue("Text should contain tags", edgeCaseComment.text.contains("<tags>"))
        assertTrue("Text should contain math symbols", edgeCaseComment.text.contains("∑∆π∞"))
        assertTrue("Text should contain more accents", edgeCaseComment.text.contains("móre"))

        assertTrue("Tags should contain unicode", edgeCaseComment.tags.contains("ünícódé"))
        assertTrue("Tags should contain emojis", edgeCaseComment.tags.contains("🚀"))
        assertTrue("Tags should contain numbers", edgeCaseComment.tags.contains("ñúméros"))
        assertTrue("Tags should contain quotes", edgeCaseComment.tags.contains("quotes"))
        assertTrue("Tags should contain nested", edgeCaseComment.tags.contains("nested"))
        assertTrue("Tags should contain commands", edgeCaseComment.tags.contains("commands"))
        assertTrue("Tags should contain tags", edgeCaseComment.tags.contains("tags"))
        assertTrue("Tags should contain math", edgeCaseComment.tags.contains("math"))
        assertTrue("Tags should contain more accents", edgeCaseComment.tags.contains("móre"))
    }

    @Test
    fun `CommentEntity equality works correctly`() {
        // Arrange
        val comment1 = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        val comment2 = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        val comment3 = CommentEntity(
            id = 2L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        // Assert
        assertEquals("Identical comments should be equal", comment1, comment2)
        assertNotEquals("Different comments should not be equal", comment1, comment3)
        assertEquals("Hash codes should be equal for identical comments", comment1.hashCode(), comment2.hashCode())
        assertTrue("Hash codes should be different for different comments", comment1.hashCode() != comment3.hashCode())
    }

    @Test
    fun `CommentEntity toString contains relevant information`() {
        // Arrange
        val comment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        // Act
        val toString = comment.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("id=1"))
        assertTrue("toString should contain round ID", toString.contains("roundId=1"))
        assertTrue("toString should contain text", toString.contains("Test comment"))
        assertTrue("toString should contain tags", toString.contains("test,comment"))
        assertTrue("toString should contain createdAt", toString.contains("createdAt=1000"))
    }

    @Test
    fun `CommentEntity handles single tag correctly`() {
        // Arrange & Act
        val singleTagComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Single tag comment",
            tags = "single"
        )

        // Assert
        assertNotNull("Single tag comment should be created", singleTagComment)
        assertEquals("Single tag should be preserved", "single", singleTagComment.tags)
    }

    @Test
    fun `CommentEntity handles many tags correctly`() {
        // Arrange & Act
        val manyTags = (1..100).joinToString(",") { "tag$it" }
        val manyTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Many tags comment",
            tags = manyTags
        )

        // Assert
        assertNotNull("Many tags comment should be created", manyTagsComment)
        assertEquals("Many tags should be preserved", manyTags, manyTagsComment.tags)
        assertEquals("Tag count should be 100", 100, manyTagsComment.tags.split(",").size)
    }

    @Test
    fun `CommentEntity handles tags with spaces correctly`() {
        // Arrange & Act
        val spaceTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Space tags comment",
            tags = "tag with spaces, another tag , third tag"
        )

        // Assert
        assertNotNull("Space tags comment should be created", spaceTagsComment)
        assertTrue("Tags should contain spaces", spaceTagsComment.tags.contains("tag with spaces"))
        assertTrue("Tags should contain another tag", spaceTagsComment.tags.contains("another tag"))
        assertTrue("Tags should contain third tag", spaceTagsComment.tags.contains("third tag"))
    }

    @Test
    fun `CommentEntity handles tags with special characters correctly`() {
        // Arrange & Act
        val specialTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Special tags comment",
            tags = "spécial,émojis,tëst,ünícódé,ñormal"
        )

        // Assert
        assertNotNull("Special tags comment should be created", specialTagsComment)
        assertTrue("Tags should contain spécial", specialTagsComment.tags.contains("spécial"))
        assertTrue("Tags should contain émojis", specialTagsComment.tags.contains("émojis"))
        assertTrue("Tags should contain tëst", specialTagsComment.tags.contains("tëst"))
        assertTrue("Tags should contain ünícódé", specialTagsComment.tags.contains("ünícódé"))
        assertTrue("Tags should contain ñormal", specialTagsComment.tags.contains("ñormal"))
    }

    @Test
    fun `CommentEntity handles tags with emojis correctly`() {
        // Arrange & Act
        val emojiTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Emoji tags comment",
            tags = "🚀,🌟,🎯,🎲,tag"
        )

        // Assert
        assertNotNull("Emoji tags comment should be created", emojiTagsComment)
        assertTrue("Tags should contain rocket emoji", emojiTagsComment.tags.contains("🚀"))
        assertTrue("Tags should contain star emoji", emojiTagsComment.tags.contains("🌟"))
        assertTrue("Tags should contain dart emoji", emojiTagsComment.tags.contains("🎯"))
        assertTrue("Tags should contain dice emoji", emojiTagsComment.tags.contains("🎲"))
        assertTrue("Tags should contain normal tag", emojiTagsComment.tags.contains("tag"))
    }

    @Test
    fun `CommentEntity handles tags with numbers correctly`() {
        // Arrange & Act
        val numberTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Number tags comment",
            tags = "tag1,tag2,tag123,tag456.789"
        )

        // Assert
        assertNotNull("Number tags comment should be created", numberTagsComment)
        assertTrue("Tags should contain tag1", numberTagsComment.tags.contains("tag1"))
        assertTrue("Tags should contain tag2", numberTagsComment.tags.contains("tag2"))
        assertTrue("Tags should contain tag123", numberTagsComment.tags.contains("tag123"))
        assertTrue("Tags should contain tag456.789", numberTagsComment.tags.contains("tag456.789"))
    }

    @Test
    fun `CommentEntity handles future timestamp correctly`() {
        // Arrange & Act
        val futureTimestamp = System.currentTimeMillis() + 1000000
        val futureTimestampComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Future timestamp comment",
            tags = "future,test",
            createdAt = futureTimestamp
        )

        // Assert
        assertNotNull("Future timestamp comment should be created", futureTimestampComment)
        assertEquals("Future timestamp should be preserved", futureTimestamp, futureTimestampComment.createdAt)
    }

    @Test
    fun `CommentEntity handles extremely large round ID correctly`() {
        // Arrange & Act
        val largeRoundIdComment = CommentEntity(
            id = 1L,
            roundId = Long.MAX_VALUE,
            text = "Large round ID comment",
            tags = "large,round,id",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Large round ID comment should be created", largeRoundIdComment)
        assertEquals("Large round ID should be preserved", Long.MAX_VALUE, largeRoundIdComment.roundId)
    }

    @Test
    fun `CommentEntity handles extremely small round ID correctly`() {
        // Arrange & Act
        val smallRoundIdComment = CommentEntity(
            id = 1L,
            roundId = Long.MIN_VALUE,
            text = "Small round ID comment",
            tags = "small,round,id",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Small round ID comment should be created", smallRoundIdComment)
        assertEquals("Small round ID should be preserved", Long.MIN_VALUE, smallRoundIdComment.roundId)
    }

    @Test
    fun `CommentEntity handles extremely large ID correctly`() {
        // Arrange & Act
        val largeIdComment = CommentEntity(
            id = Long.MAX_VALUE,
            roundId = 1L,
            text = "Large ID comment",
            tags = "large,id",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Large ID comment should be created", largeIdComment)
        assertEquals("Large ID should be preserved", Long.MAX_VALUE, largeIdComment.id)
    }

    @Test
    fun `CommentEntity handles extremely small ID correctly`() {
        // Arrange & Act
        val smallIdComment = CommentEntity(
            id = Long.MIN_VALUE,
            roundId = 1L,
            text = "Small ID comment",
            tags = "small,id",
            createdAt = 1000L
        )

        // Assert
        assertNotNull("Small ID comment should be created", smallIdComment)
        assertEquals("Small ID should be preserved", Long.MIN_VALUE, smallIdComment.id)
    }

    @Test
    fun `CommentEntity handles extremely large createdAt correctly`() {
        // Arrange & Act
        val largeCreatedAtComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Large createdAt comment",
            tags = "large,createdAt",
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Large createdAt comment should be created", largeCreatedAtComment)
        assertEquals("Large createdAt should be preserved", Long.MAX_VALUE, largeCreatedAtComment.createdAt)
    }

    @Test
    fun `CommentEntity handles extremely small createdAt correctly`() {
        // Arrange & Act
        val smallCreatedAtComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Small createdAt comment",
            tags = "small,createdAt",
            createdAt = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Small createdAt comment should be created", smallCreatedAtComment)
        assertEquals("Small createdAt should be preserved", Long.MIN_VALUE, smallCreatedAtComment.createdAt)
    }

    @Test
    fun `CommentEntity handles tags with commas correctly`() {
        // Arrange & Act
        val commaTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Comma tags comment",
            tags = "tag,with,commas,tag with, commas"
        )

        // Assert
        assertNotNull("Comma tags comment should be created", commaTagsComment)
        assertTrue("Tags should contain 'tag,with,commas'", commaTagsComment.tags.contains("tag,with,commas"))
        assertTrue("Tags should contain 'tag with, commas'", commaTagsComment.tags.contains("tag with, commas"))
    }

    @Test
    fun `CommentEntity handles tags with quotes correctly`() {
        // Arrange & Act
        val quoteTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Quote tags comment",
            tags = "\"quoted tag\",'single quoted tag'"
        )

        // Assert
        assertNotNull("Quote tags comment should be created", quoteTagsComment)
        assertTrue("Tags should contain quoted tag", quoteTagsComment.tags.contains("\"quoted tag\""))
        assertTrue("Tags should contain single quoted tag", quoteTagsComment.tags.contains("'single quoted tag'"))
    }

    @Test
    fun `CommentEntity handles tags with nested content correctly`() {
        // Arrange & Act
        val nestedTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Nested tags comment",
            tags = "tag{nested},tag[nested],tag(nested)"
        )

        // Assert
        assertNotNull("Nested tags comment should be created", nestedTagsComment)
        assertTrue("Tags should contain nested braces", nestedTagsComment.tags.contains("tag{nested}"))
        assertTrue("Tags should contain nested brackets", nestedTagsComment.tags.contains("tag[nested]"))
        assertTrue("Tags should contain nested parentheses", nestedTagsComment.tags.contains("tag(nested)"))
    }

    @Test
    fun `CommentEntity handles tags with mathematical symbols correctly`() {
        // Arrange & Act
        val mathTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Math tags comment",
            tags = "∑,∆,π,∞,√,∫,∂,math"
        )

        // Assert
        assertNotNull("Math tags comment should be created", mathTagsComment)
        assertTrue("Tags should contain sum symbol", mathTagsComment.tags.contains("∑"))
        assertTrue("Tags should contain delta symbol", mathTagsComment.tags.contains("∆"))
        assertTrue("Tags should contain pi symbol", mathTagsComment.tags.contains("π"))
        assertTrue("Tags should contain infinity symbol", mathTagsComment.tags.contains("∞"))
        assertTrue("Tags should contain square root symbol", mathTagsComment.tags.contains("√"))
        assertTrue("Tags should contain integral symbol", mathTagsComment.tags.contains("∫"))
        assertTrue("Tags should contain partial symbol", mathTagsComment.tags.contains("∂"))
        assertTrue("Tags should contain math", mathTagsComment.tags.contains("math"))
    }

    @Test
    fun `CommentEntity handles tags with chess symbols correctly`() {
        // Arrange & Act
        val chessTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Chess tags comment",
            tags = "♔,♕,♖,♗,♘,♙,chess"
        )

        // Assert
        assertNotNull("Chess tags comment should be created", chessTagsComment)
        assertTrue("Tags should contain white king", chessTagsComment.tags.contains("♔"))
        assertTrue("Tags should contain white queen", chessTagsComment.tags.contains("♕"))
        assertTrue("Tags should contain white rook", chessTagsComment.tags.contains("♖"))
        assertTrue("Tags should contain white bishop", chessTagsComment.tags.contains("♗"))
        assertTrue("Tags should contain white knight", chessTagsComment.tags.contains("♘"))
        assertTrue("Tags should contain white pawn", chessTagsComment.tags.contains("♙"))
        assertTrue("Tags should contain chess", chessTagsComment.tags.contains("chess"))
    }

    @Test
    fun `CommentEntity handles tags with card symbols correctly`() {
        // Arrange & Act
        val cardTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Card tags comment",
            tags = "♠,♥,♦,♣,🃏,cards"
        )

        // Assert
        assertNotNull("Card tags comment should be created", cardTagsComment)
        assertTrue("Tags should contain spade", cardTagsComment.tags.contains("♠"))
        assertTrue("Tags should contain heart", cardTagsComment.tags.contains("♥"))
        assertTrue("Tags should contain diamond", cardTagsComment.tags.contains("♦"))
        assertTrue("Tags should contain club", cardTagsComment.tags.contains("♣"))
        assertTrue("Tags should contain joker", cardTagsComment.tags.contains("🃏"))
        assertTrue("Tags should contain cards", cardTagsComment.tags.contains("cards"))
    }

    @Test
    fun `CommentEntity handles tags with dice and game symbols correctly`() {
        // Arrange & Act
        val gameTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Game tags comment",
            tags = "🎲,🎯,🎮,🎰,🎳,games"
        )

        // Assert
        assertNotNull("Game tags comment should be created", gameTagsComment)
        assertTrue("Tags should contain dice", gameTagsComment.tags.contains("🎲"))
        assertTrue("Tags should contain dart", gameTagsComment.tags.contains("🎯"))
        assertTrue("Tags should contain video game", gameTagsComment.tags.contains("🎮"))
        assertTrue("Tags should contain slot machine", gameTagsComment.tags.contains("🎰"))
        assertTrue("Tags should contain bowling", gameTagsComment.tags.contains("🎳"))
        assertTrue("Tags should contain games", gameTagsComment.tags.contains("games"))
    }

    @Test
    fun `CommentEntity handles tags with weather symbols correctly`() {
        // Arrange & Act
        val weatherTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Weather tags comment",
            tags = "☀️,🌤️,⛅,🌦️,🌧️,weather"
        )

        // Assert
        assertNotNull("Weather tags comment should be created", weatherTagsComment)
        assertTrue("Tags should contain sun", weatherTagsComment.tags.contains("☀️"))
        assertTrue("Tags should contain partly cloudy", weatherTagsComment.tags.contains("🌤️"))
        assertTrue("Tags should contain cloudy", weatherTagsComment.tags.contains("⛅"))
        assertTrue("Tags should contain rainy", weatherTagsComment.tags.contains("🌦️"))
        assertTrue("Tags should contain rain", weatherTagsComment.tags.contains("🌧️"))
        assertTrue("Tags should contain weather", weatherTagsComment.tags.contains("weather"))
    }

    @Test
    fun `CommentEntity handles tags with food and drink symbols correctly`() {
        // Arrange & Act
        val foodTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Food tags comment",
            tags = "🍕,🍔,🍟,🌭,🍿,food"
        )

        // Assert
        assertNotNull("Food tags comment should be created", foodTagsComment)
        assertTrue("Tags should contain pizza", foodTagsComment.tags.contains("🍕"))
        assertTrue("Tags should contain burger", foodTagsComment.tags.contains("🍔"))
        assertTrue("Tags should contain fries", foodTagsComment.tags.contains("🍟"))
        assertTrue("Tags should contain hot dog", foodTagsComment.tags.contains("🌭"))
        assertTrue("Tags should contain popcorn", foodTagsComment.tags.contains("🍿"))
        assertTrue("Tags should contain food", foodTagsComment.tags.contains("food"))
    }

    @Test
    fun `CommentEntity handles tags with activity symbols correctly`() {
        // Arrange & Act
        val activityTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Activity tags comment",
            tags = "⚽,🏀,🏈,⚾,🎾,activity"
        )

        // Assert
        assertNotNull("Activity tags comment should be created", activityTagsComment)
        assertTrue("Tags should contain soccer ball", activityTagsComment.tags.contains("⚽"))
        assertTrue("Tags should contain basketball", activityTagsComment.tags.contains("🏀"))
        assertTrue("Tags should contain football", activityTagsComment.tags.contains("🏈"))
        assertTrue("Tags should contain baseball", activityTagsComment.tags.contains("⚾"))
        assertTrue("Tags should contain tennis ball", activityTagsComment.tags.contains("🎾"))
        assertTrue("Tags should contain activity", activityTagsComment.tags.contains("activity"))
    }

    @Test
    fun `CommentEntity handles tags with travel and place symbols correctly`() {
        // Arrange & Act
        val travelTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Travel tags comment",
            tags = "🚗,✈️,🚲,⛵,🏠,travel"
        )

        // Assert
        assertNotNull("Travel tags comment should be created", travelTagsComment)
        assertTrue("Tags should contain car", travelTagsComment.tags.contains("🚗"))
        assertTrue("Tags should contain airplane", travelTagsComment.tags.contains("✈️"))
        assertTrue("Tags should contain bicycle", travelTagsComment.tags.contains("🚲"))
        assertTrue("Tags should contain boat", travelTagsComment.tags.contains("⛵"))
        assertTrue("Tags should contain house", travelTagsComment.tags.contains("🏠"))
        assertTrue("Tags should contain travel", travelTagsComment.tags.contains("travel"))
    }

    @Test
    fun `CommentEntity handles tags with symbol combinations correctly`() {
        // Arrange & Act
        val symbolComboTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Symbol combo tags comment",
            tags = "👨‍💻,👩‍🎨,🚀,🌟,⚽,🏀,symbols"
        )

        // Assert
        assertNotNull("Symbol combo tags comment should be created", symbolComboTagsComment)
        assertTrue("Tags should contain programmer", symbolComboTagsComment.tags.contains("👨‍💻"))
        assertTrue("Tags should contain artist", symbolComboTagsComment.tags.contains("👩‍🎨"))
        assertTrue("Tags should contain rocket", symbolComboTagsComment.tags.contains("🚀"))
        assertTrue("Tags should contain star", symbolComboTagsComment.tags.contains("🌟"))
        assertTrue("Tags should contain soccer", symbolComboTagsComment.tags.contains("⚽"))
        assertTrue("Tags should contain basketball", symbolComboTagsComment.tags.contains("🏀"))
        assertTrue("Tags should contain symbols", symbolComboTagsComment.tags.contains("symbols"))
    }

    @Test
    fun `CommentEntity handles tags with extremely complex content correctly`() {
        // Arrange & Act
        val extremelyComplexTags = "edge,case,ünícódé,🚀,ñúméros,quotes,nested,commands,tags,∑,∆,π,∞,♔,♕,♖,♗,♘,♙,móre"
        val extremelyComplexComment = CommentEntity(
            id = Long.MAX_VALUE,
            roundId = Long.MAX_VALUE,
            text = "Extremely complex comment",
            tags = extremelyComplexTags,
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Extremely complex comment should be created", extremelyComplexComment)
        assertEquals("ID should be maximum", Long.MAX_VALUE, extremelyComplexComment.id)
        assertEquals("Round ID should be maximum", Long.MAX_VALUE, extremelyComplexComment.roundId)
        assertEquals("CreatedAt should be maximum", Long.MAX_VALUE, extremelyComplexComment.createdAt)
        assertEquals("Tags should be extremely complex", extremelyComplexTags, extremelyComplexComment.tags)

        assertTrue("Tags should contain unicode", extremelyComplexComment.tags.contains("ünícódé"))
        assertTrue("Tags should contain rocket", extremelyComplexComment.tags.contains("🚀"))
        assertTrue("Tags should contain numbers", extremelyComplexComment.tags.contains("ñúméros"))
        assertTrue("Tags should contain quotes", extremelyComplexComment.tags.contains("quotes"))
        assertTrue("Tags should contain nested", extremelyComplexComment.tags.contains("nested"))
        assertTrue("Tags should contain commands", extremelyComplexComment.tags.contains("commands"))
        assertTrue("Tags should contain tags", extremelyComplexComment.tags.contains("tags"))
        assertTrue("Tags should contain sum", extremelyComplexComment.tags.contains("∑"))
        assertTrue("Tags should contain delta", extremelyComplexComment.tags.contains("∆"))
        assertTrue("Tags should contain pi", extremelyComplexComment.tags.contains("π"))
        assertTrue("Tags should contain infinity", extremelyComplexComment.tags.contains("∞"))
        assertTrue("Tags should contain white king", extremelyComplexComment.tags.contains("♔"))
        assertTrue("Tags should contain white queen", extremelyComplexComment.tags.contains("♕"))
        assertTrue("Tags should contain white rook", extremelyComplexComment.tags.contains("♖"))
        assertTrue("Tags should contain white bishop", extremelyComplexComment.tags.contains("♗"))
        assertTrue("Tags should contain white knight", extremelyComplexComment.tags.contains("♘"))
        assertTrue("Tags should contain white pawn", extremelyComplexComment.tags.contains("♙"))
        assertTrue("Tags should contain more accents", extremelyComplexComment.tags.contains("móre"))
    }

    @Test
    fun `CommentEntity handles tags with zero-width joiner emojis correctly`() {
        // Arrange & Act
        val zwjEmojiTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "ZWJ emoji tags comment",
            tags = "👨‍💻,👩‍🎨,👨‍🚀,zwj,emojis"
        )

        // Assert
        assertNotNull("ZWJ emoji tags comment should be created", zwjEmojiTagsComment)
        assertTrue("Tags should contain programmer", zwjEmojiTagsComment.tags.contains("👨‍💻"))
        assertTrue("Tags should contain artist", zwjEmojiTagsComment.tags.contains("👩‍🎨"))
        assertTrue("Tags should contain astronaut", zwjEmojiTagsComment.tags.contains("👨‍🚀"))
        assertTrue("Tags should contain zwj", zwjEmojiTagsComment.tags.contains("zwj"))
        assertTrue("Tags should contain emojis", zwjEmojiTagsComment.tags.contains("emojis"))
    }

    @Test
    fun `CommentEntity handles tags with skin tone modifiers correctly`() {
        // Arrange & Act
        val skinToneTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Skin tone tags comment",
            tags = "👋🏻,👋🏼,👋🏽,👋🏾,👋🏿,skin,tone"
        )

        // Assert
        assertNotNull("Skin tone tags comment should be created", skinToneTagsComment)
        assertTrue("Tags should contain light skin tone", skinToneTagsComment.tags.contains("👋🏻"))
        assertTrue("Tags should contain medium-light skin tone", skinToneTagsComment.tags.contains("👋🏼"))
        assertTrue("Tags should contain medium skin tone", skinToneTagsComment.tags.contains("👋🏽"))
        assertTrue("Tags should contain medium-dark skin tone", skinToneTagsComment.tags.contains("👋🏾"))
        assertTrue("Tags should contain dark skin tone", skinToneTagsComment.tags.contains("👋🏿"))
        assertTrue("Tags should contain skin", skinToneTagsComment.tags.contains("skin"))
        assertTrue("Tags should contain tone", skinToneTagsComment.tags.contains("tone"))
    }

    @Test
    fun `CommentEntity handles tags with country flags correctly`() {
        // Arrange & Act
        val countryFlagsTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Country flags tags comment",
            tags = "🇺🇸,🇬🇧,🇫🇷,🇩🇪,🇯🇵,🇨🇳,🇷🇺,flags"
        )

        // Assert
        assertNotNull("Country flags tags comment should be created", countryFlagsTagsComment)
        assertTrue("Tags should contain US flag", countryFlagsTagsComment.tags.contains("🇺🇸"))
        assertTrue("Tags should contain UK flag", countryFlagsTagsComment.tags.contains("🇬🇧"))
        assertTrue("Tags should contain France flag", countryFlagsTagsComment.tags.contains("🇫🇷"))
        assertTrue("Tags should contain Germany flag", countryFlagsTagsComment.tags.contains("🇩🇪"))
        assertTrue("Tags should contain Japan flag", countryFlagsTagsComment.tags.contains("🇯🇵"))
        assertTrue("Tags should contain China flag", countryFlagsTagsComment.tags.contains("🇨🇳"))
        assertTrue("Tags should contain Russia flag", countryFlagsTagsComment.tags.contains("🇷🇺"))
        assertTrue("Tags should contain flags", countryFlagsTagsComment.tags.contains("flags"))
    }

    @Test
    fun `CommentEntity handles tags with astronomical symbols correctly`() {
        // Arrange & Act
        val astronomicalTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Astronomical tags comment",
            tags = "☀️,🌙,⭐,🌟,🌠,🌌,astronomical"
        )

        // Assert
        assertNotNull("Astronomical tags comment should be created", astronomicalTagsComment)
        assertTrue("Tags should contain sun", astronomicalTagsComment.tags.contains("☀️"))
        assertTrue("Tags should contain moon", astronomicalTagsComment.tags.contains("🌙"))
        assertTrue("Tags should contain star", astronomicalTagsComment.tags.contains("⭐"))
        assertTrue("Tags should contain glowing star", astronomicalTagsComment.tags.contains("🌟"))
        assertTrue("Tags should contain shooting star", astronomicalTagsComment.tags.contains("🌠"))
        assertTrue("Tags should contain galaxy", astronomicalTagsComment.tags.contains("🌌"))
        assertTrue("Tags should contain astronomical", astronomicalTagsComment.tags.contains("astronomical"))
    }

    @Test
    fun `CommentEntity handles tags with musical notation correctly`() {
        // Arrange & Act
        val musicalTagsComment = CommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Musical tags comment",
            tags = "♪,♫,♬,♭,♮,♯,musical"
        )

        // Assert
        assertNotNull("Musical tags comment should be created", musicalTagsComment)
        assertTrue("Tags should contain eighth note", musicalTagsComment.tags.contains("♪"))
        assertTrue("Tags should contain beamed notes", musicalTagsComment.tags.contains("♫"))
        assertTrue("Tags should contain beamed sixteenth notes", musicalTagsComment.tags.contains("♬"))
        assertTrue("Tags should contain flat", musicalTagsComment.tags.contains("♭"))
        assertTrue("Tags should contain natural", musicalTagsComment.tags.contains("♮"))
        assertTrue("Tags should contain sharp", musicalTagsComment.tags.contains("♯"))
        assertTrue("Tags should contain musical", musicalTagsComment.tags.contains("musical"))
    }

    @Test
    fun `CommentEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundaryComment = CommentEntity(
            id = 0L,
            roundId = 0L,
            text = "",
            tags = "",
            createdAt = 0L
        )

        // Assert
        assertNotNull("Boundary comment should be created", boundaryComment)
        assertEquals("Zero ID should be preserved", 0L, boundaryComment.id)
        assertEquals("Zero round ID should be preserved", 0L, boundaryComment.roundId)
        assertEquals("Empty text should be preserved", "", boundaryComment.text)
        assertEquals("Empty tags should be preserved", "", boundaryComment.tags)
        assertEquals("Zero createdAt should be preserved", 0L, boundaryComment.createdAt)
    }

    @Test
    fun `CommentEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundaryComment = CommentEntity(
            id = Long.MAX_VALUE,
            roundId = Long.MAX_VALUE,
            text = "A".repeat(1000),
            tags = List(100) { "tag$it" }.joinToString(","),
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximum boundary comment should be created", maxBoundaryComment)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundaryComment.id)
        assertEquals("Maximum round ID should be preserved", Long.MAX_VALUE, maxBoundaryComment.roundId)
        assertEquals("Maximum text length should be preserved", 1000, maxBoundaryComment.text.length)
        assertEquals("Maximum tags count should be preserved", 100, maxBoundaryComment.tags.split(",").size)
        assertEquals("Maximum createdAt should be preserved", Long.MAX_VALUE, maxBoundaryComment.createdAt)
    }

    @Test
    fun `CommentEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundaryComment = CommentEntity(
            id = Long.MIN_VALUE,
            roundId = Long.MIN_VALUE,
            text = "",
            tags = "",
            createdAt = Long.MIN_VALUE
        )

        // Assert
        assertNotNull("Minimum boundary comment should be created", minBoundaryComment)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundaryComment.id)
        assertEquals("Minimum round ID should be preserved", Long.MIN_VALUE, minBoundaryComment.roundId)
        assertEquals("Minimum createdAt should be preserved", Long.MIN_VALUE, minBoundaryComment.createdAt)
    }
}