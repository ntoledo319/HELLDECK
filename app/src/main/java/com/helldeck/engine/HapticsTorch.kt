package com.helldeck.engine

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptics and torch manager for HELLDECK
 * Provides feedback through vibration and camera flash
 */
object HapticsTorch {

    private var cameraManager: CameraManager? = null
    private var vibrator: Vibrator? = null
    private var mainHandler: Handler? = null

    /**
     * Initialize the haptics and torch system
     */
    fun initialize(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        mainHandler = Handler(Looper.getMainLooper())
    }

    /**
     * Trigger haptic feedback
     */
    fun buzz(
        context: Context,
        durationMs: Long = 35,
        intensity: VibrationIntensity = VibrationIntensity.LIGHT
    ) {
        if (vibrator == null) initialize(context)

        vibrator?.let { vib ->
            if (!vib.hasVibrator()) return

            val amplitude = when (intensity) {
                VibrationIntensity.LIGHT -> 50
                VibrationIntensity.MEDIUM -> 128
                VibrationIntensity.HEAVY -> 255
                VibrationIntensity.DOUBLE -> 255
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (intensity) {
                    VibrationIntensity.DOUBLE -> {
                        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, durationMs, 100, durationMs), intArrayOf(0, amplitude, 0, amplitude), -1))
                    }
                    else -> {
                        vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                    }
                }
            } else {
                vib.vibrate(durationMs)
            }
        }
    }

    /**
     * Trigger pattern vibration
     */
    fun buzzPattern(
        context: Context,
        pattern: LongArray,
        intensities: IntArray? = null,
        repeat: Int = -1
    ) {
        if (vibrator == null) initialize(context)

        vibrator?.let { vib ->
            if (!vib.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, intensities ?: IntArray(pattern.size) { 128 }, repeat)
                vib.vibrate(effect)
            } else {
                vib.vibrate(pattern, repeat)
            }
        }
    }

    /**
     * Flash the camera torch
     */
    fun flash(
        context: Context,
        durationMs: Long = 120,
        intensity: FlashIntensity = FlashIntensity.NORMAL
    ) {
        if (cameraManager == null) initialize(context)

        cameraManager?.let { cm ->
            try {
                val cameraId = cm.cameraIdList.firstOrNull { id ->
                    cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return

                // Turn on torch
                cm.setTorchMode(cameraId, true)

                // Schedule turn off
                mainHandler?.postDelayed({
                    try {
                        cm.setTorchMode(cameraId, false)
                    } catch (e: Exception) {
                        // Camera may be unavailable
                    }
                }, durationMs)

            } catch (e: Exception) {
                // Camera or torch not available
            }
        }
    }

    /**
     * Flash torch in a pattern
     */
    fun flashPattern(
        context: Context,
        pattern: List<FlashPattern>,
        repeatCount: Int = 0
    ) {
        if (cameraManager == null) initialize(context)

        cameraManager?.let { cm ->
            try {
                val cameraId = cm.cameraIdList.firstOrNull { id ->
                    cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return

                var delay = 0L

                pattern.forEach { flashPattern ->
                    mainHandler?.postDelayed({
                        try {
                            cm.setTorchMode(cameraId, true)
                            mainHandler?.postDelayed({
                                try {
                                    cm.setTorchMode(cameraId, false)
                                } catch (e: Exception) {
                                    // Camera may be unavailable
                                }
                            }, flashPattern.durationMs)
                        } catch (e: Exception) {
                            // Camera may be unavailable
                        }
                    }, delay)

                    delay += flashPattern.durationMs + flashPattern.pauseMs
                }

            } catch (e: Exception) {
                // Camera not available
            }
        }
    }

    /**
     * Check if device has vibrator
     */
    fun hasVibrator(context: Context): Boolean {
        if (vibrator == null) initialize(context)
        return vibrator?.hasVibrator() == true
    }

    /**
     * Check if device has camera flash
     */
    fun hasFlash(context: Context): Boolean {
        if (cameraManager == null) initialize(context)

        return try {
            cameraManager?.cameraIdList?.any { cameraId ->
                cameraManager?.getCameraCharacteristics(cameraId)?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get supported vibration intensities
     */
    fun getSupportedIntensities(context: Context): List<VibrationIntensity> {
        if (vibrator == null) initialize(context)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationIntensity.values().toList()
        } else {
            listOf(VibrationIntensity.LIGHT, VibrationIntensity.MEDIUM)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        mainHandler?.removeCallbacksAndMessages(null)
        cameraManager = null
        vibrator = null
        mainHandler = null
    }
}

/**
 * Vibration intensity levels
 */
enum class VibrationIntensity(val description: String) {
    LIGHT("Light vibration"),
    MEDIUM("Medium vibration"),
    HEAVY("Heavy vibration"),
    DOUBLE("Double pulse")
}

/**
 * Flash intensity levels
 */
enum class FlashIntensity(val description: String) {
    QUICK("Quick flash"),
    NORMAL("Normal flash"),
    LONG("Long flash"),
    BRIGHT("Bright flash")
}

/**
 * Flash pattern for complex torch sequences
 */
data class FlashPattern(
    val durationMs: Long,
    val pauseMs: Long = 0,
    val intensity: FlashIntensity = FlashIntensity.NORMAL
)

/**
 * Haptic feedback patterns for different game events
 */
object HapticPatterns {

    /**
     * Pattern for phase change
     */
    val PHASE_CHANGE = longArrayOf(0, 50, 100, 50)

    /**
     * Pattern for scoring lock
     */
    val SCORING_LOCK = longArrayOf(0, 100, 50, 100, 50, 100)

    /**
     * Pattern for room heat
     */
    val ROOM_HEAT = longArrayOf(0, 200, 100, 200, 100, 400)

    /**
     * Pattern for card draw
     */
    val CARD_DRAW = longArrayOf(0, 30)

    /**
     * Pattern for vote confirmation
     */
    val VOTE_CONFIRM = longArrayOf(0, 40, 50, 40)

    /**
     * Pattern for error
     */
    val ERROR = longArrayOf(0, 100, 100, 100, 100, 100)

    /**
     * Pattern for win
     */
    val WIN = longArrayOf(0, 100, 50, 100, 50, 200, 100, 300)

    /**
     * Pattern for countdown (3, 2, 1)
     */
    val COUNTDOWN = longArrayOf(0, 200, 200, 200, 200, 200, 200, 400)
}

/**
 * Torch flash patterns for different game events
 */
object TorchPatterns {

    /**
     * Pattern for scoring lock
     */
    val SCORING_LOCK = listOf(
        FlashPattern(150),
        FlashPattern(150, 100),
        FlashPattern(150)
    )

    /**
     * Pattern for room heat
     */
    val ROOM_HEAT = listOf(
        FlashPattern(200),
        FlashPattern(200, 150),
        FlashPattern(400)
    )

    /**
     * Pattern for game start
     */
    val GAME_START = listOf(
        FlashPattern(100),
        FlashPattern(100, 100),
        FlashPattern(100)
    )

    /**
     * Pattern for round end
     */
    val ROUND_END = listOf(
        FlashPattern(300)
    )

    /**
     * Pattern for tie breaker
     */
    val TIE_BREAKER = listOf(
        FlashPattern(100, 100),
        FlashPattern(100, 100),
        FlashPattern(100)
    )
}

/**
 * Game feedback manager that coordinates haptics and torch
 */
object GameFeedback {

    /**
     * Trigger feedback for game events
     */
    fun triggerFeedback(
        context: Context,
        event: HapticEvent,
        useHaptics: Boolean = true,
        useTorch: Boolean = true
    ) {
        when (event) {
            HapticEvent.PHASE_CHANGE -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.PHASE_CHANGE)
            }
            HapticEvent.SCORING_LOCK -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.SCORING_LOCK)
                if (useTorch) HapticsTorch.flashPattern(context, TorchPatterns.SCORING_LOCK)
            }
            HapticEvent.ROOM_HEAT -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.ROOM_HEAT)
                if (useTorch) HapticsTorch.flashPattern(context, TorchPatterns.ROOM_HEAT)
            }
            HapticEvent.CARD_DRAW -> {
                if (useHaptics) HapticsTorch.buzz(context, 30)
            }
            HapticEvent.VOTE_CONFIRM -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.VOTE_CONFIRM)
            }
            HapticEvent.ERROR -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.ERROR)
            }
            HapticEvent.WIN -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.WIN)
            }
            HapticEvent.COUNTDOWN -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.COUNTDOWN)
            }
            else -> {
                // Default feedback
                if (useHaptics) HapticsTorch.buzz(context)
            }
        }
    }

    /**
     * Trigger feedback for round results
     */
    fun triggerRoundResultFeedback(
        context: Context,
        result: RoundResult,
        useHaptics: Boolean = true,
        useTorch: Boolean = true
    ) {
        when {
            result.roomHeat -> {
                triggerFeedback(context, HapticEvent.ROOM_HEAT, useHaptics, useTorch)
            }
            result.roomTrash -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.ERROR)
            }
            result.points > 0 -> {
                if (useHaptics) HapticsTorch.buzzPattern(context, HapticPatterns.VOTE_CONFIRM)
            }
            else -> {
                if (useHaptics) HapticsTorch.buzz(context, 100, VibrationIntensity.LIGHT)
            }
        }
    }

    /**
     * Trigger feedback for timer events
     */
    fun triggerTimerFeedback(
        context: Context,
        timeRemainingMs: Int,
        totalTimeMs: Int,
        useHaptics: Boolean = true,
        useTorch: Boolean = false
    ) {
        val progress = timeRemainingMs.toFloat() / totalTimeMs.toFloat()

        when {
            progress < 0.1 -> {
                // Critical time - rapid feedback
                if (useHaptics) HapticsTorch.buzz(context, 100, VibrationIntensity.HEAVY)
            }
            progress < 0.3 -> {
                // Warning time - medium feedback
                if (useHaptics) HapticsTorch.buzz(context, 75, VibrationIntensity.MEDIUM)
            }
            else -> {
                // Normal time - light feedback
                if (useHaptics) HapticsTorch.buzz(context, 35, VibrationIntensity.LIGHT)
            }
        }
    }
}

