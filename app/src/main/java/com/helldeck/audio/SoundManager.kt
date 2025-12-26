package com.helldeck.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import com.helldeck.utils.Logger

/**
 * Game sounds for enhancing player feedback
 */
enum class GameSound {
    CARD_DRAW,      // Soft "whoosh" when new card appears
    LOL_RATING,     // Light applause for LOL rating
    MEH_RATING,     // Neutral tone for MEH rating
    TRASH_RATING,   // Buzzer for TRASH rating
    ROUND_WIN,      // Fanfare for winning a round
    MILESTONE,      // Special sound for milestone achievements
    BUTTON_PRESS,   // Subtle feedback for button interactions
    TIMER_TICK      // Countdown timer tick
}

/**
 * Manages optional sound effects for game events
 *
 * Sound effects enhance the party game atmosphere while being
 * completely optional via settings toggle.
 */
class SoundManager private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<GameSound, Int>()
    private var isInitialized = false

    var enabled: Boolean = true
        set(value) {
            field = value
            Logger.d("SoundManager: Sound ${if (value) "enabled" else "disabled"}")
        }

    /**
     * Initializes the sound pool and loads sound resources
     */
    fun initialize() {
        if (isInitialized) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()

            // Note: In a production app, you would load actual sound files here
            // For now, we'll use system beep tones as placeholders
            // soundMap[GameSound.CARD_DRAW] = soundPool?.load(context, R.raw.card_draw, 1) ?: 0

            isInitialized = true
            Logger.i("SoundManager: Initialized with ${soundMap.size} sounds")
        } catch (e: Exception) {
            Logger.e("SoundManager: Failed to initialize", e)
        }
    }

    /**
     * Plays a game sound if enabled
     */
    fun play(sound: GameSound, volume: Float = 1f) {
        if (!enabled || !isInitialized) return

        try {
            // For now, use simple system beeps
            // In production, replace with actual sound files
            when (sound) {
                GameSound.LOL_RATING -> playSystemTone(ToneGenerator.TONE_PROP_ACK)
                GameSound.TRASH_RATING -> playSystemTone(ToneGenerator.TONE_PROP_NACK)
                GameSound.ROUND_WIN -> playSystemTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
                GameSound.MILESTONE -> playSystemTone(ToneGenerator.TONE_CDMA_ANSWER)
                GameSound.BUTTON_PRESS -> playSystemTone(ToneGenerator.TONE_PROP_BEEP)
                else -> {
                    // For other sounds, check if we have a loaded sound
                    soundMap[sound]?.let { soundId ->
                        soundPool?.play(soundId, volume, volume, 1, 0, 1f)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("SoundManager: Failed to play sound $sound", e)
        }
    }

    /**
     * Plays a system tone as a placeholder
     * In production, replace with actual sound files
     */
    private fun playSystemTone(tone: Int) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            toneGen.startTone(tone, 150)
            // Release after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, 200)
        } catch (e: Exception) {
            Logger.e("SoundManager: Failed to play system tone", e)
        }
    }

    /**
     * Releases sound pool resources
     */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            isInitialized = false
            Logger.d("SoundManager: Released")
        } catch (e: Exception) {
            Logger.e("SoundManager: Failed to release", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null

        /**
         * Gets the singleton SoundManager instance
         */
        fun get(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also {
                    INSTANCE = it
                    it.initialize()
                }
            }
        }

        /**
         * Quick play method for convenience
         */
        fun play(context: Context, sound: GameSound) {
            get(context).play(sound)
        }
    }
}
