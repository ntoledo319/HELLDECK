package com.helldeck.data.entities

import com.helldeck.data.PlayerEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PlayerEntity validation and behavior
 */
class PlayerEntityTest {

    @Test
    fun `PlayerEntity with valid data creates successfully`() {
        // Arrange & Act
        val player = PlayerEntity(
            id = "test_player",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        // Assert
        assertNotNull("Player should be created", player)
        assertEquals("ID should match", "test_player", player.id)
        assertEquals("Name should match", "Test Player", player.name)
        assertEquals("Avatar should match", "😀", player.avatar)
        assertEquals("SessionPoints should match", 10, player.sessionPoints)
    }

    @Test
    fun `PlayerEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val player = PlayerEntity(
            id = "min_player",
            name = "A",
            avatar = "A",
            sessionPoints = 0
        )

        // Assert
        assertNotNull("Minimal player should be created", player)
        assertEquals("Minimal ID should match", "min_player", player.id)
        assertEquals("Minimal name should match", "A", player.name)
        assertEquals("Minimal avatar should match", "A", player.avatar)
        assertEquals("Minimal sessionPoints should match", 0, player.sessionPoints)
    }

    @Test
    fun `PlayerEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val player = PlayerEntity(
            id = "A".repeat(100),
            name = "B".repeat(100),
            avatar = "🚀",
            sessionPoints = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Maximal player should be created", player)
        assertEquals("Maximal ID should match", "A".repeat(100), player.id)
        assertEquals("Maximal name should match", "B".repeat(100), player.name)
        assertEquals("Maximal avatar should match", "🚀", player.avatar)
        assertEquals("Maximal sessionPoints should match", Int.MAX_VALUE, player.sessionPoints)
    }

    @Test
    fun `PlayerEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalPlayer = PlayerEntity(
            id = "original",
            name = "Original Name",
            avatar = "😀",
            sessionPoints = 5
        )

        // Act
        val copiedPlayer = originalPlayer.copy(
            name = "Copied Name",
            avatar = "🤠",
            sessionPoints = 15
        )

        // Assert
        assertNotNull("Copied player should not be null", copiedPlayer)
        assertEquals("ID should remain same", originalPlayer.id, copiedPlayer.id)
        assertEquals("Name should be updated", "Copied Name", copiedPlayer.name)
        assertEquals("Avatar should be updated", "🤠", copiedPlayer.avatar)
        assertEquals("SessionPoints should be updated", 15, copiedPlayer.sessionPoints)

        assertNotEquals("Original and copy should not be same object", originalPlayer, copiedPlayer)
    }

    @Test
    fun `PlayerEntity handles special characters correctly`() {
        // Arrange & Act
        val specialPlayer = PlayerEntity(
            id = "spécial_plâyér_ñ",
            name = "José María García 🚀",
            avatar = "🌟",
            sessionPoints = 25
        )

        // Assert
        assertNotNull("Special player should be created", specialPlayer)
        assertEquals("Special ID should be preserved", "spécial_plâyér_ñ", specialPlayer.id)
        assertEquals("Special name should be preserved", "José María García 🚀", specialPlayer.name)
        assertEquals("Special avatar should be preserved", "🌟", specialPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodePlayer = PlayerEntity(
            id = "ünícódé_plâyér",
            name = "Üsér wíth çömplëx çhâractërs",
            avatar = "🎭",
            sessionPoints = 50
        )

        // Assert
        assertNotNull("Unicode player should be created", unicodePlayer)
        assertEquals("Unicode ID should be preserved", "ünícódé_plâyér", unicodePlayer.id)
        assertEquals("Unicode name should be preserved", "Üsér wíth çömplëx çhâractërs", unicodePlayer.name)
        assertEquals("Unicode avatar should be preserved", "🎭", unicodePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptyPlayer = PlayerEntity(
            id = "",
            name = "",
            avatar = "",
            sessionPoints = 0
        )

        // Assert
        assertNotNull("Empty player should be created", emptyPlayer)
        assertEquals("Empty ID should be preserved", "", emptyPlayer.id)
        assertEquals("Empty name should be preserved", "", emptyPlayer.name)
        assertEquals("Empty avatar should be preserved", "", emptyPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespacePlayer = PlayerEntity(
            id = "   ",
            name = "   ",
            avatar = "   ",
            sessionPoints = 0
        )

        // Assert
        assertNotNull("Whitespace player should be created", whitespacePlayer)
        assertEquals("Whitespace ID should be preserved", "   ", whitespacePlayer.id)
        assertEquals("Whitespace name should be preserved", "   ", whitespacePlayer.name)
        assertEquals("Whitespace avatar should be preserved", "   ", whitespacePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundaryPlayer = PlayerEntity(
            id = "boundary_test",
            name = "Boundary Test Player",
            avatar = "🎯",
            sessionPoints = 0 // Minimum valid sessionPoints
        )

        // Assert
        assertNotNull("Boundary player should be created", boundaryPlayer)
        assertEquals("Zero sessionPoints should be preserved", 0, boundaryPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles very long name correctly`() {
        // Arrange & Act
        val longName = "A".repeat(1000) + " Very Long Player Name " + "B".repeat(1000)
        val longNamePlayer = PlayerEntity(
            id = "long_name_player",
            name = longName,
            avatar = "👑",
            sessionPoints = 100
        )

        // Assert
        assertNotNull("Long name player should be created", longNamePlayer)
        assertEquals("Long name should be preserved", longName, longNamePlayer.name)
        assertEquals("Long name length should match", 2027, longNamePlayer.name.length)
    }

    @Test
    fun `PlayerEntity handles very long ID correctly`() {
        // Arrange & Act
        val longId = "A".repeat(1000) + "_very_long_player_id_" + "B".repeat(1000)
        val longIdPlayer = PlayerEntity(
            id = longId,
            name = "Long ID Player",
            avatar = "🏆",
            sessionPoints = 200
        )

        // Assert
        assertNotNull("Long ID player should be created", longIdPlayer)
        assertEquals("Long ID should be preserved", longId, longIdPlayer.id)
        assertEquals("Long ID length should match", 2021, longIdPlayer.id.length)
    }

    @Test
    fun `PlayerEntity handles unicode avatars correctly`() {
        // Arrange & Act
        val unicodeAvatars = listOf("🚀", "🌟", "🎯", "🎲", "🎪", "🎨", "🎭", "🎪")
        val players = unicodeAvatars.mapIndexed { index, avatar ->
            PlayerEntity(
                id = "unicode_avatar_player_$index",
                name = "Player $index",
                avatar = avatar,
                sessionPoints = index * 10
            )
        }

        // Assert
        unicodeAvatars.forEachIndexed { index, expectedAvatar ->
            val player = players[index]
            assertNotNull("Unicode avatar player should be created", player)
            assertEquals("Unicode avatar should be preserved", expectedAvatar, player.avatar)
            assertEquals("SessionPoints should match", index * 10, player.sessionPoints)
        }
    }

    @Test
    fun `PlayerEntity handles negative session points correctly`() {
        // Arrange & Act
        val negativePointsPlayer = PlayerEntity(
            id = "negative_points_player",
            name = "Negative Points Player",
            avatar = "👎",
            sessionPoints = -50
        )

        // Assert
        assertNotNull("Negative points player should be created", negativePointsPlayer)
        assertEquals("Negative sessionPoints should be preserved", -50, negativePointsPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles zero session points correctly`() {
        // Arrange & Act
        val zeroPointsPlayer = PlayerEntity(
            id = "zero_points_player",
            name = "Zero Points Player",
            avatar = "😐",
            sessionPoints = 0
        )

        // Assert
        assertNotNull("Zero points player should be created", zeroPointsPlayer)
        assertEquals("Zero sessionPoints should be preserved", 0, zeroPointsPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles high session points correctly`() {
        // Arrange & Act
        val highPointsPlayer = PlayerEntity(
            id = "high_points_player",
            name = "High Points Player",
            avatar = "⭐",
            sessionPoints = 9999
        )

        // Assert
        assertNotNull("High points player should be created", highPointsPlayer)
        assertEquals("High sessionPoints should be preserved", 9999, highPointsPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles extremely high session points correctly`() {
        // Arrange & Act
        val extremePointsPlayer = PlayerEntity(
            id = "extreme_points_player",
            name = "Extreme Points Player",
            avatar = "💎",
            sessionPoints = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Extreme points player should be created", extremePointsPlayer)
        assertEquals("Extreme sessionPoints should be preserved", Int.MAX_VALUE, extremePointsPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles extremely low session points correctly`() {
        // Arrange & Act
        val extremeLowPointsPlayer = PlayerEntity(
            id = "extreme_low_points_player",
            name = "Extreme Low Points Player",
            avatar = "📉",
            sessionPoints = Int.MIN_VALUE
        )

        // Assert
        assertNotNull("Extreme low points player should be created", extremeLowPointsPlayer)
        assertEquals("Extreme low sessionPoints should be preserved", Int.MIN_VALUE, extremeLowPointsPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles emojis in names correctly`() {
        // Arrange & Act
        val emojiNamePlayer = PlayerEntity(
            id = "emoji_name_player",
            name = "Player 🚀 with 🌟 emojis ⭐ in name",
            avatar = "🎮",
            sessionPoints = 75
        )

        // Assert
        assertNotNull("Emoji name player should be created", emojiNamePlayer)
        assertTrue("Name should contain rocket emoji", emojiNamePlayer.name.contains("🚀"))
        assertTrue("Name should contain star emoji", emojiNamePlayer.name.contains("🌟"))
        assertTrue("Name should contain star emoji", emojiNamePlayer.name.contains("⭐"))
        assertEquals("Avatar should be preserved", "🎮", emojiNamePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles numbers in names correctly`() {
        // Arrange & Act
        val numbersNamePlayer = PlayerEntity(
            id = "numbers_name_player",
            name = "Player123 with 456.789 numbers",
            avatar = "🔢",
            sessionPoints = 123
        )

        // Assert
        assertNotNull("Numbers name player should be created", numbersNamePlayer)
        assertTrue("Name should contain numbers", numbersNamePlayer.name.contains("123"))
        assertTrue("Name should contain decimal", numbersNamePlayer.name.contains("456.789"))
        assertEquals("Avatar should be preserved", "🔢", numbersNamePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles special regex characters in names correctly`() {
        // Arrange & Act
        val regexCharsPlayer = PlayerEntity(
            id = "regex_chars_player",
            name = "Player ^$*+?{}[]\\|() with regex",
            avatar = "🔍",
            sessionPoints = 45
        )

        // Assert
        assertNotNull("Regex chars player should be created", regexCharsPlayer)
        assertTrue("Name should contain regex characters", regexCharsPlayer.name.contains("^$*+?{}[]\\|()"))
        assertEquals("Avatar should be preserved", "🔍", regexCharsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles quotes in names correctly`() {
        // Arrange & Act
        val quotesPlayer = PlayerEntity(
            id = "quotes_player",
            name = "Player \"with\" 'quotes' and `backticks`",
            avatar = "\"",
            sessionPoints = 67
        )

        // Assert
        assertNotNull("Quotes player should be created", quotesPlayer)
        assertTrue("Name should contain double quotes", quotesPlayer.name.contains("\"with\""))
        assertTrue("Name should contain single quotes", quotesPlayer.name.contains("'quotes'"))
        assertTrue("Name should contain backticks", quotesPlayer.name.contains("`backticks`"))
        assertEquals("Avatar should be preserved", "\"", quotesPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles newlines in names correctly`() {
        // Arrange & Act
        val newlinesPlayer = PlayerEntity(
            id = "newlines_player",
            name = "Player\nwith\r\nnewlines",
            avatar = "📝",
            sessionPoints = 89
        )

        // Assert
        assertNotNull("Newlines player should be created", newlinesPlayer)
        assertTrue("Name should contain newlines", newlinesPlayer.name.contains("\n"))
        assertTrue("Name should contain carriage returns", newlinesPlayer.name.contains("\r"))
        assertEquals("Avatar should be preserved", "📝", newlinesPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles tabs in names correctly`() {
        // Arrange & Act
        val tabsPlayer = PlayerEntity(
            id = "tabs_player",
            name = "Player\twith\ttabs",
            avatar = "📋",
            sessionPoints = 101
        )

        // Assert
        assertNotNull("Tabs player should be created", tabsPlayer)
        assertTrue("Name should contain tabs", tabsPlayer.name.contains("\t"))
        assertEquals("Avatar should be preserved", "📋", tabsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles HTML-like content in names correctly`() {
        // Arrange & Act
        val htmlLikePlayer = PlayerEntity(
            id = "html_like_player",
            name = "Player <with> <html> like & content",
            avatar = "<",
            sessionPoints = 112
        )

        // Assert
        assertNotNull("HTML-like player should be created", htmlLikePlayer)
        assertTrue("Name should contain angle brackets", htmlLikePlayer.name.contains("<with>"))
        assertTrue("Name should contain HTML tag", htmlLikePlayer.name.contains("<html>"))
        assertTrue("Name should contain HTML entity", htmlLikePlayer.name.contains("&"))
        assertEquals("Avatar should be preserved", "<", htmlLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles URLs in names correctly`() {
        // Arrange & Act
        val urlPlayer = PlayerEntity(
            id = "url_player",
            name = "Player https://example.com with ftp://test.org URLs",
            avatar = "🌐",
            sessionPoints = 131
        )

        // Assert
        assertNotNull("URL player should be created", urlPlayer)
        assertTrue("Name should contain HTTPS URL", urlPlayer.name.contains("https://example.com"))
        assertTrue("Name should contain FTP URL", urlPlayer.name.contains("ftp://test.org"))
        assertEquals("Avatar should be preserved", "🌐", urlPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles email addresses in names correctly`() {
        // Arrange & Act
        val emailPlayer = PlayerEntity(
            id = "email_player",
            name = "Player user@example.com and test.email+tag@domain.co.uk",
            avatar = "✉️",
            sessionPoints = 142
        )

        // Assert
        assertNotNull("Email player should be created", emailPlayer)
        assertTrue("Name should contain email", emailPlayer.name.contains("user@example.com"))
        assertTrue("Name should contain complex email", emailPlayer.name.contains("test.email+tag@domain.co.uk"))
        assertEquals("Avatar should be preserved", "✉️", emailPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles file paths in names correctly`() {
        // Arrange & Act
        val filePathPlayer = PlayerEntity(
            id = "filepath_player",
            name = "Player /usr/local/bin and C:\\Windows\\System32 paths",
            avatar = "📁",
            sessionPoints = 153
        )

        // Assert
        assertNotNull("File path player should be created", filePathPlayer)
        assertTrue("Name should contain Unix path", filePathPlayer.name.contains("/usr/local/bin"))
        assertTrue("Name should contain Windows path", filePathPlayer.name.contains("C:\\Windows\\System32"))
        assertEquals("Avatar should be preserved", "📁", filePathPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles JSON-like content in names correctly`() {
        // Arrange & Act
        val jsonLikePlayer = PlayerEntity(
            id = "json_like_player",
            name = "Player {\"name\": \"value\", \"points\": 123} JSON",
            avatar = "{",
            sessionPoints = 164
        )

        // Assert
        assertNotNull("JSON-like player should be created", jsonLikePlayer)
        assertTrue("Name should contain JSON object", jsonLikePlayer.name.contains("{\"name\": \"value\", \"points\": 123}"))
        assertEquals("Avatar should be preserved", "{", jsonLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles SQL-like content in names correctly`() {
        // Arrange & Act
        val sqlLikePlayer = PlayerEntity(
            id = "sql_like_player",
            name = "Player SELECT * FROM players WHERE name = 'test'",
            avatar = "🗃️",
            sessionPoints = 175
        )

        // Assert
        assertNotNull("SQL-like player should be created", sqlLikePlayer)
        assertTrue("Name should contain SQL query", sqlLikePlayer.name.contains("SELECT * FROM players WHERE name = 'test'"))
        assertEquals("Avatar should be preserved", "🗃️", sqlLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles programming code in names correctly`() {
        // Arrange & Act
        val codeLikePlayer = PlayerEntity(
            id = "code_like_player",
            name = "Player if (points > 100) { celebrate(); } code",
            avatar = "💻",
            sessionPoints = 186
        )

        // Assert
        assertNotNull("Code-like player should be created", codeLikePlayer)
        assertTrue("Name should contain if statement", codeLikePlayer.name.contains("if (points > 100)"))
        assertTrue("Name should contain braces", codeLikePlayer.name.contains("{ celebrate(); }"))
        assertEquals("Avatar should be preserved", "💻", codeLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles mathematical symbols in names correctly`() {
        // Arrange & Act
        val mathSymbolsPlayer = PlayerEntity(
            id = "math_symbols_player",
            name = "Player ∑∆π∞ with √∫∂ math symbols",
            avatar = "📐",
            sessionPoints = 197
        )

        // Assert
        assertNotNull("Math symbols player should be created", mathSymbolsPlayer)
        assertTrue("Name should contain sum symbol", mathSymbolsPlayer.name.contains("∑"))
        assertTrue("Name should contain delta symbol", mathSymbolsPlayer.name.contains("∆"))
        assertTrue("Name should contain pi symbol", mathSymbolsPlayer.name.contains("π"))
        assertTrue("Name should contain infinity symbol", mathSymbolsPlayer.name.contains("∞"))
        assertTrue("Name should contain square root symbol", mathSymbolsPlayer.name.contains("√"))
        assertTrue("Name should contain integral symbol", mathSymbolsPlayer.name.contains("∫"))
        assertTrue("Name should contain partial symbol", mathSymbolsPlayer.name.contains("∂"))
        assertEquals("Avatar should be preserved", "📐", mathSymbolsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles binary-like content in names correctly`() {
        // Arrange & Act
        val binaryLikePlayer = PlayerEntity(
            id = "binary_like_player",
            name = "Player 01010101 binary 11001100 content",
            avatar = "🔀",
            sessionPoints = 208
        )

        // Assert
        assertNotNull("Binary-like player should be created", binaryLikePlayer)
        assertTrue("Name should contain binary-like content", binaryLikePlayer.name.contains("01010101"))
        assertTrue("Name should contain more binary-like content", binaryLikePlayer.name.contains("11001100"))
        assertEquals("Avatar should be preserved", "🔀", binaryLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles right-to-left text in names correctly`() {
        // Arrange & Act
        val rtlPlayer = PlayerEntity(
            id = "rtl_player",
            name = "Player العربية ועברית RTL text",
            avatar = "🔄",
            sessionPoints = 219
        )

        // Assert
        assertNotNull("RTL player should be created", rtlPlayer)
        assertTrue("Name should contain Arabic text", rtlPlayer.name.contains("العربية"))
        assertTrue("Name should contain Hebrew text", rtlPlayer.name.contains("עברית"))
        assertEquals("Avatar should be preserved", "🔄", rtlPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles combining characters in names correctly`() {
        // Arrange & Act
        val combiningCharsPlayer = PlayerEntity(
            id = "combining_chars_player",
            name = "Player with áéíóú combining characters",
            avatar = "🔗",
            sessionPoints = 230
        )

        // Assert
        assertNotNull("Combining chars player should be created", combiningCharsPlayer)
        assertTrue("Name should contain combining characters", combiningCharsPlayer.name.contains("á"))
        assertTrue("Name should contain more combining characters", combiningCharsPlayer.name.contains("é"))
        assertEquals("Avatar should be preserved", "🔗", combiningCharsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles zero-width characters in names correctly`() {
        // Arrange & Act
        val zeroWidthPlayer = PlayerEntity(
            id = "zero_width_player",
            name = "Player\u200Bwith\u200Czero\u200Dwidth\u200Echaracters\u200F",
            avatar = "🚫",
            sessionPoints = 241
        )

        // Assert
        assertNotNull("Zero width player should be created", zeroWidthPlayer)
        assertTrue("Name should contain zero-width characters", zeroWidthPlayer.name.contains("\u200B"))
        assertEquals("Avatar should be preserved", "🚫", zeroWidthPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles invisible characters in names correctly`() {
        // Arrange & Act
        val invisibleCharsPlayer = PlayerEntity(
            id = "invisible_chars_player",
            name = "Player\u0000with\u0001invisible\u0002chars\u0003",
            avatar = "👻",
            sessionPoints = 252
        )

        // Assert
        assertNotNull("Invisible chars player should be created", invisibleCharsPlayer)
        assertTrue("Name should contain invisible characters", invisibleCharsPlayer.name.contains("\u0000"))
        assertEquals("Avatar should be preserved", "👻", invisibleCharsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles mixed content in names correctly`() {
        // Arrange & Act
        val mixedContentPlayer = PlayerEntity(
            id = "mixed_content_player",
            name = "Mixed números 123, emojis 🚀🌟, spécial çharácters, \"quotes\", and móre!",
            avatar = "🎨",
            sessionPoints = 263
        )

        // Assert
        assertNotNull("Mixed content player should be created", mixedContentPlayer)
        assertTrue("Name should contain numbers", mixedContentPlayer.name.contains("123"))
        assertTrue("Name should contain emojis", mixedContentPlayer.name.contains("🚀") && mixedContentPlayer.name.contains("🌟"))
        assertTrue("Name should contain special characters", mixedContentPlayer.name.contains("çharácters"))
        assertTrue("Name should contain quotes", mixedContentPlayer.name.contains("\"quotes\""))
        assertTrue("Name should contain more accents", mixedContentPlayer.name.contains("móre"))
        assertEquals("Avatar should be preserved", "🎨", mixedContentPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles extremely long names correctly`() {
        // Arrange & Act
        val extremelyLongName = "A".repeat(10000) + " Extremely Long Name " + "B".repeat(10000)
        val extremelyLongNamePlayer = PlayerEntity(
            id = "extremely_long_name_player",
            name = extremelyLongName,
            avatar = "📏",
            sessionPoints = 274
        )

        // Assert
        assertNotNull("Extremely long name player should be created", extremelyLongNamePlayer)
        assertEquals("Extremely long name should be preserved", extremelyLongName, extremelyLongNamePlayer.name)
        assertEquals("Extremely long name length should match", 40005, extremelyLongNamePlayer.name.length)
    }

    @Test
    fun `PlayerEntity handles extremely long IDs correctly`() {
        // Arrange & Act
        val extremelyLongId = "A".repeat(10000) + "_extremely_long_player_id_" + "B".repeat(10000)
        val extremelyLongIdPlayer = PlayerEntity(
            id = extremelyLongId,
            name = "Extremely Long ID Player",
            avatar = "🆔",
            sessionPoints = 285
        )

        // Assert
        assertNotNull("Extremely long ID player should be created", extremelyLongIdPlayer)
        assertEquals("Extremely long ID should be preserved", extremelyLongId, extremelyLongIdPlayer.id)
        assertEquals("Extremely long ID length should match", 40023, extremelyLongIdPlayer.id.length)
    }

    @Test
    fun `PlayerEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCasePlayer = PlayerEntity(
            id = "A".repeat(1000),
            name = "Edge case player with ünícódé, émojis 🚀🌟, ñúméros 123.456, \"quotes\", and {{{braces}}}",
            avatar = "🎭",
            sessionPoints = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Edge case player should be created", edgeCasePlayer)
        assertEquals("ID should be extremely long", 1000, edgeCasePlayer.id.length)
        assertEquals("Name should be extremely long", 108, edgeCasePlayer.name.length)
        assertTrue("Name should contain unicode", edgeCasePlayer.name.contains("ünícódé"))
        assertTrue("Name should contain emojis", edgeCasePlayer.name.contains("🚀") && edgeCasePlayer.name.contains("🌟"))
        assertTrue("Name should contain numbers", edgeCasePlayer.name.contains("123.456"))
        assertTrue("Name should contain quotes", edgeCasePlayer.name.contains("\"quotes\""))
        assertTrue("Name should contain braces", edgeCasePlayer.name.contains("{{{braces}}}"))
        assertEquals("Avatar should be preserved", "🎭", edgeCasePlayer.avatar)
        assertEquals("SessionPoints should be maximum", Int.MAX_VALUE, edgeCasePlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity equality works correctly`() {
        // Arrange
        val player1 = PlayerEntity(
            id = "test_id",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        val player2 = PlayerEntity(
            id = "test_id",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        val player3 = PlayerEntity(
            id = "different_id",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        // Assert
        assertEquals("Identical players should be equal", player1, player2)
        assertNotEquals("Different players should not be equal", player1, player3)
        assertEquals("Hash codes should be equal for identical players", player1.hashCode(), player2.hashCode())
        assertTrue("Hash codes should be different for different players", player1.hashCode() != player3.hashCode())
    }

    @Test
    fun `PlayerEntity toString contains relevant information`() {
        // Arrange
        val player = PlayerEntity(
            id = "test_id",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        // Act
        val toString = player.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("test_id"))
        assertTrue("toString should contain name", toString.contains("Test Player"))
        assertTrue("toString should contain avatar", toString.contains("😀"))
        assertTrue("toString should contain sessionPoints", toString.contains("sessionPoints=10"))
    }

    @Test
    fun `PlayerEntity handles names with only numbers correctly`() {
        // Arrange & Act
        val numbersOnlyPlayer = PlayerEntity(
            id = "numbers_only_player",
            name = "123456789",
            avatar = "🔢",
            sessionPoints = 296
        )

        // Assert
        assertNotNull("Numbers only player should be created", numbersOnlyPlayer)
        assertEquals("Numbers only name should be preserved", "123456789", numbersOnlyPlayer.name)
        assertEquals("Avatar should be preserved", "🔢", numbersOnlyPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with only special characters correctly`() {
        // Arrange & Act
        val specialCharsOnlyPlayer = PlayerEntity(
            id = "special_chars_only_player",
            name = "!@#$%^&*()",
            avatar = "🎭",
            sessionPoints = 307
        )

        // Assert
        assertNotNull("Special chars only player should be created", specialCharsOnlyPlayer)
        assertEquals("Special chars only name should be preserved", "!@#$%^&*()", specialCharsOnlyPlayer.name)
        assertEquals("Avatar should be preserved", "🎭", specialCharsOnlyPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with only emojis correctly`() {
        // Arrange & Act
        val emojisOnlyPlayer = PlayerEntity(
            id = "emojis_only_player",
            name = "🚀🌟🎯🎲🎪🎨🎭🎪",
            avatar = "⭐",
            sessionPoints = 318
        )

        // Assert
        assertNotNull("Emojis only player should be created", emojisOnlyPlayer)
        assertTrue("Name should contain rocket emoji", emojisOnlyPlayer.name.contains("🚀"))
        assertTrue("Name should contain star emoji", emojisOnlyPlayer.name.contains("🌟"))
        assertTrue("Name should contain dart emoji", emojisOnlyPlayer.name.contains("🎯"))
        assertTrue("Name should contain dice emoji", emojisOnlyPlayer.name.contains("🎲"))
        assertEquals("Avatar should be preserved", "⭐", emojisOnlyPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles avatars with complex emojis correctly`() {
        // Arrange & Act
        val complexEmojiAvatars = listOf("🚀", "🌟", "🎯", "🎲", "🎪", "🎨", "🎭", "🎪", "👨‍💻", "👩‍🎨", "🚗", "🏠")
        val players = complexEmojiAvatars.mapIndexed { index, avatar ->
            PlayerEntity(
                id = "complex_avatar_player_$index",
                name = "Player $index",
                avatar = avatar,
                sessionPoints = index * 10
            )
        }

        // Assert
        complexEmojiAvatars.forEachIndexed { index, expectedAvatar ->
            val player = players[index]
            assertNotNull("Complex avatar player should be created", player)
            assertEquals("Complex avatar should be preserved", expectedAvatar, player.avatar)
            assertEquals("SessionPoints should match", index * 10, player.sessionPoints)
        }
    }

    @Test
    fun `PlayerEntity handles names with mixed language content correctly`() {
        // Arrange & Act
        val mixedLanguagePlayer = PlayerEntity(
            id = "mixed_language_player",
            name = "Player with English, Español, Français, and 日本語 content",
            avatar = "🌍",
            sessionPoints = 329
        )

        // Assert
        assertNotNull("Mixed language player should be created", mixedLanguagePlayer)
        assertTrue("Name should contain English", mixedLanguagePlayer.name.contains("English"))
        assertTrue("Name should contain Spanish", mixedLanguagePlayer.name.contains("Español"))
        assertTrue("Name should contain French", mixedLanguagePlayer.name.contains("Français"))
        assertTrue("Name should contain Japanese", mixedLanguagePlayer.name.contains("日本語"))
        assertEquals("Avatar should be preserved", "🌍", mixedLanguagePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with programming language keywords correctly`() {
        // Arrange & Act
        val programmingKeywordsPlayer = PlayerEntity(
            id = "programming_keywords_player",
            name = "Player with if else for while function class object keywords",
            avatar = "💻",
            sessionPoints = 340
        )

        // Assert
        assertNotNull("Programming keywords player should be created", programmingKeywordsPlayer)
        assertTrue("Name should contain if", programmingKeywordsPlayer.name.contains("if"))
        assertTrue("Name should contain else", programmingKeywordsPlayer.name.contains("else"))
        assertTrue("Name should contain for", programmingKeywordsPlayer.name.contains("for"))
        assertTrue("Name should contain while", programmingKeywordsPlayer.name.contains("while"))
        assertTrue("Name should contain function", programmingKeywordsPlayer.name.contains("function"))
        assertTrue("Name should contain class", programmingKeywordsPlayer.name.contains("class"))
        assertTrue("Name should contain object", programmingKeywordsPlayer.name.contains("object"))
        assertEquals("Avatar should be preserved", "💻", programmingKeywordsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with markdown-like content correctly`() {
        // Arrange & Act
        val markdownLikePlayer = PlayerEntity(
            id = "markdown_like_player",
            name = "Player with # header *bold* **strong** `code` content",
            avatar = "#️⃣",
            sessionPoints = 351
        )

        // Assert
        assertNotNull("Markdown-like player should be created", markdownLikePlayer)
        assertTrue("Name should contain header", markdownLikePlayer.name.contains("# header"))
        assertTrue("Name should contain bold", markdownLikePlayer.name.contains("*bold*"))
        assertTrue("Name should contain strong", markdownLikePlayer.name.contains("**strong**"))
        assertTrue("Name should contain code", markdownLikePlayer.name.contains("`code`"))
        assertEquals("Avatar should be preserved", "#️⃣", markdownLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with LaTeX-like content correctly`() {
        // Arrange & Act
        val latexLikePlayer = PlayerEntity(
            id = "latex_like_player",
            name = "Player with \\alpha \\beta \\gamma \\delta math content",
            avatar = "📚",
            sessionPoints = 362
        )

        // Assert
        assertNotNull("LaTeX-like player should be created", latexLikePlayer)
        assertTrue("Name should contain alpha", latexLikePlayer.name.contains("\\alpha"))
        assertTrue("Name should contain beta", latexLikePlayer.name.contains("\\beta"))
        assertTrue("Name should contain gamma", latexLikePlayer.name.contains("\\gamma"))
        assertTrue("Name should contain delta", latexLikePlayer.name.contains("\\delta"))
        assertEquals("Avatar should be preserved", "📚", latexLikePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with extremely nested content correctly`() {
        // Arrange & Act
        val nestedContentPlayer = PlayerEntity(
            id = "nested_content_player",
            name = "Player (((with))) {{{deep}}} {{{{{nested}}}}} content",
            avatar = "🪆",
            sessionPoints = 373
        )

        // Assert
        assertNotNull("Nested content player should be created", nestedContentPlayer)
        assertTrue("Name should contain simple nesting", nestedContentPlayer.name.contains("(((with)))"))
        assertTrue("Name should contain medium nesting", nestedContentPlayer.name.contains("{{{deep}}}"))
        assertTrue("Name should contain deep nesting", nestedContentPlayer.name.contains("{{{{{nested}}}}}"))
        assertEquals("Avatar should be preserved", "🪆", nestedContentPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with extremely long avatar correctly`() {
        // Arrange & Act
        val longAvatar = "🚀".repeat(1000)
        val longAvatarPlayer = PlayerEntity(
            id = "long_avatar_player",
            name = "Long Avatar Player",
            avatar = longAvatar,
            sessionPoints = 384
        )

        // Assert
        assertNotNull("Long avatar player should be created", longAvatarPlayer)
        assertEquals("Long avatar should be preserved", longAvatar, longAvatarPlayer.avatar)
        assertEquals("Long avatar length should match", 1000, longAvatarPlayer.avatar.length)
    }

    @Test
    fun `PlayerEntity handles names with all possible edge cases correctly`() {
        // Arrange & Act
        val allEdgeCasesPlayer = PlayerEntity(
            id = "A".repeat(1000),
            name = "All edge cases: ünícódé 🚀🌟, ñúméros 123.456, \"'quotes'\", {{{nested}}}, \\commands, <tags>, and móre!",
            avatar = "🎭",
            sessionPoints = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("All edge cases player should be created", allEdgeCasesPlayer)
        assertEquals("ID should be extremely long", 1000, allEdgeCasesPlayer.id.length)
        assertEquals("Name should be extremely long", 124, allEdgeCasesPlayer.name.length)
        assertTrue("Name should contain unicode", allEdgeCasesPlayer.name.contains("ünícódé"))
        assertTrue("Name should contain emojis", allEdgeCasesPlayer.name.contains("🚀") && allEdgeCasesPlayer.name.contains("🌟"))
        assertTrue("Name should contain numbers", allEdgeCasesPlayer.name.contains("123.456"))
        assertTrue("Name should contain quotes", allEdgeCasesPlayer.name.contains("\"'quotes'\""))
        assertTrue("Name should contain nested braces", allEdgeCasesPlayer.name.contains("{{{nested}}}"))
        assertTrue("Name should contain commands", allEdgeCasesPlayer.name.contains("\\commands"))
        assertTrue("Name should contain tags", allEdgeCasesPlayer.name.contains("<tags>"))
        assertTrue("Name should contain more accents", allEdgeCasesPlayer.name.contains("móre"))
        assertEquals("Avatar should be preserved", "🎭", allEdgeCasesPlayer.avatar)
        assertEquals("SessionPoints should be maximum", Int.MAX_VALUE, allEdgeCasesPlayer.sessionPoints)
    }

    @Test
    fun `PlayerEntity handles names with zero-width joiner emojis correctly`() {
        // Arrange & Act
        val zwjEmojiPlayer = PlayerEntity(
            id = "zwj_emoji_player",
            name = "Player with 👨‍💻👩‍🎨👨‍🚀 zero-width joiner emojis",
            avatar = "👨‍💻",
            sessionPoints = 395
        )

        // Assert
        assertNotNull("ZWJ emoji player should be created", zwjEmojiPlayer)
        assertTrue("Name should contain programmer emoji", zwjEmojiPlayer.name.contains("👨‍💻"))
        assertTrue("Name should contain artist emoji", zwjEmojiPlayer.name.contains("👩‍🎨"))
        assertTrue("Name should contain astronaut emoji", zwjEmojiPlayer.name.contains("👨‍🚀"))
        assertEquals("Avatar should be preserved", "👨‍💻", zwjEmojiPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with skin tone modifiers correctly`() {
        // Arrange & Act
        val skinTonePlayer = PlayerEntity(
            id = "skin_tone_player",
            name = "Player with 👋🏻👋🏼👋🏽👋🏾👋🏿 skin tone modifiers",
            avatar = "👋",
            sessionPoints = 406
        )

        // Assert
        assertNotNull("Skin tone player should be created", skinTonePlayer)
        assertTrue("Name should contain light skin tone", skinTonePlayer.name.contains("👋🏻"))
        assertTrue("Name should contain medium-light skin tone", skinTonePlayer.name.contains("👋🏼"))
        assertTrue("Name should contain medium skin tone", skinTonePlayer.name.contains("👋🏽"))
        assertTrue("Name should contain medium-dark skin tone", skinTonePlayer.name.contains("👋🏾"))
        assertTrue("Name should contain dark skin tone", skinTonePlayer.name.contains("👋🏿"))
        assertEquals("Avatar should be preserved", "👋", skinTonePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with country flags correctly`() {
        // Arrange & Act
        val countryFlagsPlayer = PlayerEntity(
            id = "country_flags_player",
            name = "Player with 🇺🇸🇬🇧🇫🇷🇩🇪🇯🇵🇨🇳🇷🇺 country flags",
            avatar = "🇺🇳",
            sessionPoints = 417
        )

        // Assert
        assertNotNull("Country flags player should be created", countryFlagsPlayer)
        assertTrue("Name should contain US flag", countryFlagsPlayer.name.contains("🇺🇸"))
        assertTrue("Name should contain UK flag", countryFlagsPlayer.name.contains("🇬🇧"))
        assertTrue("Name should contain France flag", countryFlagsPlayer.name.contains("🇫🇷"))
        assertTrue("Name should contain Germany flag", countryFlagsPlayer.name.contains("🇩🇪"))
        assertTrue("Name should contain Japan flag", countryFlagsPlayer.name.contains("🇯🇵"))
        assertTrue("Name should contain China flag", countryFlagsPlayer.name.contains("🇨🇳"))
        assertTrue("Name should contain Russia flag", countryFlagsPlayer.name.contains("🇷🇺"))
        assertEquals("Avatar should be preserved", "🇺🇳", countryFlagsPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with astronomical symbols correctly`() {
        // Arrange & Act
        val astronomicalPlayer = PlayerEntity(
            id = "astronomical_player",
            name = "Player with ☀️🌙⭐🌟🌠🌌 astronomical symbols",
            avatar = "🌟",
            sessionPoints = 428
        )

        // Assert
        assertNotNull("Astronomical player should be created", astronomicalPlayer)
        assertTrue("Name should contain sun", astronomicalPlayer.name.contains("☀️"))
        assertTrue("Name should contain moon", astronomicalPlayer.name.contains("🌙"))
        assertTrue("Name should contain star", astronomicalPlayer.name.contains("⭐"))
        assertTrue("Name should contain glowing star", astronomicalPlayer.name.contains("🌟"))
        assertTrue("Name should contain shooting star", astronomicalPlayer.name.contains("🌠"))
        assertTrue("Name should contain galaxy", astronomicalPlayer.name.contains("🌌"))
        assertEquals("Avatar should be preserved", "🌟", astronomicalPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with musical notation correctly`() {
        // Arrange & Act
        val musicalPlayer = PlayerEntity(
            id = "musical_player",
            name = "Player with ♪♫♬♭♮♯ musical notation",
            avatar = "🎵",
            sessionPoints = 439
        )

        // Assert
        assertNotNull("Musical player should be created", musicalPlayer)
        assertTrue("Name should contain eighth note", musicalPlayer.name.contains("♪"))
        assertTrue("Name should contain beamed notes", musicalPlayer.name.contains("♫"))
        assertTrue("Name should contain beamed sixteenth notes", musicalPlayer.name.contains("♬"))
        assertTrue("Name should contain flat", musicalPlayer.name.contains("♭"))
        assertTrue("Name should contain natural", musicalPlayer.name.contains("♮"))
        assertTrue("Name should contain sharp", musicalPlayer.name.contains("♯"))
        assertEquals("Avatar should be preserved", "🎵", musicalPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with chess symbols correctly`() {
        // Arrange & Act
        val chessPlayer = PlayerEntity(
            id = "chess_player",
            name = "Player with ♔♕♖♗♘♙♚♛♜♝♞♟ chess symbols",
            avatar = "♟",
            sessionPoints = 450
        )

        // Assert
        assertNotNull("Chess player should be created", chessPlayer)
        assertTrue("Name should contain white king", chessPlayer.name.contains("♔"))
        assertTrue("Name should contain white queen", chessPlayer.name.contains("♕"))
        assertTrue("Name should contain white rook", chessPlayer.name.contains("♖"))
        assertTrue("Name should contain white bishop", chessPlayer.name.contains("♗"))
        assertTrue("Name should contain white knight", chessPlayer.name.contains("♘"))
        assertTrue("Name should contain white pawn", chessPlayer.name.contains("♙"))
        assertTrue("Name should contain black king", chessPlayer.name.contains("♚"))
        assertTrue("Name should contain black queen", chessPlayer.name.contains("♛"))
        assertTrue("Name should contain black rook", chessPlayer.name.contains("♜"))
        assertTrue("Name should contain black bishop", chessPlayer.name.contains("♝"))
        assertTrue("Name should contain black knight", chessPlayer.name.contains("♞"))
        assertTrue("Name should contain black pawn", chessPlayer.name.contains("♟"))
        assertEquals("Avatar should be preserved", "♟", chessPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with card symbols correctly`() {
        // Arrange & Act
        val cardPlayer = PlayerEntity(
            id = "card_player",
            name = "Player with ♠♥♦♣🃏 card symbols",
            avatar = "🃏",
            sessionPoints = 461
        )

        // Assert
        assertNotNull("Card player should be created", cardPlayer)
        assertTrue("Name should contain spade", cardPlayer.name.contains("♠"))
        assertTrue("Name should contain heart", cardPlayer.name.contains("♥"))
        assertTrue("Name should contain diamond", cardPlayer.name.contains("♦"))
        assertTrue("Name should contain club", cardPlayer.name.contains("♣"))
        assertTrue("Name should contain joker", cardPlayer.name.contains("🃏"))
        assertEquals("Avatar should be preserved", "🃏", cardPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with dice and game symbols correctly`() {
        // Arrange & Act
        val gamePlayer = PlayerEntity(
            id = "game_player",
            name = "Player with 🎲🎯🎮🎰🎳 dice and game symbols",
            avatar = "🎲",
            sessionPoints = 472
        )

        // Assert
        assertNotNull("Game player should be created", gamePlayer)
        assertTrue("Name should contain dice", gamePlayer.name.contains("🎲"))
        assertTrue("Name should contain dart", gamePlayer.name.contains("🎯"))
        assertTrue("Name should contain video game", gamePlayer.name.contains("🎮"))
        assertTrue("Name should contain slot machine", gamePlayer.name.contains("🎰"))
        assertTrue("Name should contain bowling", gamePlayer.name.contains("🎳"))
        assertEquals("Avatar should be preserved", "🎲", gamePlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with weather symbols correctly`() {
        // Arrange & Act
        val weatherPlayer = PlayerEntity(
            id = "weather_player",
            name = "Player with ☀️🌤️⛅🌦️🌧️⛈️❄️ weather symbols",
            avatar = "☀️",
            sessionPoints = 483
        )

        // Assert
        assertNotNull("Weather player should be created", weatherPlayer)
        assertTrue("Name should contain sun", weatherPlayer.name.contains("☀️"))
        assertTrue("Name should contain partly cloudy", weatherPlayer.name.contains("🌤️"))
        assertTrue("Name should contain cloudy", weatherPlayer.name.contains("⛅"))
        assertTrue("Name should contain rainy", weatherPlayer.name.contains("🌦️"))
        assertTrue("Name should contain rain", weatherPlayer.name.contains("🌧️"))
        assertTrue("Name should contain thunderstorm", weatherPlayer.name.contains("⛈️"))
        assertTrue("Name should contain snow", weatherPlayer.name.contains("❄️"))
        assertEquals("Avatar should be preserved", "☀️", weatherPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with food and drink symbols correctly`() {
        // Arrange & Act
        val foodPlayer = PlayerEntity(
            id = "food_player",
            name = "Player with 🍕🍔🍟🌭🍿🧂🍽️ food and drink symbols",
            avatar = "🍕",
            sessionPoints = 494
        )

        // Assert
        assertNotNull("Food player should be created", foodPlayer)
        assertTrue("Name should contain pizza", foodPlayer.name.contains("🍕"))
        assertTrue("Name should contain burger", foodPlayer.name.contains("🍔"))
        assertTrue("Name should contain fries", foodPlayer.name.contains("🍟"))
        assertTrue("Name should contain hot dog", foodPlayer.name.contains("🌭"))
        assertTrue("Name should contain popcorn", foodPlayer.name.contains("🍿"))
        assertTrue("Name should contain salt", foodPlayer.name.contains("🧂"))
        assertTrue("Name should contain plate", foodPlayer.name.contains("🍽️"))
        assertEquals("Avatar should be preserved", "🍕", foodPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with activity symbols correctly`() {
        // Arrange & Act
        val activityPlayer = PlayerEntity(
            id = "activity_player",
            name = "Player with ⚽🏀🏈⚾🎾🏐🏉🎱 activity symbols",
            avatar = "⚽",
            sessionPoints = 505
        )

        // Assert
        assertNotNull("Activity player should be created", activityPlayer)
        assertTrue("Name should contain soccer ball", activityPlayer.name.contains("⚽"))
        assertTrue("Name should contain basketball", activityPlayer.name.contains("🏀"))
        assertTrue("Name should contain football", activityPlayer.name.contains("🏈"))
        assertTrue("Name should contain baseball", activityPlayer.name.contains("⚾"))
        assertTrue("Name should contain tennis ball", activityPlayer.name.contains("🎾"))
        assertTrue("Name should contain volleyball", activityPlayer.name.contains("🏐"))
        assertTrue("Name should contain rugby ball", activityPlayer.name.contains("🏉"))
        assertTrue("Name should contain billiard ball", activityPlayer.name.contains("🎱"))
        assertEquals("Avatar should be preserved", "⚽", activityPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with travel and place symbols correctly`() {
        // Arrange & Act
        val travelPlayer = PlayerEntity(
            id = "travel_player",
            name = "Player with 🚗✈️🚲⛵🏠🏭🏢🏣 travel and place symbols",
            avatar = "✈️",
            sessionPoints = 516
        )

        // Assert
        assertNotNull("Travel player should be created", travelPlayer)
        assertTrue("Name should contain car", travelPlayer.name.contains("🚗"))
        assertTrue("Name should contain airplane", travelPlayer.name.contains("✈️"))
        assertTrue("Name should contain bicycle", travelPlayer.name.contains("🚲"))
        assertTrue("Name should contain boat", travelPlayer.name.contains("⛵"))
        assertTrue("Name should contain house", travelPlayer.name.contains("🏠"))
        assertTrue("Name should contain factory", travelPlayer.name.contains("🏭"))
        assertTrue("Name should contain office building", travelPlayer.name.contains("🏢"))
        assertTrue("Name should contain post office", travelPlayer.name.contains("🏣"))
        assertEquals("Avatar should be preserved", "✈️", travelPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with symbol combinations correctly`() {
        // Arrange & Act
        val symbolComboPlayer = PlayerEntity(
            id = "symbol_combo_player",
            name = "Player with 👨‍💻👩‍🎨🚀🌟⚽🏀🎲🎯 complex symbol combinations",
            avatar = "🎭",
            sessionPoints = 527
        )

        // Assert
        assertNotNull("Symbol combo player should be created", symbolComboPlayer)
        assertTrue("Name should contain programmer", symbolComboPlayer.name.contains("👨‍💻"))
        assertTrue("Name should contain artist", symbolComboPlayer.name.contains("👩‍🎨"))
        assertTrue("Name should contain rocket", symbolComboPlayer.name.contains("🚀"))
        assertTrue("Name should contain star", symbolComboPlayer.name.contains("🌟"))
        assertTrue("Name should contain soccer", symbolComboPlayer.name.contains("⚽"))
        assertTrue("Name should contain basketball", symbolComboPlayer.name.contains("🏀"))
        assertTrue("Name should contain dice", symbolComboPlayer.name.contains("🎲"))
        assertTrue("Name should contain dart", symbolComboPlayer.name.contains("🎯"))
        assertEquals("Avatar should be preserved", "🎭", symbolComboPlayer.avatar)
    }

    @Test
    fun `PlayerEntity handles names with extremely complex content correctly`() {
        // Arrange & Act
        val extremelyComplexPlayer = PlayerEntity(
            id = "A".repeat(1000),
            name = "Extremely complex: ünícódé 🚀🌟, ñúméros 123.456, \"'quotes'\", {{{nested}}}, \\commands, <tags>, ∑∆π∞, ♔♕♖♗♘♙, and móre!",
            avatar = "🎭",
            sessionPoints = Int.MAX_VALUE
        )

        // Assert
        assertNotNull("Extremely complex player should be created", extremelyComplexPlayer)
        assertEquals("ID should be extremely long", 1000, extremelyComplexPlayer.id.length)
        assertEquals("Name should be extremely long", 142, extremelyComplexPlayer.name.length)
        assertTrue("Name should contain unicode", extremelyComplexPlayer.name.contains("ünícódé"))
        assertTrue("Name should contain emojis", extremelyComplexPlayer.name.contains("🚀") && extremelyComplexPlayer.name.contains("🌟"))
        assertTrue("Name should contain numbers", extremelyComplexPlayer.name.contains("123.456"))
        assertTrue("Name should contain quotes", extremelyComplexPlayer.name.contains("\"'quotes'\""))
        assertTrue("Name should contain nested braces", extremelyComplexPlayer.name.contains("{{{nested}}}"))
        assertTrue("Name should contain commands", extremelyComplexPlayer.name.contains("\\commands"))
        assertTrue("Name should contain tags", extremelyComplexPlayer.name.contains("<tags>"))
        assertTrue("Name should contain math symbols", extremelyComplexPlayer.name.contains("∑∆π∞"))
        assertTrue("Name should contain chess symbols", extremelyComplexPlayer.name.contains("♔♕♖♗♘♙"))
        assertTrue("Name should contain more accents", extremelyComplexPlayer.name.contains("móre"))
        assertEquals("Avatar should be preserved", "🎭", extremelyComplexPlayer.avatar)
        assertEquals("SessionPoints should be maximum", Int.MAX_VALUE, extremelyComplexPlayer.sessionPoints)
    }
}