/**
 * Haptic event enumeration for feedback
 */
enum class HapticEvent(val description: String) {
    PHASE_CHANGE("Game phase changed"),
    SCORING_LOCK("Scoring locked in"),
    ROOM_HEAT("Room heat achieved"),
    CARD_DRAW("New card drawn"),
    VOTE_CONFIRM("Vote confirmed"),
    ERROR("Error occurred"),
    WIN("Player won"),
    COUNTDOWN("Timer countdown"),
    GAME_START("Game started"),
    GAME_END("Game ended"),
    ROUND_START("Round started"),
    ROUND_END("Round ended")
}

/**
 * Device capability checker
 */
object DeviceCapabilityChecker {

    /**
     * Check all device capabilities for HELLDECK
     */
    fun getDeviceCapabilities(context: Context): DeviceCapabilities {
        return DeviceCapabilities(
            hasVibrator = HapticsTorch.hasVibrator(context),
            hasFlash = HapticsTorch.hasFlash(context),
            supportedVibrationIntensities = HapticsTorch.getSupportedIntensities(context),
            isKioskSupported = com.helldeck.engine.Kiosk.isKioskModeSupported(context),
            screenResolution = getScreenResolution(context),
            androidVersion = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL
        )
    }

    private fun getScreenResolution(context: Context): String {
        return try {
            val displayMetrics = context.resources.displayMetrics
            "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

/**
 * Device capabilities data class
 */
data class DeviceCapabilities(
    val hasVibrator: Boolean,
    val hasFlash: Boolean,
    val supportedVibrationIntensities: List<VibrationIntensity>,
    val isKioskSupported: Boolean,
    val screenResolution: String,
    val androidVersion: Int,
    val deviceModel: String
) {
    val canProvideFullFeedback: Boolean
        get() = hasVibrator && hasFlash

    val canProvideBasicFeedback: Boolean
        get() = hasVibrator || hasFlash

    val isOptimalForKiosk: Boolean
        get() = isKioskSupported && androidVersion >= Build.VERSION_CODES.LOLLIPOP
}