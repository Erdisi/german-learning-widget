package com.germanleraningwidget.worker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.util.AppLogger
import com.germanleraningwidget.util.DebugUtils
import com.germanleraningwidget.util.OptimizationUtils
import com.germanleraningwidget.util.AppConstants
import com.germanleraningwidget.widget.GermanLearningWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Optimized worker that delivers new German sentences to widgets every 90 minutes.
 * 
 * Features a fixed schedule approach:
 * - 10 sentences selected daily at midnight
 * - Updates every 90 minutes during the day
 * - Cycles through the daily sentence pool
 * - Performance monitoring and optimization
 * - Comprehensive error handling and recovery
 */
class SentenceDeliveryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sentenceRepository = SentenceRepository.getInstance(applicationContext)
    private val userPreferencesRepository = UserPreferencesRepository(applicationContext)

    companion object {
        private const val TAG = "SentenceDeliveryWorker"
        const val WORK_NAME = AppConstants.SENTENCE_DELIVERY_WORK_NAME
        const val FIXED_UPDATE_INTERVAL_MINUTES = AppConstants.FIXED_UPDATE_INTERVAL_MINUTES.toLong()
        const val FIXED_SENTENCES_PER_DAY = AppConstants.FIXED_SENTENCES_PER_DAY
        
        // Performance monitoring keys
        private const val OPERATION_WORKER_EXECUTION = AppConstants.PerformanceOperations.WORKER_EXECUTION
        private const val OPERATION_SENTENCE_FETCH = AppConstants.PerformanceOperations.SENTENCE_FETCH
        private const val OPERATION_WIDGET_UPDATE = AppConstants.PerformanceOperations.WIDGET_UPDATE
        private const val OPERATION_DAILY_POOL_GENERATION = AppConstants.PerformanceOperations.DAILY_POOL_GENERATION
        
        /**
         * Create work request for sentence delivery
         */
        fun createWorkRequest() = PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(
            FIXED_UPDATE_INTERVAL_MINUTES, 
            TimeUnit.MINUTES
        ).build()
        
        /**
         * Schedule the sentence delivery work
         */
        fun scheduleWork(context: Context) {
            try {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val workRequest = createWorkRequest()
                
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                
                DebugUtils.logInfo(TAG, "Sentence delivery work scheduled successfully")
                AppLogger.logInfo(TAG, "Work scheduled")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Failed to schedule sentence delivery work", e)
                AppLogger.logError(TAG, "Work scheduling failed", e)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext OptimizationUtils.measureOptimizedOperation(OPERATION_WORKER_EXECUTION) {
            try {
                DebugUtils.logInfo(TAG, "Starting sentence delivery work")
                AppLogger.logInfo(TAG, "Worker execution started")
                
                val preferences = OptimizationUtils.measureOptimizedOperation(AppConstants.PerformanceOperations.LOAD_PREFERENCES) {
                    userPreferencesRepository.userPreferences.first()
                }
                
                if (preferences.selectedGermanLevels.isEmpty() || preferences.selectedTopics.isEmpty()) {
                    DebugUtils.logWarning(TAG, "No learning preferences set - skipping update")
                    AppLogger.logInfo(TAG, "Skipped - no preferences")
                    return@measureOptimizedOperation Result.success()
                }

                // Check if we need to generate a new daily sentence pool
                val shouldRegeneratePool = OptimizationUtils.measureOptimizedOperation(AppConstants.PerformanceOperations.CHECK_POOL_REGENERATION) {
                    sentenceRepository.shouldRegenerateDailyPool()
                }
                
                if (shouldRegeneratePool) {
                    DebugUtils.logInfo(TAG, "Generating new daily sentence pool")
                    generateDailyPool(preferences.selectedGermanLevels, preferences.selectedTopics)
                }

                // Get next sentence from the daily pool
                val nextSentence = OptimizationUtils.measureOptimizedOperation(OPERATION_SENTENCE_FETCH) {
                    sentenceRepository.getNextSentenceFromDailyPool()
                }

                if (nextSentence == null) {
                    DebugUtils.logWarning(TAG, "No sentence available from daily pool")
                    AppLogger.logInfo(TAG, "No sentence available - attempting pool regeneration")
                    
                    // Try to regenerate pool as fallback
                    generateDailyPool(preferences.selectedGermanLevels, preferences.selectedTopics)
                    val fallbackSentence = sentenceRepository.getNextSentenceFromDailyPool()
                    
                    if (fallbackSentence == null) {
                        DebugUtils.logError(TAG, "Failed to get sentence even after pool regeneration")
                        AppLogger.logError(TAG, "Critical: No sentences available after regeneration")
                        return@measureOptimizedOperation Result.failure()
                    }
                    
                    updateWidgets(fallbackSentence.id)
                } else {
                    updateWidgets(nextSentence.id)
                }

                DebugUtils.logInfo(TAG, "Sentence delivery completed successfully")
                AppLogger.logInfo(TAG, "Worker execution completed successfully")
                
                // Perform periodic cleanup if needed
                performPeriodicCleanup()
                
                Result.success()
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error in sentence delivery", e)
                AppLogger.logError(TAG, "Worker failed: ${e.message}", e)
                handleWorkerError(e)
                Result.failure()
            }
        }
    }

    /**
     * Generate daily sentence pool with performance monitoring
     */
    private suspend fun generateDailyPool(levels: Set<String>, topics: Set<String>) {
        OptimizationUtils.measureOptimizedOperation(OPERATION_DAILY_POOL_GENERATION) {
            try {
                DebugUtils.logInfo(TAG, "Generating daily pool for levels: $levels, topics: $topics")
                
                val poolSentences = sentenceRepository.generateDailyPool(
                    levels = levels,
                    topics = topics,
                    poolSize = FIXED_SENTENCES_PER_DAY
                )
                
                DebugUtils.logInfo(TAG, "Generated daily pool with ${poolSentences.size} sentences")
                AppLogger.logInfo(TAG, "Daily pool generated: ${poolSentences.size} sentences")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error generating daily pool", e)
                AppLogger.logError(TAG, "Daily pool generation failed", e)
                throw e
            }
        }
    }

    /**
     * Update all widgets with performance monitoring
     */
    private suspend fun updateWidgets(sentenceId: Long) = withContext(Dispatchers.Main) {
        OptimizationUtils.measureOptimizedOperation(OPERATION_WIDGET_UPDATE) {
            try {
                DebugUtils.logInfo(TAG, "Updating widgets with sentence ID: $sentenceId")
                
                val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val widgetIntent = Intent(applicationContext, GermanLearningWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra("sentenceId", sentenceId)
                    putExtra("triggerUpdate", true)
                }
                
                applicationContext.sendBroadcast(widgetIntent)
                
                DebugUtils.logInfo(TAG, "Widget update broadcast sent")
                AppLogger.logInfo(TAG, "Widgets updated with sentence ID: $sentenceId")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error updating widgets", e)
                AppLogger.logError(TAG, "Widget update failed", e)
                throw e
            }
        }
    }
    
    /**
     * Handle worker errors with intelligent recovery
     */
    private suspend fun handleWorkerError(error: Exception) {
        try {
            when {
                error.message?.contains("network", ignoreCase = true) == true -> {
                    DebugUtils.logInfo(TAG, "Network error detected - will retry on next schedule")
                    AppLogger.logInfo(TAG, "Network error - scheduled retry")
                }
                
                error.message?.contains("database", ignoreCase = true) == true -> {
                    DebugUtils.logWarning(TAG, "Database error - attempting recovery")
                    AppLogger.logWarning(TAG, "Database error - recovery attempted")
                    
                    // Try to recover by clearing corrupted data if any
                    try {
                        sentenceRepository.clearDailyPool()
                    } catch (recoveryError: Exception) {
                        DebugUtils.logError(TAG, "Recovery failed", recoveryError)
                    }
                }
                
                else -> {
                    DebugUtils.logError(TAG, "Unknown error type - general recovery")
                    AppLogger.logError(TAG, "Unknown error - general recovery")
                }
            }
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error in error handling", e)
        }
    }
    
    /**
     * Perform periodic cleanup to maintain performance
     */
    private suspend fun performPeriodicCleanup() {
        try {
            // Only perform cleanup occasionally to avoid overhead
            if (System.currentTimeMillis() % (6 * 60 * 60 * 1000L) == 0L) { // Every 6 hours
                DebugUtils.logInfo(TAG, "Performing periodic cleanup")
                
                // Clean up old logs and temporary data
                OptimizationUtils.performComprehensiveCleanup(applicationContext, aggressive = false)
                
                // Generate health report for monitoring
                val healthReport = OptimizationUtils.generateHealthReport(applicationContext)
                if (healthReport.overallScore < 70) {
                    DebugUtils.logWarning(TAG, 
                        "App health score: ${healthReport.overallScore}. Recommendations: ${healthReport.recommendations}")
                }
            }
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error in periodic cleanup", e)
        }
    }
} 