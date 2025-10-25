package com.helldeck.analytics

import android.content.Context
import android.os.Bundle
import com.helldeck.utils.Logger
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Analytics manager for HELLDECK
 * 
 * Provides comprehensive analytics and crash reporting including:
 * - Custom event tracking
 * - User behavior analytics
 * - Performance monitoring
 * - Crash reporting with stack traces
 * - Session tracking
 * - Funnel analysis
 */
class AnalyticsManager private constructor(
    private val context: Context
) {
    private val eventQueue = ConcurrentLinkedQueue<AnalyticsEvent>()
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun initialize(context: Context) {
            getInstance(context).initialize()
        }
    }

    /**
     * Initialize analytics manager
     */
    fun initialize() {
        if (isInitialized) return
        
        try {
            Logger.i("Initializing analytics...")
            
            // Initialize crash handler
            setupCrashHandler()
            
            // Start event processing
            startEventProcessing()
            
            isInitialized = true
            Logger.i("Analytics initialized successfully")
            
            // Track app launch
            trackEvent("app_launched", mapOf(
                "timestamp" to System.currentTimeMillis(),
                "version" to getAppVersion(),
                "device_info" to getDeviceInfo()
            ))
            
        } catch (e: Exception) {
            Logger.e("Failed to initialize analytics", e)
        }
    }

    /**
     * Track custom analytics event
     */
    fun trackEvent(
        eventName: String,
        parameters: Map<String, Any> = emptyMap(),
        isImmediate: Boolean = false
    ) {
        if (!isInitialized) {
            Logger.w("Analytics not initialized, skipping event: $eventName")
            return
        }

        val event = AnalyticsEvent(
            name = eventName,
            parameters = parameters + mapOf(
                "session_id" to getSessionId()
            ),
            timestamp = System.currentTimeMillis(),
            type = EventType.CUSTOM
        )

        if (isImmediate) {
            sendEvent(event)
        } else {
            eventQueue.offer(event)
        }
    }

    /**
     * Track game session
     */
    fun trackGameSession(
        gameId: String,
        gameName: String,
        playerCount: Int,
        duration: Long,
        outcome: String,
        score: Int? = null
    ) {
        trackEvent("game_session_completed", mapOf(
            "game_id" to gameId,
            "game_name" to gameName,
            "player_count" to (playerCount as Any),
            "duration_ms" to duration,
            "outcome" to outcome,
            "score" to (score as Any),
            "completion_rate" to if (score != null) 1.0 else 0.0
        ))
    }

    /**
     * Track user interaction
     */
    fun trackUserInteraction(
        action: String,
        target: String,
        context: String? = null,
        value: String? = null
    ) {
        trackEvent("user_interaction", mapOf(
            "action" to action,
            "target" to target,
            "context" to (context as Any),
            "value" to (value as Any)
        ))
    }

    /**
     * Track performance metrics
     */
    fun trackPerformance(
        operation: String,
        duration: Long,
        success: Boolean,
        metadata: Map<String, Any> = emptyMap()
    ) {
        trackEvent("performance", mapOf(
            "operation" to operation,
            "duration_ms" to duration,
            "success" to success,
            "metadata" to metadata
        ))
    }

    /**
     * Track error
     */
    fun trackError(
        errorType: String,
        message: String,
        stackTrace: String? = null,
        context: String? = null
    ) {
        trackEvent("error", mapOf(
            "error_type" to errorType,
            "message" to message,
            "stack_trace" to (stackTrace as Any),
            "context" to (context as Any),
            "fatal" to false
        ))
    }

    /**
     * Track screen view
     */
    fun trackScreenView(
        screenName: String,
        parameters: Map<String, Any> = emptyMap()
    ) {
        trackEvent("screen_view", mapOf(
            "screen_name" to screenName
        ) + parameters)
    }

    /**
     * Track user engagement
     */
    fun trackEngagement(
        type: String,
        value: Double,
        context: Map<String, Any> = emptyMap()
    ) {
        trackEvent("engagement", mapOf(
            "engagement_type" to type,
            "value" to value
        ) + context)
    }

    /**
     * Track funnel event
     */
    fun trackFunnel(
        funnelName: String,
        step: String,
        stepNumber: Int,
        success: Boolean,
        parameters: Map<String, Any> = emptyMap()
    ) {
        trackEvent("funnel", mapOf(
            "funnel_name" to funnelName,
            "step" to step,
            "step_number" to stepNumber,
            "success" to success
        ) + parameters)
    }

    /**
     * Setup crash handler
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e("Uncaught exception in thread: ${thread.name}", throwable)
            
            // Track crash
            trackEvent("crash", mapOf(
                "thread_name" to thread.name,
                "exception_type" to throwable.javaClass.simpleName,
                "message" to (throwable.message as Any),
                "stack_trace" to getStackTrace(throwable),
                "fatal" to true,
                "timestamp" to System.currentTimeMillis()
            ))
            
            // Send any queued events immediately
            flushEvents()
            
            // Call original handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Start event processing
     */
    private fun startEventProcessing() {
        scope.launch {
            while (isActive) {
                delay(5000) // Process every 5 seconds
                
                val events = mutableListOf<AnalyticsEvent>()
                while (eventQueue.isNotEmpty() && events.size < 50) {
                    events.add(eventQueue.poll() ?: break)
                }
                
                if (events.isNotEmpty()) {
                    sendBatchEvents(events)
                }
            }
        }
    }

    /**
     * Send single event
     */
    private fun sendEvent(event: AnalyticsEvent) {
        try {
            val json = JSONObject().apply {
                put("event_name", event.name)
                put("event_type", event.type.name)
                put("timestamp", event.timestamp)
                put("session_id", getSessionId())
                
                val params = JSONObject()
                event.parameters.forEach { (key, value) ->
                    params.put(key, value.toString())
                }
                put("parameters", params)
            }
            
            // In a real implementation, this would send to analytics service
            Logger.d("Event: ${json.toString()}")
            
        } catch (e: Exception) {
            Logger.e("Failed to send event", e)
        }
    }

    /**
     * Send batch events
     */
    private fun sendBatchEvents(events: List<AnalyticsEvent>) {
        try {
            val jsonArray = org.json.JSONArray()
            events.forEach { event ->
                val json = JSONObject().apply {
                    put("event_name", event.name)
                    put("event_type", event.type.name)
                    put("timestamp", event.timestamp)
                    put("session_id", getSessionId())
                    
                    val params = JSONObject()
                    event.parameters.forEach { (key, value) ->
                        params.put(key, value.toString())
                    }
                    put("parameters", params)
                }
                jsonArray.put(json)
            }
            
            // In a real implementation, this would send to analytics service
            Logger.d("Batch events: ${jsonArray.toString()}")
            
        } catch (e: Exception) {
            Logger.e("Failed to send batch events", e)
        }
    }

    /**
     * Flush all queued events
     */
    fun flushEvents() {
        val events = mutableListOf<AnalyticsEvent>()
        while (eventQueue.isNotEmpty()) {
            events.add(eventQueue.poll() ?: break)
        }
        
        if (events.isNotEmpty()) {
            sendBatchEvents(events)
        }
    }

    /**
     * Get session ID
     */
    private fun getSessionId(): String {
        // In a real implementation, this would generate or retrieve a session ID
        return "session_${System.currentTimeMillis()}"
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            Logger.e("Failed to get app version", e)
            "unknown"
        }
    }

    /**
     * Get device info
     */
    private fun getDeviceInfo(): String {
        return try {
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
        } catch (e: Exception) {
            Logger.e("Failed to get device info", e)
            "unknown"
        }
    }

    /**
     * Get stack trace
     */
    private fun getStackTrace(throwable: Throwable): String {
        return throwable.stackTraceToString()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        flushEvents()
        isInitialized = false
        Logger.i("Analytics cleaned up")
    }

    /**
     * Analytics event data class
     */
    private data class AnalyticsEvent(
        val name: String,
        val parameters: Map<String, Any>,
        val timestamp: Long,
        val type: EventType
    )

    /**
     * Event types
     */
    private enum class EventType {
        CUSTOM,
        GAME_SESSION,
        USER_INTERACTION,
        PERFORMANCE,
        ERROR,
        SCREEN_VIEW,
        ENGAGEMENT,
        FUNNEL,
        CRASH
    }
}