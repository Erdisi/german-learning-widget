package com.germanleraningwidget.util

import android.content.Context
import android.util.Log
import com.germanleraningwidget.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centralized logging system for the German Learning Widget app.
 * 
 * Features:
 * - Structured logging with categories and tags
 * - Error tracking and crash reporting preparation
 * - Performance monitoring and metrics
 * - Thread-safe log buffering
 * - Memory-efficient log management
 * - Debug/Release mode handling
 * 
 * Thread Safety: All operations are thread-safe
 * Performance: Efficient logging with minimal overhead
 * Memory Management: Automatic cleanup of old logs
 */
object AppLogger {
    
    // Configuration
    private const val MAX_LOG_ENTRIES = 1000
    private const val MAX_ERROR_ENTRIES = 100
    private const val TAG_PREFIX = "GLW_"
    
    // Logging scope
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Thread-safe log storage
    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    private val errorEntries = ConcurrentLinkedQueue<ErrorEntry>()
    private val performanceMetrics = ConcurrentLinkedQueue<PerformanceMetric>()
    
    // Counters
    private val totalLogs = AtomicInteger(0)
    private val totalErrors = AtomicInteger(0)
    private val totalWarnings = AtomicInteger(0)
    
    // Date formatter for timestamps
    private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Log levels with priority ordering.
     */
    enum class Level(val priority: Int, val symbol: String) {
        VERBOSE(2, "V"),
        DEBUG(3, "D"),
        INFO(4, "I"),
        WARN(5, "W"),
        ERROR(6, "E"),
        ASSERT(7, "A")
    }
    
    /**
     * Log categories for better organization.
     */
    enum class Category(val tag: String) {
        REPOSITORY("Repo"),
        UI("UI"),
        WIDGET("Widget"),
        WORKER("Worker"),
        NAVIGATION("Nav"),
        PERFORMANCE("Perf"),
        NETWORK("Net"),
        DATABASE("DB"),
        PREFERENCE("Pref"),
        THEME("Theme"),
        ANIMATION("Anim"),
        LIFECYCLE("Lifecycle"),
        ERROR("Error"),
        SECURITY("Security")
    }
    
