package com.germanleraningwidget

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.germanleraningwidget.util.AppLogger
import com.germanleraningwidget.util.DebugUtils
import com.germanleraningwidget.util.OptimizationUtils
import com.germanleraningwidget.util.PerformanceMonitor
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Optimized main application class with comprehensive performance monitoring and debugging.
 * 
 * Features:
 * - Performance monitoring and optimization
 * - Comprehensive logging and debugging
 * - Memory management and cleanup
 * - Background work scheduling
 * - Health monitoring and reporting
 * 
 * This class ensures optimal app performance from startup to shutdown.
 */
class GermanLearningApplication : Application(), Configuration.Provider {
    
    // Application-wide coroutine scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    companion object {
        private const val TAG = "GermanLearningApp"
        
        // Performance monitoring flags
        private const val ENABLE_PERFORMANCE_MONITORING = true
        private const val ENABLE_MEMORY_MONITORING = true
        private const val ENABLE_DEBUG_LOGGING = true
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize performance monitoring immediately
        initializePerformanceMonitoring()
        
        // Initialize core application components
        OptimizationUtils.measureOptimizedOperation("app_initialization") {
            initializeApplication()
        }
        
        DebugUtils.logInfo(TAG, "German Learning Widget application initialized successfully")
        AppLogger.logInfo(TAG, "Application startup completed")
    }
    
    /**
     * Initialize comprehensive performance monitoring
     */
    private fun initializePerformanceMonitoring() {
        try {
            if (ENABLE_PERFORMANCE_MONITORING) {
                DebugUtils.logInfo(TAG, "Starting performance monitoring")
                OptimizationUtils.startMonitoring(this)
                
                // Start performance monitor
                PerformanceMonitor.startMonitoring(this)
                
                // Schedule periodic health reports
                scheduleHealthReporting()
            }
            
            if (ENABLE_DEBUG_LOGGING) {
                DebugUtils.logInfo(TAG, "Debug logging enabled")
            }
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error initializing performance monitoring", e)
        }
    }
    
    /**
     * Initialize core application components
     */
    private fun initializeApplication() {
        try {
            // Initialize app logger
            AppLogger.logInfo(TAG, "Initializing application components")
            
            // Initialize background work
            initializeBackgroundWork()
            
            // CRITICAL FIX: Initialize widget customizations to prevent reset on app restart
            initializeWidgetCustomizations()
            
            // Perform initial cleanup
            performInitialCleanup()
            
            // Log application health
            logApplicationHealth()
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error during application initialization", e)
            AppLogger.logError(TAG, "Application initialization failed", e)
        }
    }
    
    /**
     * Initialize widget customizations to prevent reset on app restart.
     * CRITICAL FIX: Preloads cached customizations so widgets don't revert to defaults.
     */
    private fun initializeWidgetCustomizations() {
        applicationScope.launch {
            OptimizationUtils.measureOptimizedOperation("widget_customization_initialization") {
                try {
                    DebugUtils.logInfo(TAG, "Preloading widget customizations")
                    
                    // Import widget types
                    val widgetTypes = listOf(
                        com.germanleraningwidget.data.model.WidgetType.MAIN,
                        com.germanleraningwidget.data.model.WidgetType.BOOKMARKS,
                        com.germanleraningwidget.data.model.WidgetType.HERO
                    )
                    
                    // Preload customizations for all widget types SYNCHRONOUSLY
                    widgetTypes.forEach { widgetType ->
                        try {
                            com.germanleraningwidget.widget.WidgetCustomizationHelper.refreshCache(this@GermanLearningApplication, widgetType, sync = true)
                            DebugUtils.logInfo(TAG, "Synchronously loaded customizations for ${widgetType.displayName}")
                        } catch (e: Exception) {
                            DebugUtils.logWarning(TAG, "Failed to synchronously load customizations for ${widgetType.displayName}: ${e.message}")
                        }
                    }
                    
                    // Small delay to ensure cache is fully loaded, then update widgets
                    kotlinx.coroutines.delay(200)
                    
                    // Trigger widget updates with the loaded customizations
                    try {
                        com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateAllWidgetUpdates(this@GermanLearningApplication)
                        DebugUtils.logInfo(TAG, "Triggered widget updates with loaded customizations")
                    } catch (e: Exception) {
                        DebugUtils.logWarning(TAG, "Failed to trigger immediate widget updates: ${e.message}")
                    }
                    
                    DebugUtils.logInfo(TAG, "Widget customization initialization completed")
                    
                } catch (e: Exception) {
                    DebugUtils.logError(TAG, "Error initializing widget customizations", e)
                }
            }
        }
    }

    /**
     * Initialize and schedule background work
     */
    private fun initializeBackgroundWork() {
        OptimizationUtils.measureOptimizedOperation("background_work_initialization") {
            try {
                DebugUtils.logInfo(TAG, "Initializing background work")
                
                val workManager = WorkManager.getInstance(this)
                
                // Schedule sentence delivery work
                val workRequest = SentenceDeliveryWorker.createWorkRequest()
                workManager.enqueueUniquePeriodicWork(
                    "SentenceDeliveryWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                
                DebugUtils.logInfo(TAG, "Background work scheduled successfully")
                AppLogger.logInfo(TAG, "Sentence delivery worker scheduled")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error initializing background work", e)
                AppLogger.logError(TAG, "Background work initialization failed", e)
            }
        }
    }
    
    /**
     * Perform initial application cleanup
     */
    private fun performInitialCleanup() {
        applicationScope.launch {
            try {
                DebugUtils.logInfo(TAG, "Performing initial cleanup")
                
                // Clean up old temporary files and caches
                OptimizationUtils.performComprehensiveCleanup(this@GermanLearningApplication, aggressive = false)
                
                // Force garbage collection to start with clean memory
                val memoryFreed = OptimizationUtils.forceGarbageCollectionWithMonitoring()
                DebugUtils.logInfo(TAG, "Initial cleanup completed: ${memoryFreed.toInt()}MB freed")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error during initial cleanup", e)
            }
        }
    }
    
    /**
     * Log comprehensive application health information
     */
    private fun logApplicationHealth() {
        try {
            val healthReport = OptimizationUtils.generateHealthReport(this)
            
            DebugUtils.logInfo(TAG, "Application Health Report:")
            DebugUtils.logInfo(TAG, "- Overall Score: ${healthReport.overallScore}/100")
            DebugUtils.logInfo(TAG, "- Memory Health: ${healthReport.memoryHealth.score}/100 (${healthReport.memoryHealth.currentUsagePercentage.toInt()}% usage)")
            DebugUtils.logInfo(TAG, "- Performance Health: ${healthReport.performanceHealth.score}/100")
            DebugUtils.logInfo(TAG, "- Resource Health: ${healthReport.resourceHealth.score}/100")
            
            if (healthReport.recommendations.isNotEmpty()) {
                DebugUtils.logInfo(TAG, "Health Recommendations:")
                healthReport.recommendations.forEach { recommendation ->
                    DebugUtils.logInfo(TAG, "  • $recommendation")
                }
            }
            
            // Log optimization suggestions
            val suggestions = OptimizationUtils.getOptimizationSuggestions(this)
            if (suggestions.isNotEmpty()) {
                DebugUtils.logInfo(TAG, "Optimization Suggestions:")
                suggestions.forEach { suggestion ->
                    DebugUtils.logInfo(TAG, "  • $suggestion")
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error logging application health", e)
        }
    }
    
    /**
     * Schedule periodic health reporting for monitoring
     */
    private fun scheduleHealthReporting() {
        applicationScope.launch {
            try {
                // Schedule health reports every hour
                while (true) {
                    kotlinx.coroutines.delay(60 * 60 * 1000L) // 1 hour
                    
                    val healthReport = OptimizationUtils.generateHealthReport(this@GermanLearningApplication)
                    
                    // Only log if there are issues or recommendations
                    if (healthReport.overallScore < 80 || healthReport.recommendations.isNotEmpty()) {
                        DebugUtils.logInfo(TAG, "Periodic Health Check - Score: ${healthReport.overallScore}/100")
                        
                        if (healthReport.recommendations.isNotEmpty()) {
                            DebugUtils.logWarning(TAG, "Health recommendations: ${healthReport.recommendations.joinToString("; ")}")
                        }
                        
                        // Perform automatic cleanup if health is poor
                        if (healthReport.overallScore < 60) {
                            DebugUtils.logWarning(TAG, "Poor health detected - performing automatic cleanup")
                            OptimizationUtils.performComprehensiveCleanup(this@GermanLearningApplication, aggressive = true)
                        }
                    }
                }
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error in health reporting", e)
            }
        }
    }
    
    /**
     * WorkManager configuration for optimized background processing
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (ENABLE_DEBUG_LOGGING) android.util.Log.DEBUG else android.util.Log.INFO)
            .setMaxSchedulerLimit(20) // Reasonable limit for background tasks
            .build()
    
    /**
     * Handle application termination with cleanup
     */
    override fun onTerminate() {
        super.onTerminate()
        
        try {
            DebugUtils.logInfo(TAG, "Application terminating - performing cleanup")
            
            // Stop performance monitoring
            OptimizationUtils.stopMonitoring()
            PerformanceMonitor.stopMonitoring()
            
            // Final cleanup
            applicationScope.launch {
                OptimizationUtils.performComprehensiveCleanup(this@GermanLearningApplication, aggressive = false)
            }
            
            DebugUtils.logInfo(TAG, "Application terminated gracefully")
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error during application termination", e)
        }
    }
    
    /**
     * Handle low memory situations
     */
    override fun onLowMemory() {
        super.onLowMemory()
        
        applicationScope.launch {
            try {
                DebugUtils.logWarning(TAG, "Low memory detected - performing aggressive cleanup")
                AppLogger.logWarning(TAG, "Low memory situation - cleaning up")
                
                // Perform aggressive cleanup
                OptimizationUtils.performComprehensiveCleanup(this@GermanLearningApplication, aggressive = true)
                
                // Force garbage collection
                val memoryFreed = OptimizationUtils.forceGarbageCollectionWithMonitoring()
                DebugUtils.logInfo(TAG, "Low memory cleanup completed: ${memoryFreed.toInt()}MB freed")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error during low memory cleanup", e)
            }
        }
    }
} 