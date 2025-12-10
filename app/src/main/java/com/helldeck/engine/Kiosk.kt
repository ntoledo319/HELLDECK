package com.helldeck.engine

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.helldeck.admin.HelldeckDeviceAdminReceiver

/**
 * Kiosk mode manager for HELLDECK
 * Handles device lockdown and immersive mode
 */
@Suppress("DEPRECATION")
object Kiosk {

    private const val SYSTEM_UI_FLAG_IMMERSIVE_STICKY = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val SYSTEM_UI_FLAG_HIDE_NAVIGATION = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    private const val SYSTEM_UI_FLAG_FULLSCREEN = View.SYSTEM_UI_FLAG_FULLSCREEN
    private const val SYSTEM_UI_FLAG_LAYOUT_STABLE = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    private const val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    private const val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

    /**
     * Enable immersive mode on a view
     */
    fun enableImmersiveMode(decorView: View) {
        // Back-compat fallback for older APIs
        decorView.systemUiVisibility = (
            SYSTEM_UI_FLAG_LAYOUT_STABLE
            or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or SYSTEM_UI_FLAG_FULLSCREEN
            or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    /**
     * Preferred immersive mode using WindowInsetsControllerCompat
     */
    fun enableImmersiveMode(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Enable lock task mode for kiosk functionality
     */
    fun enableLockTask(context: Context) {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

            // Set lock task packages if device admin is active
            if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                devicePolicyManager.setLockTaskPackages(
                    componentName,
                    arrayOf(context.packageName)
                )
            }
        } catch (e: Exception) {
            // Device admin not available or not device owner
        }
    }

    /**
     * Start lock task mode
     */
    fun startLockTask(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startLockTask()
        }
    }

