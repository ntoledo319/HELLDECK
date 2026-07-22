package com.helldeck.content.engine

import com.helldeck.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pre-generation buffer for instant card delivery.
 *
 * Maintains a buffer of 3 pre-generated cards to eliminate perceived latency.
 * While the user is engaged with the current card, the buffer refills in the background.
 *
 * This transforms the user experience from:
 *   Card shown → Vote → **WAIT 1-2s** → Next card
 *
 * To:
 *   Card shown → Vote → **INSTANT** → Next card
 *
 * Impact: Massive improvement in perceived performance and game flow.
 */
class CardBuffer(
    private val engine: GameEngine,
    private val bufferSize: Int = 3,
    private val scope: CoroutineScope,
) {
    private val buffer = ArrayDeque<GameEngine.Result>(bufferSize)
    private val stateLock = Any()
    private val generationMutex = Mutex()
    private var bufferingJob: Job? = null
    private var currentRequest: GameEngine.Request? = null
    private var requestGeneration = 0L
    private var running = false
    private val generationCount = AtomicInteger(0)
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)

    /**
     * Starts buffering cards for the given request parameters.
     * Call this when starting a new game or session to pre-generate cards.
     */
    fun start(request: GameEngine.Request) {
        Logger.d("CardBuffer: Starting buffer for game=${request.gameId}")
        val generation = prepareRequest(request, forceRestart = true)
        ensureBuffering(request, generation)
    }

    /**
     * Gets the next card from the buffer.
     * Returns instantly if buffer has cards, otherwise generates on-demand.
     *
     * The request is supplied here so a first-round call cannot observe an initialized but
     * unstarted buffer, and a game/settings change cannot consume a stale card.
     *
     * @return The next card, either from buffer (instant) or freshly generated (fallback).
     */
    suspend fun getNext(request: GameEngine.Request): GameEngine.Result {
        val generation = prepareRequest(request, forceRestart = false)
        val fromBuffer = takeBufferedCard(request, generation)

        return if (fromBuffer != null) {
            cacheHits.incrementAndGet()
            Logger.d("CardBuffer: Cache HIT (${cacheHits.get()} hits, ${cacheMisses.get()} misses)")
            ensureBuffering(request, generation)
            fromBuffer
        } else {
            generateOnDemand(request, generation)
        }
    }

    /**
     * Updates the request parameters for future card generation.
     * Useful when game settings change mid-session (e.g., spice level adjusted).
     */
    fun updateRequest(request: GameEngine.Request) {
        Logger.d("CardBuffer: Updating request (clearing buffer)")
        start(request)
    }

    /**
     * Stops buffering and clears the buffer.
     * Call this when ending a game session.
     */
    fun stop() {
        val jobToCancel: Job?
        val bufferedCards: Int
        synchronized(stateLock) {
            jobToCancel = bufferingJob
            bufferingJob = null
            requestGeneration++
            running = false
            currentRequest = null
            bufferedCards = buffer.size
            buffer.clear()
        }
        jobToCancel?.cancel()

        Logger.d(
            "CardBuffer: Stopping (discarded $bufferedCards buffered, generated ${generationCount.get()} cards, " +
                "${cacheHits.get()} hits, ${cacheMisses.get()} misses)",
        )
    }

    /**
     * Stops accepting results and waits until any generator currently using the engine exits.
     * Use this before replacing or closing the engine's repository/model dependencies.
     */
    suspend fun stopAndAwait() {
        stop()
        generationMutex.withLock { /* Wait for in-flight foreground or background generation. */ }
    }

    /**
     * Gets buffer statistics for debugging/analytics.
     */
    fun getStats(): BufferStats = synchronized(stateLock) {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        BufferStats(
            bufferSize = buffer.size,
            maxBufferSize = bufferSize,
            totalGenerated = generationCount.get(),
            cacheHits = hits,
            cacheMisses = misses,
            hitRate = if (hits + misses > 0) hits.toFloat() / (hits + misses) else 0f,
        )
    }

    private fun prepareRequest(
        request: GameEngine.Request,
        forceRestart: Boolean,
    ): Long {
        val jobToCancel: Job?
        val generation: Long
        synchronized(stateLock) {
            if (!forceRestart && running && currentRequest == request) {
                return requestGeneration
            }

            jobToCancel = bufferingJob
            bufferingJob = null
            requestGeneration++
            generation = requestGeneration
            running = true
            currentRequest = request
            buffer.clear()
        }
        jobToCancel?.cancel()
        return generation
    }

    private fun takeBufferedCard(
        request: GameEngine.Request,
        generation: Long,
    ): GameEngine.Result? = synchronized(stateLock) {
        if (isCurrentRequest(request, generation) && buffer.isNotEmpty()) buffer.removeFirst() else null
    }

    private suspend fun generateOnDemand(
        request: GameEngine.Request,
        generation: Long,
    ): GameEngine.Result {
        cacheMisses.incrementAndGet()
        Logger.w(
            "CardBuffer: Cache MISS - generating on-demand (${cacheHits.get()} hits, ${cacheMisses.get()} misses)",
        )

        val result = withContext(Dispatchers.Default) {
            generationMutex.withLock {
                if (!isCurrentRequest(request, generation)) throw supersededRequest()

                takeBufferedCard(request, generation) ?: engine.next(request).also {
                    if (!isCurrentRequest(request, generation)) throw supersededRequest()
                    generationCount.incrementAndGet()
                }
            }
        }
        ensureBuffering(request, generation)
        return result
    }

    private fun ensureBuffering(
        request: GameEngine.Request,
        generation: Long,
    ) {
        if (bufferSize <= 0) return

        val newJob = synchronized(stateLock) {
            if (!isCurrentRequest(request, generation) || bufferingJob?.isCompleted == false) {
                return
            }

            scope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
                refillUntilFull(request, generation)
            }.also { bufferingJob = it }
        }
        newJob.invokeOnCompletion {
            synchronized(stateLock) {
                if (bufferingJob === newJob) bufferingJob = null
            }
        }
        newJob.start()
    }

    private suspend fun refillUntilFull(
        request: GameEngine.Request,
        generation: Long,
    ) {
        while (needsBufferedCard(request, generation)) {
            try {
                generationMutex.withLock {
                    if (!needsBufferedCard(request, generation)) return@withLock

                    Logger.d("CardBuffer: Generating card in background")
                    val card = engine.next(request)
                    val accepted = synchronized(stateLock) {
                        if (isCurrentRequest(request, generation) && buffer.size < bufferSize) {
                            buffer.addLast(card)
                            generationCount.incrementAndGet()
                            true
                        } else {
                            false
                        }
                    }
                    if (accepted) {
                        Logger.d("CardBuffer: Card generated (buffer size: ${getStats().bufferSize}/$bufferSize)")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("CardBuffer: Error during background generation", e)
                return
            }
        }
    }

    private fun supersededRequest() = CancellationException("Card generation request was superseded")

    private fun isCurrentRequest(
        request: GameEngine.Request,
        generation: Long,
    ): Boolean = synchronized(stateLock) {
        running && requestGeneration == generation && currentRequest == request
    }

    private fun needsBufferedCard(
        request: GameEngine.Request,
        generation: Long,
    ): Boolean = synchronized(stateLock) {
        running && requestGeneration == generation && currentRequest == request && buffer.size < bufferSize
    }

    data class BufferStats(
        val bufferSize: Int,
        val maxBufferSize: Int,
        val totalGenerated: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val hitRate: Float,
    ) {
        override fun toString(): String {
            return "BufferStats(buffer=$bufferSize/$maxBufferSize, generated=$totalGenerated, hits=$cacheHits, misses=$cacheMisses, hitRate=${(hitRate * 100).toInt()}%)"
        }
    }
}
