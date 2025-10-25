package com.helldeck.content.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.GameOptions
import com.helldeck.content.util.SeededRng
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameEngineV2IntegrationTest {

    private lateinit var context: Context
    private lateinit var repo: ContentRepository
    private lateinit var engineV2: GameEngineV2

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = ContentRepository(context)
        engineV2 = ContentEngineProviderV2.get(context)
    }

    @Test
    fun `engineV2 next() with V2 template yields correct GameOptions`() = runBlocking {
        val request = GameEngineV2.Request(
            sessionId = "test_session",
            gameId = "MAJORITY_REPORT",
            players = listOf("Player1", "Player2", "Player3")
        )

        val result = engineV2.next(request)

        assert(result.filledCard.text.isNotEmpty())
        assert(result.options is GameOptions.AB)
    }
}