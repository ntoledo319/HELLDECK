package com.helldeck.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin Receiver for HELLDECK kiosk functionality
 * Handles device administrator events and permissions
 */
class HelldeckDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)

        // Device admin enabled - setup kiosk features
        Toast.makeText(context, "HELLDECK: Device Admin enabled - Kiosk mode active", Toast.LENGTH_SHORT).show()

        // Setup system restrictions
        setupKioskRestrictions(context)

        // Log event
        logDeviceAdminEvent(context, "enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        // Device admin disabled - remove kiosk features
        Toast.makeText(context, "HELLDECK: Device Admin disabled - Kiosk mode inactive", Toast.LENGTH_SHORT).show()

        // Restore system settings
        restoreSystemSettings(context)

        // Log event
        logDeviceAdminEvent(context, "disabled")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)

        // Entering lock task mode
        Toast.makeText(context, "HELLDECK: Entering kiosk mode", Toast.LENGTH_SHORT).show()

        logDeviceAdminEvent(context, "lock_task_entering", mapOf("package" to pkg))
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)

        // Exiting lock task mode
        Toast.makeText(context, "HELLDECK: Exiting kiosk mode", Toast.LENGTH_SHORT).show()

        logDeviceAdminEvent(context, "lock_task_exiting")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, userHandle)

        // Password changed - may affect kiosk security
        logDeviceAdminEvent(context, "password_changed")
    }

    override fun onPasswordExpiring(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordExpiring(context, intent, userHandle)

        // Password expiring soon
        Toast.makeText(context, "HELLDECK: Device password expiring soon", Toast.LENGTH_LONG).show()

        logDeviceAdminEvent(context, "password_expiring")
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)

        // Password attempt failed
        logDeviceAdminEvent(context, "password_failed")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)

        // Password attempt succeeded
        logDeviceAdminEvent(context, "password_succeeded")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle custom actions
        when (intent.action) {
            ACTION_SETUP_KIOSK -> setupKioskRestrictions(context)
            ACTION_RESTORE_SYSTEM -> restoreSystemSettings(context)
            ACTION_CHECK_KIOSK_STATUS -> checkKioskStatus(context)
        }
    }

    /**
     * Setup kiosk-specific restrictions
     */
    private fun setupKioskRestrictions(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // Device owner permissions
                dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                dpm.setStatusBarDisabled(adminComponent, true)
                dpm.setKeyguardDisabled(adminComponent, true)
                dpm.setMaximumTimeToLock(adminComponent, Long.MAX_VALUE)

                // Disable camera if needed (torch still works)
                // dpm.setCameraDisabled(adminComponent, false)

                // Disable screen capture
                dpm.setScreenCaptureDisabled(adminComponent, true)
            } else if (dpm.isAdminActive(adminComponent)) {
                // Device admin permissions (limited)
                dpm.setMaximumTimeToLock(adminComponent, 30 * 60 * 1000L) // 30 minutes
            }
        } catch (e: Exception) {
            // Failed to setup restrictions
        }
    }

    /**
     * Restore normal system settings
     */
    private fun restoreSystemSettings(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // Restore device owner settings
                dpm.setStatusBarDisabled(adminComponent, false)
                dpm.setKeyguardDisabled(adminComponent, false)
                dpm.setMaximumTimeToLock(adminComponent, 5 * 60 * 1000L) // 5 minutes
                dpm.setScreenCaptureDisabled(adminComponent, false)
            } else if (dpm.isAdminActive(adminComponent)) {
                // Restore device admin settings
                dpm.setMaximumTimeToLock(adminComponent, 5 * 60 * 1000L) // 5 minutes
            }
        } catch (e: Exception) {
            // Failed to restore settings
        }
    }

    /**
     * Check current kiosk status
     */
    private fun checkKioskStatus(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

            val status: Map<String, Any> = mapOf(
                "isDeviceOwner" to dpm.isDeviceOwnerApp(context.packageName),
                "isAdminActive" to dpm.isAdminActive(adminComponent),
                "isStatusBarDisabled" to if (dpm.isDeviceOwnerApp(context.packageName)) {
                    dpm.getStorageEncryptionStatus() != android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED
                } else {
                    false
                },
                "lockTaskPackages" to (dpm.getLockTaskPackages(adminComponent)?.toList() ?: emptyList<String>()),
            )

            logDeviceAdminEvent(context, "status_check", status)
        } catch (e: Exception) {
            logDeviceAdminEvent(context, "status_check_failed", mapOf("error" to e.message.toString()))
        }
    }

    /**
     * Log device admin events for debugging
     */
    private fun logDeviceAdminEvent(context: Context, event: String, data: Map<String, Any>? = null) {
        try {
            // In a real implementation, you might want to log to a file or send to analytics
            val logData = mapOf(
                "event" to event,
                "timestamp" to System.currentTimeMillis(),
                "package" to context.packageName,
            ) + (data ?: emptyMap())

            // For now, just log to system log
            android.util.Log.d("HelldeckDeviceAdmin", "Event: $event, Data: $logData")
        } catch (e: Exception) {
            // Logging failed
        }
    }

    companion object {
        const val ACTION_SETUP_KIOSK = "com.helldeck.action.SETUP_KIOSK"
        const val ACTION_RESTORE_SYSTEM = "com.helldeck.action.RESTORE_SYSTEM"
        const val ACTION_CHECK_KIOSK_STATUS = "com.helldeck.action.CHECK_KIOSK_STATUS"

        /**
         * Create intent to setup kiosk mode
         */
        fun createSetupKioskIntent(context: Context): Intent {
            return Intent(ACTION_SETUP_KIOSK).apply {
                component = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            }
        }

        /**
         * Create intent to restore system settings
         */
        fun createRestoreSystemIntent(context: Context): Intent {
            return Intent(ACTION_RESTORE_SYSTEM).apply {
                component = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            }
        }

        /**
         * Create intent to check kiosk status
         */
        fun createCheckStatusIntent(context: Context): Intent {
            return Intent(ACTION_CHECK_KIOSK_STATUS).apply {
                component = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
            }
        }
    }
}