    /**
     * Data classes for structured logging.
     */
    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val category: Category,
        val tag: String,
        val message: String,
        val threadName: String = Thread.currentThread().name
    ) {
        val formattedTimestamp: String by lazy {
            timestampFormatter.format(Date(timestamp))
        }
    }
    
    data class ErrorEntry(
        val timestamp: Long,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
        val stackTrace: String,
        val threadName: String = Thread.currentThread().name
    ) {
        val formattedTimestamp: String by lazy {
            timestampFormatter.format(Date(timestamp))
        }
    }
    
    data class PerformanceMetric(
        val timestamp: Long,
        val operation: String,
        val durationMs: Long,
        val category: Category,
        val additionalData: Map<String, Any> = emptyMap()
    )
    
    /**
     * Core logging functions.
     */
    fun v(category: Category, tag: String, message: String) {
        // Only log verbose messages in debug builds
        if (BuildConfig.DEBUG) {
            log(Level.VERBOSE, category, tag, message)
        }
    }
    
    fun d(category: Category, tag: String, message: String) {
        // Only log debug messages in debug builds
        if (BuildConfig.DEBUG) {
            log(Level.DEBUG, category, tag, message)
        }
    }
    
    fun i(category: Category, tag: String, message: String) {
        // Info logs are kept in production for essential information
        log(Level.INFO, category, tag, message)
    }
    
    fun w(category: Category, tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, category, tag, message, throwable)
        totalWarnings.incrementAndGet()
    }
    
    fun e(category: Category, tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, category, tag, message, throwable)
        recordError(tag, message, throwable)
        totalErrors.incrementAndGet()
    }
    
    /**
     * Performance monitoring functions.
     */
    fun <T> measureTime(
        category: Category,
        operation: String,
        additionalData: Map<String, Any> = emptyMap(),
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordPerformance(operation, duration, category, additionalData)
        }
    }
    
    /**
     * Specialized logging functions for common scenarios.
     */
    fun logRepositoryOperation(tag: String, operation: String, success: Boolean, durationMs: Long? = null) {
        val level = if (success) Level.INFO else Level.ERROR
        val message = "Repository operation: $operation - ${if (success) "SUCCESS" else "FAILED"}"
        log(level, Category.REPOSITORY, tag, message)
        
        durationMs?.let {
            recordPerformance(operation, it, Category.REPOSITORY, mapOf("success" to success))
        }
    }
    
    fun logUIEvent(tag: String, event: String, additionalInfo: String = "") {
        val message = "UI Event: $event${if (additionalInfo.isNotEmpty()) " - $additionalInfo" else ""}"
        log(Level.DEBUG, Category.UI, tag, message)
    }
    
    fun logWidgetUpdate(tag: String, widgetType: String, success: Boolean, errorMessage: String? = null) {
        val level = if (success) Level.INFO else Level.ERROR
        val message = "Widget update: $widgetType - ${if (success) "SUCCESS" else "FAILED${errorMessage?.let { ": $it" } ?: ""}"}"
        log(level, Category.WIDGET, tag, message)
    }
    
    fun logNavigation(from: String, to: String, success: Boolean = true) {
        val level = if (success) Level.DEBUG else Level.WARN
        val message = "Navigation: $from -> $to${if (!success) " (FAILED)" else ""}"
        log(level, Category.NAVIGATION, "Navigation", message)
    }
    
    fun logLifecycleEvent(component: String, event: String) {
        log(Level.DEBUG, Category.LIFECYCLE, component, "Lifecycle: $event")
    }
    
    /**
     * Security and privacy logging.
     */
    fun logSecurityEvent(tag: String, event: String, severity: Level = Level.WARN) {
        log(severity, Category.SECURITY, tag, "Security Event: $event")
    }
    
    /**
     * Core logging implementation.
     */
    private fun log(level: Level, category: Category, tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX${category.tag}_$tag"
        
        // Log to Android's built-in logging system
        when (level) {
            Level.VERBOSE -> Log.v(fullTag, message, throwable)
            Level.DEBUG -> Log.d(fullTag, message, throwable)
            Level.INFO -> Log.i(fullTag, message, throwable)
            Level.WARN -> Log.w(fullTag, message, throwable)
            Level.ERROR -> Log.e(fullTag, message, throwable)
            Level.ASSERT -> Log.wtf(fullTag, message, throwable)
        }
        
        // Store in internal log for later analysis
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            tag = fullTag,
            message = message
        )
        
        addLogEntry(logEntry)
        totalLogs.incrementAndGet()
    }
    
    /**
     * Record error for crash reporting and analysis.
     */
    private fun recordError(tag: String, message: String, throwable: Throwable?) {
        val stackTrace = throwable?.let { getStackTrace(it) } ?: ""
        val errorEntry = ErrorEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            throwable = throwable,
            stackTrace = stackTrace
        )
        
        addErrorEntry(errorEntry)
    }
    
    /**
     * Record performance metric.
     */
    private fun recordPerformance(operation: String, durationMs: Long, category: Category, additionalData: Map<String, Any>) {
        val metric = PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            operation = operation,
            durationMs = durationMs,
            category = category,
            additionalData = additionalData
        )
        
        addPerformanceMetric(metric)
        
        // Log slow operations
        if (durationMs > 1000) {
            w(Category.PERFORMANCE, "SlowOperation", "Operation '$operation' took ${durationMs}ms")
        }
    }
    
    /**
     * Thread-safe log management.
     */
    private fun addLogEntry(entry: LogEntry) {
        logEntries.offer(entry)
        while (logEntries.size > MAX_LOG_ENTRIES) {
            logEntries.poll()
        }
    }
    
    private fun addErrorEntry(entry: ErrorEntry) {
        errorEntries.offer(entry)
        while (errorEntries.size > MAX_ERROR_ENTRIES) {
            errorEntries.poll()
        }
    }
    
    private fun addPerformanceMetric(metric: PerformanceMetric) {
        performanceMetrics.offer(metric)
        while (performanceMetrics.size > MAX_LOG_ENTRIES) {
            performanceMetrics.poll()
        }
    }
    
    /**
     * Utility functions.
     */
    private fun getStackTrace(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
    
    /**
     * Export functions for debugging and analysis.
     */
    fun getRecentLogs(count: Int = 50): List<LogEntry> {
        return logEntries.toList().takeLast(count)
    }
    
    fun getRecentErrors(count: Int = 20): List<ErrorEntry> {
        return errorEntries.toList().takeLast(count)
    }
    
    fun getPerformanceMetrics(category: Category? = null, count: Int = 50): List<PerformanceMetric> {
        val filtered = if (category != null) {
            performanceMetrics.filter { it.category == category }
        } else {
            performanceMetrics.toList()
        }
        return filtered.takeLast(count)
    }
    
    /**
     * Statistics and health monitoring.
     */
    fun getLogStatistics(): LogStatistics {
        return LogStatistics(
            totalLogs = totalLogs.get(),
            totalErrors = totalErrors.get(),
            totalWarnings = totalWarnings.get(),
            currentLogBufferSize = logEntries.size,
            currentErrorBufferSize = errorEntries.size,
            currentPerformanceBufferSize = performanceMetrics.size
        )
    }
    
    /**
     * Clear logs (useful for testing).
     */
    fun clearLogs() {
        logEntries.clear()
        errorEntries.clear()
        performanceMetrics.clear()
        totalLogs.set(0)
        totalErrors.set(0)
        totalWarnings.set(0)
    }
    
    /**
     * Data class for log statistics.
     */
    data class LogStatistics(
        val totalLogs: Int,
        val totalErrors: Int,
        val totalWarnings: Int,
        val currentLogBufferSize: Int,
        val currentErrorBufferSize: Int,
        val currentPerformanceBufferSize: Int
    ) {
        val errorRate: Float = if (totalLogs > 0) totalErrors.toFloat() / totalLogs else 0f
        val warningRate: Float = if (totalLogs > 0) totalWarnings.toFloat() / totalLogs else 0f
    }

    // Static-like methods for easy access from anywhere
    fun logInfo(tag: String, message: String) {
        i(Category.UI, tag, message)
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        e(Category.ERROR, tag, message, throwable)
    }
    
    fun logWarning(tag: String, message: String) {
        w(Category.UI, tag, message)
    }
    
    fun logDebug(tag: String, message: String) {
        d(Category.UI, tag, message)
    }
} 