    /**
     * Stop lock task mode
     */
    fun stopLockTask(activity: ComponentActivity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.stopLockTask()
            }
        } catch (e: Exception) {
            // Lock task not active or not permitted
        }
    }

    /**
     * Check if device is in lock task mode
     */
    fun isInLockTaskMode(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Setup activity for kiosk mode
     */
    fun setupKioskActivity(activity: Activity) {
        // Hide navigation and status bars (new API)
        enableImmersiveMode(activity)

        // Enable lock task if possible
        enableLockTask(activity)

        // Start lock task mode
        startLockTask(activity)

        // Setup window flags for fullscreen
        val window = activity.window
        // Modern edge-to-edge behavior
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Prefer modern APIs when available
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            // Fallback flags for pre-27
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            devicePolicyManager.isAdminActive(componentName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if app is device owner
     */
    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            devicePolicyManager.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Request device admin permissions
     */
    fun requestDeviceAdmin(context: Context) {
        try {
            val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "HELLDECK requires device admin to enable kiosk mode")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Cannot start device admin request
        }
    }

    /**
     * Remove device admin permissions
     */
    fun removeDeviceAdmin(context: Context) {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            devicePolicyManager.removeActiveAdmin(componentName)
        } catch (e: Exception) {
            // Cannot remove device admin
        }
    }

    /**
     * Check if kiosk mode is properly configured
     */
    fun isKioskModeConfigured(context: Context): Boolean {
        return try {
            // Check if device admin is active
            val isAdminActive = isDeviceAdminActive(context)

            // Check if app can be set as home app
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            val canBeHome = resolveInfo?.activityInfo?.packageName == context.packageName

            isAdminActive || canBeHome
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Setup system-level kiosk restrictions
     */
    fun setupSystemRestrictions(context: Context) {
        try {
            // Disable status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

                if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                    // Set status bar disabled
                    devicePolicyManager.setStatusBarDisabled(componentName, true)

                    // Set keyguard disabled
                    devicePolicyManager.setKeyguardDisabled(componentName, true)

                    // Set maximum time to lock (prevent sleep)
                    devicePolicyManager.setMaximumTimeToLock(componentName, Long.MAX_VALUE)
                }
            }
        } catch (e: Exception) {
            // System restrictions not available
        }
    }

    /**
     * Restore system settings (when exiting kiosk mode)
     */
    fun restoreSystemSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val componentName = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

                if (devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
                    // Re-enable status bar
                    devicePolicyManager.setStatusBarDisabled(componentName, false)

                    // Re-enable keyguard
                    devicePolicyManager.setKeyguardDisabled(componentName, false)

                    // Set normal time to lock (5 minutes)
                    devicePolicyManager.setMaximumTimeToLock(componentName, 5 * 60 * 1000L)
                }
            }
        } catch (e: Exception) {
            // Cannot restore system settings
        }
    }

    /**
     * Check if device supports kiosk mode
     */
    fun isKioskModeSupported(context: Context): Boolean {
        return try {
            // Check for device admin support
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            devicePolicyManager != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get kiosk mode status
     */
    fun getKioskStatus(context: Context): KioskStatus {
        return KioskStatus(
            isDeviceAdminActive = isDeviceAdminActive(context),
            isDeviceOwner = isDeviceOwner(context),
            isInLockTaskMode = false, // Would need activity context
            isImmersiveMode = true, // Would check current state
            isKioskConfigured = isKioskModeConfigured(context),
            isKioskSupported = isKioskModeSupported(context)
        )
    }

    /**
     * Setup broadcast receiver for system events
     */
    fun setupKioskBroadcastReceiver(context: Context): KioskBroadcastReceiver {
        return KioskBroadcastReceiver().apply {
            register(context)
        }
    }
}

/**
 * Kiosk status data class
 */
data class KioskStatus(
    val isDeviceAdminActive: Boolean,
    val isDeviceOwner: Boolean,
    val isInLockTaskMode: Boolean,
    val isImmersiveMode: Boolean,
    val isKioskConfigured: Boolean,
    val isKioskSupported: Boolean
) {
    val isFullyFunctional: Boolean
        get() = isKioskSupported && (isDeviceAdminActive || isDeviceOwner)

    val needsSetup: Boolean
        get() = isKioskSupported && !isKioskConfigured

    val canEnableKiosk: Boolean
        get() = isKioskSupported && isDeviceAdminActive
}

/**
 * Broadcast receiver for kiosk-related system events
 */
class KioskBroadcastReceiver : android.content.BroadcastReceiver() {

    private var isRegistered = false

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Screen turned off - could log or handle
            }
            Intent.ACTION_SCREEN_ON -> {
                // Screen turned on - ensure immersive mode
                val activity = getCurrentActivity(context)
                activity?.let { Kiosk.enableImmersiveMode(it) }
            }
            Intent.ACTION_USER_PRESENT -> {
                // User unlocked device - ensure kiosk mode
                val activity = getCurrentActivity(context)
                activity?.let {
                    if (Kiosk.isDeviceAdminActive(context)) {
                        Kiosk.startLockTask(it)
                    }
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Package replaced - ensure kiosk configuration
                if (intent.data?.schemeSpecificPart == context.packageName) {
                    Kiosk.enableLockTask(context)
                }
            }
        }
    }

    fun register(context: Context) {
        if (isRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
        }

        ContextCompat.registerReceiver(
            context,
            this,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return

        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            // Receiver not registered
        }

        isRegistered = false
    }

    private fun getCurrentActivity(context: Context): Activity? {
        return try {
            // This is a simplified approach - in practice you'd need a more robust method
            // to get the current activity
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Kiosk mode utilities
 */
object KioskUtils {

    /**
     * Check if device is in a state that supports kiosk mode
     */
    fun isDeviceKioskReady(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

            // Check if device is in a user that supports device owner
            val isUserSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                !userManager.isDemoUser
            } else {
                true
            }

            isUserSupported && devicePolicyManager != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get recommended kiosk setup method for device
     */
    fun getRecommendedSetupMethod(context: Context): KioskSetupMethod {
        return when {
            Kiosk.isDeviceOwner(context) -> KioskSetupMethod.DEVICE_OWNER
            Kiosk.isDeviceAdminActive(context) -> KioskSetupMethod.DEVICE_ADMIN
            canSetAsHomeApp(context) -> KioskSetupMethod.HOME_APP
            else -> KioskSetupMethod.MANUAL
        }
    }

    /**
     * Check if app can be set as home app
     */
    private fun canSetAsHomeApp(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Setup fallback kiosk mode using system settings
     */
    fun setupFallbackKiosk(context: Context) {
        try {
            // Enable screen pinning if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }

            // Set app as default home (requires user interaction)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            // Cannot setup fallback kiosk
        }
    }
}

/**
 * Kiosk setup methods
 */
enum class KioskSetupMethod(val description: String) {
    DEVICE_OWNER("Device Owner (most secure)"),
    DEVICE_ADMIN("Device Administrator"),
    HOME_APP("Home App Replacement"),
    MANUAL("Manual Setup Required")
}
