package com.helldeck.content.engine

import com.helldeck.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val bufferSize: Int = 3
) {
    private val buffer = ArrayDeque<GameEngine.Result>(bufferSize)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bufferingJob: Job? = null
    private var currentRequest: GameEngine.Request? = null
    private var isActive = false
    private val generationCount = AtomicInteger(0)
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)

    /**
     * Starts buffering cards for the given request parameters.
     * Call this when starting a new game or session to pre-generate cards.
     */
    fun start(request: GameEngine.Request) {
        Logger.d("CardBuffer: Starting buffer for game=${request.gameId}")
        currentRequest = request
        isActive = true

        bufferingJob?.cancel()
        bufferingJob = scope.launch {
            while (isActive && coroutineContext.isActive) {
                try {
                    mutex.withLock {
                        if (buffer.size < bufferSize && currentRequest != null) {
                            Logger.d("CardBuffer: Generating card (buffer size: ${buffer.size}/$bufferSize)")
                            val card = engine.next(currentRequest!!)
                            buffer.addLast(card)
                            generationCount.incrementAndGet()
                            Logger.d("CardBuffer: Card generated (buffer size: ${buffer.size}/$bufferSize)")
                        }
                    }

                    // Small delay to prevent tight loop
                    delay(100)
                } catch (e: Exception) {
                    Logger.e("CardBuffer: Error during background generation", e)
                    delay(1000) // Back off on error
                }
            }
        }
    }

    /**
     * Gets the next card from the buffer.
     * Returns instantly if buffer has cards, otherwise generates on-demand.
     *
     * @return The next card, either from buffer (instant) or freshly generated (fallback)
     */
    suspend fun getNext(): GameEngine.Result {
        val fromBuffer = mutex.withLock {
            if (buffer.isNotEmpty()) {
                cacheHits.incrementAndGet()
                buffer.removeFirst()
            } else {
                null
            }
        }

        return if (fromBuffer != null) {
            Logger.d("CardBuffer: Cache HIT (${cacheHits.get()} hits, ${cacheMisses.get()} misses)")
            fromBuffer
        } else {
            cacheMisses.incrementAndGet()
            Logger.w("CardBuffer: Cache MISS - generating on-demand (${cacheHits.get()} hits, ${cacheMisses.get()} misses)")

            // Fallback: generate immediately if buffer empty
            currentRequest?.let { request ->
                engine.next(request)
            } ?: throw IllegalStateException("CardBuffer: No request set, cannot generate card")
        }
    }

    /**
     * Updates the request parameters for future card generation.
     * Useful when game settings change mid-session (e.g., spice level adjusted).
     */
    fun updateRequest(request: GameEngine.Request) {
        Logger.d("CardBuffer: Updating request (clearing buffer)")
        currentRequest = request

        scope.launch {
            mutex.withLock {
                // Clear buffer when request changes
                buffer.clear()
            }
        }
    }

    /**
     * Stops buffering and clears the buffer.
     * Call this when ending a game session.
     */
    fun stop() {
        Logger.d("CardBuffer: Stopping (generated ${generationCount.get()} cards, ${cacheHits.get()} hits, ${cacheMisses.get()} misses)")
        isActive = false
        bufferingJob?.cancel()
        bufferingJob = null

        scope.launch {
            mutex.withLock {
                buffer.clear()
            }
        }
    }

    /**
     * Gets buffer statistics for debugging/analytics.
     */
    fun getStats(): BufferStats {
        return BufferStats(
            bufferSize = buffer.size,
            maxBufferSize = bufferSize,
            totalGenerated = generationCount.get(),
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            hitRate = if (cacheHits.get() + cacheMisses.get() > 0) {
                cacheHits.get().toFloat() / (cacheHits.get() + cacheMisses.get())
            } else 0f
        )
    }

    data class BufferStats(
        val bufferSize: Int,
        val maxBufferSize: Int,
        val totalGenerated: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val hitRate: Float
    ) {
        override fun toString(): String {
            return "BufferStats(buffer=$bufferSize/$maxBufferSize, generated=$totalGenerated, hits=$cacheHits, misses=$cacheMisses, hitRate=${(hitRate * 100).toInt()}%)"
        }
    }
}
