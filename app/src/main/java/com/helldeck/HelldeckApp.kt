package com.helldeck

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.helldeck.data.Repository
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
                    enableRemoteLogging = false
                )
            )

            Logger.i("HelldeckApp created successfully")

            // Initialize configuration
            Config.load(this)

            // Initialize repository in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = Repository.get(this@HelldeckApp)
                    repository.initialize()
                    Logger.i("Repository initialized successfully")
                } catch (e: Exception) {
                    Logger.e("Failed to initialize repository", e)
                }
            }

        } catch (e: Exception) {
            Logger.e("Failed to initialize HelldeckApp", e)
            // Continue execution even if initialization fails
            // The MainActivity will handle specific errors
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Logger.w("Application received low memory warning")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Logger.d("Application received trim memory level: $level")
    }

    override fun onTerminate() {
        super.onTerminate()
        Logger.i("HelldeckApp terminated")
    }
}

/**
 * Application context holder for global access
 */
object AppCtx {
    lateinit var ctx: Context
        private set

    fun get(context: Context): AppCtx {
        ctx = context.applicationContext
        return this
    }
}