/**
 * Device admin utilities
 */
object DeviceAdminUtils {

    /**
     * Check if device admin permissions are granted
     */
    fun isDeviceAdminGranted(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    /**
     * Check if device owner permissions are granted
     */
    fun isDeviceOwnerGranted(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Get device admin features status
     */
    fun getDeviceAdminFeatures(context: Context): DeviceAdminFeatures {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)

        return DeviceAdminFeatures(
            isAdminActive = dpm.isAdminActive(adminComponent),
            isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName),
            canLockTask = dpm.isLockTaskPermitted(context.packageName),
            canDisableStatusBar = dpm.isDeviceOwnerApp(context.packageName),
            canDisableKeyguard = dpm.isDeviceOwnerApp(context.packageName),
            canDisableCamera = dpm.isDeviceOwnerApp(context.packageName),
            canDisableScreenCapture = dpm.isDeviceOwnerApp(context.packageName),
        )
    }

    /**
     * Request device admin permissions with explanation
     */
    fun requestDeviceAdminWithExplanation(context: Context) {
        val adminComponent = ComponentName(context, HelldeckDeviceAdminReceiver::class.java)
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "HELLDECK needs device administrator permissions to:\n" +
                    "• Enable kiosk mode for dedicated gameplay\n" +
                    "• Prevent accidental exit from the game\n" +
                    "• Maintain immersive fullscreen experience\n" +
                    "• Lock the device to HELLDECK app\n\n" +
                    "These permissions are only used for kiosk functionality and can be removed at any time.",
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Cannot start device admin request
        }
    }
}

/**
 * Device admin features status
 */
data class DeviceAdminFeatures(
    val isAdminActive: Boolean,
    val isDeviceOwner: Boolean,
    val canLockTask: Boolean,
    val canDisableStatusBar: Boolean,
    val canDisableKeyguard: Boolean,
    val canDisableCamera: Boolean,
    val canDisableScreenCapture: Boolean,
) {
    val kioskLevel: KioskLevel
        get() = when {
            isDeviceOwner -> KioskLevel.FULL
            isAdminActive -> KioskLevel.PARTIAL
            else -> KioskLevel.NONE
        }

    enum class KioskLevel(val description: String) {
        NONE("No kiosk capabilities"),
        PARTIAL("Limited kiosk features"),
        FULL("Full kiosk functionality"),
    }
}
