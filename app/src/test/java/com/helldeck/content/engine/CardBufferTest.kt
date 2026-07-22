package com.helldeck.content.engine

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.InteractionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CardBufferTest {

    @Test
    fun firstRequestGeneratesWithoutSeparateStart() = runBlocking {
        val engine = mockk<GameEngine>()
        val request = requestFor("ROAST_CONSENSUS")
        val expected = resultFor("ROAST_CONSENSUS")
        coEvery { engine.next(request) } returns expected
        val buffer = CardBuffer(engine, bufferSize = 0, scope = this)

        try {
            assertSame(expected, buffer.getNext(request))
            coVerify(exactly = 1) { engine.next(request) }
        } finally {
            buffer.stopAndAwait()
        }
    }

    @Test
    fun changedRequestCannotReturnAStaleGame() = runBlocking {
        val engine = mockk<GameEngine>()
        coEvery { engine.next(any()) } answers {
            resultFor(firstArg<GameEngine.Request>().gameId.orEmpty())
        }
        val buffer = CardBuffer(engine, bufferSize = 0, scope = this)
        val firstRequest = requestFor("ROAST_CONSENSUS")
        val secondRequest = requestFor("POISON_PITCH")

        try {
            assertEquals("ROAST_CONSENSUS", buffer.getNext(firstRequest).filledCard.game)
            assertEquals("POISON_PITCH", buffer.getNext(secondRequest).filledCard.game)
            coVerify(exactly = 1) { engine.next(firstRequest) }
            coVerify(exactly = 1) { engine.next(secondRequest) }
        } finally {
            buffer.stopAndAwait()
        }
    }

    @Test
    fun changedRequestDiscardsAPrefetchedCard() = runBlocking {
        val engine = mockk<GameEngine>()
        coEvery { engine.next(any()) } answers {
            resultFor(firstArg<GameEngine.Request>().gameId.orEmpty())
        }
        val buffer = CardBuffer(engine, bufferSize = 1, scope = this)
        val firstRequest = requestFor("ROAST_CONSENSUS")
        val secondRequest = requestFor("POISON_PITCH")

        try {
            buffer.start(firstRequest)
            withTimeout(5_000L) {
                while (buffer.getStats().bufferSize < 1) delay(10)
            }

            assertEquals("POISON_PITCH", buffer.getNext(secondRequest).filledCard.game)
        } finally {
            buffer.stopAndAwait()
        }
    }

    @Test
    fun supersededInFlightRequestIsCancelled() = runBlocking {
        val engine = mockk<GameEngine>()
        val firstRequest = requestFor("ROAST_CONSENSUS")
        val secondRequest = requestFor("POISON_PITCH")
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        coEvery { engine.next(firstRequest) } coAnswers {
            firstStarted.complete(Unit)
            releaseFirst.await()
            resultFor("ROAST_CONSENSUS")
        }
        coEvery { engine.next(secondRequest) } returns resultFor("POISON_PITCH")
        val buffer = CardBuffer(engine, bufferSize = 0, scope = this)

        try {
            val staleResult = async(Dispatchers.Default) { buffer.getNext(firstRequest) }
            firstStarted.await()
            launch { releaseFirst.complete(Unit) }

            val currentResult = buffer.getNext(secondRequest)
            val staleFailure = runCatching { staleResult.await() }.exceptionOrNull()

            assertTrue(staleFailure is CancellationException)
            assertEquals("POISON_PITCH", currentResult.filledCard.game)
        } finally {
            releaseFirst.complete(Unit)
            buffer.stopAndAwait()
        }
    }

    private fun requestFor(gameId: String) = GameEngine.Request(
        sessionId = "buffer-regression",
        gameId = gameId,
        players = listOf("Seat 1", "Seat 2", "Seat 3"),
    )

    private fun resultFor(gameId: String) = GameEngine.Result(
        filledCard = FilledCard(
            id = "card-$gameId",
            game = gameId,
            text = "Test card for $gameId",
            family = "test",
            spice = 1,
            locality = 1,
        ),
        options = GameOptions.None,
        timer = 30,
        interactionType = InteractionType.NONE,
    )
}
