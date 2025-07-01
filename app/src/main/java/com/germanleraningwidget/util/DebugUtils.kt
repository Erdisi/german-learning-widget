package com.germanleraningwidget.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.germanleraningwidget.BuildConfig

/**
 * Lightweight debug utilities for German Learning Widget.
 * 
 * Provides:
 * - Centralized debug configuration
 * - Simple performance timing
 * - Conditional logging
 * - Debug feature flags
 * 
 * Replaces the complex PerformanceMonitor with essential debugging only.
 */
object DebugUtils {
    
    private const val TAG = "DebugUtils"
    
    // Debug feature flags - only active in debug builds
    object FeatureFlags {
        val DETAILED_WIDGET_LOGGING = BuildConfig.DEBUG
        val PERFORMANCE_TIMING = BuildConfig.DEBUG
        val MEMORY_MONITORING = BuildConfig.DEBUG && false // Disabled by default
        val CRASH_REPORTING = true // Always enabled
        val VERBOSE_REPOSITORY_LOGGING = BuildConfig.DEBUG && false // Too verbose
    }
    
    // Simple performance timing
    private val operationTimes = mutableMapOf<String, Long>()
    
    /**
     * Start timing an operation.
     */
    fun startTiming(operation: String) {
        if (FeatureFlags.PERFORMANCE_TIMING) {
            operationTimes[operation] = System.currentTimeMillis()
        }
    }
    
    /**
     * End timing and log result if enabled.
     */
    fun endTiming(operation: String) {
        if (FeatureFlags.PERFORMANCE_TIMING) {
            val startTime = operationTimes.remove(operation)
            if (startTime != null) {
                val duration = System.currentTimeMillis() - startTime
                if (duration > 100) { // Only log operations > 100ms
                    logDebug("Performance", "$operation took ${duration}ms")
                }
            }
        }
    }
    
    /**
     * Measure operation with automatic timing.
     */
    inline fun <T> measureOperation(operation: String, block: () -> T): T {
        startTiming(operation)
        return try {
            block()
        } finally {
            endTiming(operation)
        }
    }
    
    /**
     * Conditional debug logging - only logs in debug builds.
     */
    fun logDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("GLW_$tag", message)
        }
    }
    
    /**
     * Info logging - always logs important information.
     */
    fun logInfo(tag: String, message: String) {
        Log.i("GLW_$tag", message)
    }
    
    /**
     * Warning logging - always logs warnings.
     */
    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("GLW_$tag", message, throwable)
    }
    
    /**
     * Error logging - always logs errors.
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("GLW_$tag", message, throwable)
    }
    
    /**
     * Widget-specific logging with feature flag.
     */
    fun logWidget(message: String) {
        if (FeatureFlags.DETAILED_WIDGET_LOGGING) {
            logDebug("Widget", message)
        }
    }
    
    /**
     * Repository-specific logging with feature flag.
     */
    fun logRepository(message: String) {
        if (FeatureFlags.VERBOSE_REPOSITORY_LOGGING) {
            logDebug("Repository", message)
        }
    }
    
    /**
     * Get basic device information for debugging.
     */
    fun getDeviceInfo(): String {
        return "Device: ${Build.MANUFACTURER} ${Build.MODEL}, " +
                "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                "App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
    
    /**
     * Get simple memory usage information.
     */
    fun getMemoryInfo(): String {
        if (!FeatureFlags.MEMORY_MONITORING) return "Memory monitoring disabled"
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
        val totalMemory = runtime.totalMemory() / (1024 * 1024) // MB
        val freeMemory = runtime.freeMemory() / (1024 * 1024) // MB
        val usedMemory = totalMemory - freeMemory
        
        return "Memory: ${usedMemory}MB used, ${maxMemory}MB max"
    }
    
    /**
     * Log app initialization for debugging.
     */
    fun logAppStart(context: Context) {
        logInfo("App", "German Learning Widget started")
        logDebug("Device", getDeviceInfo())
        if (FeatureFlags.MEMORY_MONITORING) {
            logDebug("Memory", getMemoryInfo())
        }
    }
    
    /**
     * Log widget update for debugging.
     */
    fun logWidgetUpdate(widgetType: String, widgetId: Int) {
        logWidget("Updating $widgetType widget (ID: $widgetId)")
    }
    
    /**
     * Simple crash reporting placeholder.
     * In a real app, this would integrate with Firebase Crashlytics or similar.
     */
    fun reportCrash(throwable: Throwable, context: String = "Unknown") {
        if (FeatureFlags.CRASH_REPORTING) {
            logError("Crash", "Crash in $context: ${throwable.message}", throwable)
            // Future: Consider integrating with Firebase Crashlytics or similar crash reporting service
        }
    }
    
    /**
     * Check if debug features should be shown in UI.
     */
    fun isDebugModeEnabled(): Boolean {
        return BuildConfig.DEBUG
    }
    
    /**
     * Get debug summary for debugging screens.
     */
    fun getDebugSummary(): String {
        return buildString {
            appendLine(getDeviceInfo())
            if (FeatureFlags.MEMORY_MONITORING) {
                appendLine(getMemoryInfo())
            }
            appendLine("Debug Features:")
            appendLine("- Widget Logging: ${FeatureFlags.DETAILED_WIDGET_LOGGING}")
            appendLine("- Performance Timing: ${FeatureFlags.PERFORMANCE_TIMING}")
            appendLine("- Memory Monitoring: ${FeatureFlags.MEMORY_MONITORING}")
            appendLine("- Crash Reporting: ${FeatureFlags.CRASH_REPORTING}")
        }
    }
}

/**
 * Extension functions for easier debugging.
 */
fun <T> T.debugLog(tag: String, message: String): T {
    DebugUtils.logDebug(tag, message)
    return this
}

fun <T> T.debugLogIf(condition: Boolean, tag: String, message: String): T {
    if (condition) {
        DebugUtils.logDebug(tag, message)
    }
    return this
} 