package com.germanleraningwidget

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import java.util.concurrent.Executors

/**
 * Custom Application class for German Learning Widget app.
 * 
 * Responsibilities:
 * - Initialize WorkManager with custom configuration
 * - Set up global app-level configurations
 * - Handle application lifecycle events
 * - Provide dependency injection points for testing
 * 
 * Thread Safety: All initialization is done on main thread
 * Error Handling: Graceful handling of initialization failures
 * Performance: Optimized WorkManager configuration
 */
class GermanLearningApplication : Application(), Configuration.Provider {
    
    companion object {
        private const val TAG = "GermanLearningApp"
        private const val WORK_MANAGER_THREAD_POOL_SIZE = 4
        
        // For testing - allows access to application instance
        @Volatile
        private var instance: GermanLearningApplication? = null
        
        fun getInstance(): GermanLearningApplication? = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            instance = this
            
            Log.d(TAG, "Initializing German Learning Application")
            
            // Initialize WorkManager with custom configuration
            initializeWorkManager()
            
            // Initialize other app-level components
            initializeAppComponents()
            
            Log.i(TAG, "Application initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize application", e)
            // Don't crash the app, but log the error for debugging
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating")
        instance = null
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning - performing aggressive cleanup")
        
        performMemoryCleanup(aggressive = true)
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            @Suppress("DEPRECATION")
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.w(TAG, "Moderate memory pressure (level: $level) - performing light cleanup")
                performMemoryCleanup(aggressive = false)
            }
            @Suppress("DEPRECATION")
            TRIM_MEMORY_RUNNING_LOW,
            @Suppress("DEPRECATION") 
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "High memory pressure (level: $level) - performing aggressive cleanup")
                performMemoryCleanup(aggressive = true)
            }
            @Suppress("DEPRECATION")
            TRIM_MEMORY_UI_HIDDEN,
            @Suppress("DEPRECATION")
            TRIM_MEMORY_BACKGROUND -> {
                Log.d(TAG, "App backgrounded (level: $level) - performing background cleanup")
                performMemoryCleanup(aggressive = false)
            }
        }
    }
    
    /**
     * Initialize WorkManager with optimized configuration.
     */
    private fun initializeWorkManager() {
        try {
            WorkManager.initialize(this, workManagerConfiguration)
            Log.d(TAG, "WorkManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WorkManager", e)
            throw ApplicationInitializationException("WorkManager initialization failed", e)
        }
    }
    
    /**
     * Initialize other app-level components.
     */
    private fun initializeAppComponents() {
        try {
            // Pre-initialize repositories to avoid cold start delays
            com.germanleraningwidget.data.repository.SentenceRepository.getInstance(this)
            
            Log.d(TAG, "App components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize app components", e)
            // Don't throw here - app can still function without pre-initialization
        }
    }
    
    /**
     * Custom WorkManager configuration with optimized settings.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setExecutor(
                Executors.newFixedThreadPool(
                    WORK_MANAGER_THREAD_POOL_SIZE,
                    { runnable ->
                        Thread(runnable, "WorkManager-Thread").apply {
                            priority = Thread.NORM_PRIORITY
                            isDaemon = false
                        }
                    }
                )
            )
            .setTaskExecutor(
                Executors.newFixedThreadPool(
                    WORK_MANAGER_THREAD_POOL_SIZE / 2,
                    { runnable ->
                        Thread(runnable, "WorkManager-Task-Thread").apply {
                            priority = Thread.NORM_PRIORITY - 1
                            isDaemon = false
                        }
                    }
                )
            )
            .build()
    
    /**
     * Perform memory cleanup based on memory pressure level.
     */
    private fun performMemoryCleanup(aggressive: Boolean) {
        try {
            // Clear repository caches
            val sentenceRepository = com.germanleraningwidget.data.repository.SentenceRepository
                .getInstance(this)
            sentenceRepository.clearCache()
            
            if (aggressive) {
                // Clear validation caches in data models
                com.germanleraningwidget.data.model.UserPreferences.clearValidationCache()
                
                // Suggest garbage collection (though the system ultimately decides)
                System.gc()
                
                Log.d(TAG, "Aggressive memory cleanup completed")
            } else {
                Log.d(TAG, "Light memory cleanup completed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform memory cleanup", e)
        }
    }
    
    /**
     * Get application-level statistics for monitoring.
     */
    fun getApplicationStatistics(): ApplicationStatistics {
        return try {
            val sentenceRepo = com.germanleraningwidget.data.repository.SentenceRepository.getInstance(this)
            val repoStats = sentenceRepo.getStatistics()
            
            ApplicationStatistics(
                isInitialized = true,
                workManagerInitialized = true,
                repositoryStatistics = repoStats,
                memoryInfo = getMemoryInfo()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get application statistics", e)
            ApplicationStatistics(
                isInitialized = false,
                workManagerInitialized = false,
                repositoryStatistics = null,
                memoryInfo = null,
                error = e.message
            )
        }
    }
    
    /**
     * Get memory information for monitoring.
     */
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        return MemoryInfo(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory()
        )
    }
    
    /**
     * Data classes for application monitoring
     */
    data class ApplicationStatistics(
        val isInitialized: Boolean,
        val workManagerInitialized: Boolean,
        val repositoryStatistics: com.germanleraningwidget.data.repository.SentenceRepository.RepositoryStatistics?,
        val memoryInfo: MemoryInfo?,
        val error: String? = null
    )
    
    data class MemoryInfo(
        val totalMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usedMemory: Long
    ) {
        val memoryUsagePercentage: Double
            get() = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0
    }
    
    /**
     * Custom exception for application initialization errors.
     */
    class ApplicationInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
} 