package com.helldeck

import android.app.Application
import android.content.Context
import com.helldeck.content.data.ContentRepository
import com.helldeck.engine.Config
import com.helldeck.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application class for HELLDECK
 * Handles global application initialization and lifecycle management
 */
class HelldeckApp : Application() {

    /**
     * Called when the application is starting, before any activity, service, or receiver objects have been created.
     * Performs global initialization including logging, configuration, and content repository setup.
     */
    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize app context
            AppCtx.get(this)

            // Initialize logging
            Logger.initialize(
                context = this,
                config = com.helldeck.utils.LoggerConfig(
                    level = com.helldeck.utils.LogLevel.INFO,
                    enableFileLogging = true,
                    enableRemoteLogging = false,
                ),
            )

            Logger.i("HelldeckApp created successfully")

            // Initialize configuration
            Config.load(this)

            // Initialize new content repository in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val contentRepository = ContentRepository(this@HelldeckApp)
                    contentRepository.initialize()
                    Logger.i("ContentRepository initialized successfully")
                } catch (e: Exception) {
                    Logger.e("Failed to initialize ContentRepository or preflight validation failed", e)
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to initialize HelldeckApp", e)
            // Continue execution even if initialization fails
            // The MainActivity will handle specific errors
        }
    }

    /**
     * Called when the system is running low on memory.
     * Logs the event and allows the system to handle memory cleanup.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Logger.w("Application received low memory warning")
    }

    /**
     * Called when the system determines that the amount of memory available is low.
     * Logs the trim level for debugging purposes.
     *
     * @param level The trim level, as defined in {@link android.content.ComponentCallbacks2}.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Logger.d("Application received trim memory level: $level")
    }

    /**
     * Called when the application is being terminated.
     * Logs the termination event.
     */
    override fun onTerminate() {
        super.onTerminate()
        Logger.i("HelldeckApp terminated")
    }
}

/**
 * Application context holder for global access.
 * Provides a singleton instance to access the application context throughout the app.
 */
object AppCtx {
    lateinit var ctx: Context
        private set

    fun get(context: Context): AppCtx {
        ctx = context.applicationContext
        return this
    }
}
