package com.helldeck.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.helldeck.R
import com.helldeck.engine.ExportImport
import com.helldeck.engine.ImportResult
import kotlinx.coroutines.*

/**
 * Foreground service for handling export/import operations
 * Provides progress updates and handles long-running operations
 */
class ExportImportService : Service() {

    private val binder = ExportImportBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    // Progress tracking
    private var currentOperation: String = ""
    private var progressCallback: ((Int, String) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXPORT_BRAINPACK -> {
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: "brainpack.zip"
                startExport(filename)
            }
            ACTION_IMPORT_BRAINPACK -> {
                val uri = intent.getParcelableExtra<android.net.Uri>(EXTRA_URI)
                if (uri != null) {
                    startImport(uri)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Start brainpack export operation
     */
    private fun startExport(filename: String) {
        currentOperation = "Exporting brainpack..."
        startForeground(NOTIFICATION_ID, createNotification("Preparing export..."))

        serviceScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    progressCallback?.invoke(10, "Collecting data...")
                }

                val uri = ExportImport.exportBrainpack(this@ExportImportService, filename)

                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val message = "Exported brainpack to ${uri.path ?: uri}"
                        progressCallback?.invoke(100, message)
                        updateNotification(message, 100, true)
                    } else {
                        progressCallback?.invoke(-1, "Export failed")
                        updateNotification("Export failed", -1, false)
                    }
                }

                stopSelf()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressCallback?.invoke(-1, "Export failed: ${e.message}")
                    updateNotification("Export failed: ${e.message}", -1, false)
                }
                stopSelf()
            }
        }
    }

    /**
     * Start brainpack import operation
     */
    private fun startImport(uri: android.net.Uri) {
        currentOperation = "Importing brainpack..."
        startForeground(NOTIFICATION_ID, createNotification("Preparing import..."))

        serviceScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    progressCallback?.invoke(0, "Starting import...")
                }

                val result = ExportImport.importBrainpack(this@ExportImportService, uri)

                withContext(Dispatchers.Main) {
                    when (result) {
                        is ImportResult.Success -> {
                            val message = "Imported ${result.templatesImported} templates, ${result.playersImported} players, ${result.roundsImported} rounds"
                            progressCallback?.invoke(100, message)
                            updateNotification("Import completed: $message", 100, true)
                        }
                        is ImportResult.Failure -> {
                            progressCallback?.invoke(-1, "Import failed: ${result.error}")
                            updateNotification("Import failed: ${result.error}", -1, false)
                        }
                        ImportResult.Cancelled -> {
                            progressCallback?.invoke(-1, "Import cancelled")
                            updateNotification("Import cancelled", -1, false)
                        }
                    }
                }

                stopSelf()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressCallback?.invoke(-1, "Import failed: ${e.message}")
                    updateNotification("Import failed: ${e.message}", -1, false)
                }
                stopSelf()
            }
        }
    }

    /**
     * Set progress callback for UI updates
     */
    fun setProgressCallback(callback: (Int, String) -> Unit) {
        progressCallback = callback
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HELLDECK Export/Import",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress for brainpack export/import operations"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create progress notification
     */
    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HELLDECK")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(100, 0, true)
            // // // // // .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Update notification with progress
     */
    private fun updateNotification(content: String, progress: Int, isSuccess: Boolean) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HELLDECK")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(false)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                }
                if (isSuccess) {
                    setCategory("success")
                } else {
                    setCategory(NotificationCompat.CATEGORY_ERROR)
                }
            }
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Auto-cancel success notifications after delay
        if (isSuccess) {
            serviceScope.launch {
                delay(3000)
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    /**
     * Binder for service communication
     */
    inner class ExportImportBinder : Binder() {
        fun getService(): ExportImportService = this@ExportImportService
    }

    companion object {
        const val ACTION_EXPORT_BRAINPACK = "com.helldeck.action.EXPORT_BRAINPACK"
        const val ACTION_IMPORT_BRAINPACK = "com.helldeck.action.IMPORT_BRAINPACK"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_URI = "uri"

        private const val CHANNEL_ID = "helldeck_export_import"
        private const val NOTIFICATION_ID = 1001

        /**
         * Create intent for export operation
         */
        fun createExportIntent(context: Context, filename: String): Intent {
            return Intent(context, ExportImportService::class.java).apply {
                action = ACTION_EXPORT_BRAINPACK
                putExtra(EXTRA_FILENAME, filename)
            }
        }

        /**
         * Create intent for import operation
         */
        fun createImportIntent(context: Context, uri: android.net.Uri): Intent {
            return Intent(context, ExportImportService::class.java).apply {
                action = ACTION_IMPORT_BRAINPACK
                putExtra(EXTRA_URI, uri)
            }
        }
    }
}
