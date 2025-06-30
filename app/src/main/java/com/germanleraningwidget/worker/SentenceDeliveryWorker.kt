package com.germanleraningwidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import androidx.work.*

import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.model.WidgetCustomization
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import com.germanleraningwidget.widget.GermanLearningWidget
import com.germanleraningwidget.widget.BookmarksWidget
import com.germanleraningwidget.widget.BookmarksHeroWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import java.util.*

/**
 * Background worker for delivering German sentences to widgets with customizable frequency.
 * 
 * This worker:
 * - Fetches user preferences and widget customizations
 * - Selects appropriate sentences based on user criteria
 * - Updates widgets based on their individual sentences per day settings
 * - Schedules itself dynamically based on the most frequent widget requirement
 * - Handles errors gracefully with retry policies
 * - Provides comprehensive logging for debugging
 * 
 * Thread Safety: Uses coroutines with proper context switching
 * Error Handling: Comprehensive error handling with retry logic
 * Performance: Efficient widget updates with timeout protection
 * Scheduling: Dynamic scheduling based on widget customization settings
 */
class SentenceDeliveryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SentenceDeliveryWorker"
        private const val WORK_NAME = "sentence_delivery_work"
        private const val WORK_TIMEOUT_MS = 30_000L // 30 seconds
        private const val MIN_RETRY_DELAY_MS = 10_000L // 10 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        
        // Keys for tracking widget update schedules
        private const val LAST_UPDATE_KEY_PREFIX = "last_update_"
        
        /**
         * Schedule periodic sentence delivery work based on widget customizations.
         * Uses the most frequent update requirement among all widgets.
         * 
         * @param context Application context
         */
        fun scheduleWork(context: Context) {
            try {
                Log.d(TAG, "Scheduling work with dynamic frequency based on widget settings")
                
                val workManager = WorkManager.getInstance(context)
                
                // Cancel existing work to avoid duplicates
                workManager.cancelUniqueWork(WORK_NAME)
                
                // Schedule with dynamic interval calculation
                scheduleWorkWithDynamicInterval(context, workManager)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule work", e)
                throw WorkerException("Failed to schedule sentence delivery work", e)
            }
        }
        
        /**
         * Schedule work with dynamic interval calculation.
         * This is done asynchronously to avoid blocking the main thread.
         */
        private fun scheduleWorkWithDynamicInterval(context: Context, workManager: WorkManager) {
            // Schedule a one-time work to calculate the proper interval and reschedule
            val dynamicSchedulingWork = OneTimeWorkRequestBuilder<DynamicSchedulingWorker>()
                .addTag("${TAG}_dynamic_scheduling")
                .build()
                
            workManager.enqueue(dynamicSchedulingWork)
            
            // Also schedule with default interval as fallback
            val defaultWorkRequest = createWorkRequest(60) // Default 60 minutes
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                defaultWorkRequest
            )
            
            Log.i(TAG, "Work scheduled with dynamic interval calculation")
        }
        
        /**
         * Cancel all scheduled sentence delivery work.
         */
        fun cancelWork(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.i(TAG, "Work cancelled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel work", e)
            }
        }
        
        /**
         * Get work status for monitoring.
         */
        fun getWorkStatus(context: Context) = 
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME)
        
        /**
         * Create work request with appropriate constraints and retry policy.
         * 
         * @param intervalMinutes Update interval in minutes
         */
        internal fun createWorkRequest(intervalMinutes: Int): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(true)
                .build()
            
            // Ensure interval is at least 15 minutes (WorkManager minimum)
            val safeInterval = maxOf(intervalMinutes, 15)
            
            return PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(safeInterval.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    MIN_RETRY_DELAY_MS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG)
                .build()
        }
        
        /**
         * Force an immediate update of all widgets (useful for testing or immediate updates).
         */
        fun forceUpdate(context: Context) {
            try {
                val workManager = WorkManager.getInstance(context)
                val immediateWork = OneTimeWorkRequestBuilder<SentenceDeliveryWorker>()
                    .addTag("${TAG}_immediate")
                    .build()
                    
                workManager.enqueue(immediateWork)
                Log.i(TAG, "Immediate update requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to force update", e)
            }
        }
    }
    
    /**
     * Main work execution with timeout protection and comprehensive error handling.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting sentence delivery work (attempt ${runAttemptCount + 1})")
        
        // Add timeout protection
        val result = withTimeoutOrNull(WORK_TIMEOUT_MS) {
            executeWorkWithRetry()
        }
        
        result ?: run {
            Log.e(TAG, "Work timed out after ${WORK_TIMEOUT_MS}ms")
            Result.failure(workDataOf("error" to "Work execution timed out"))
        }
    }
    
    /**
     * Execute work with retry logic and detailed error reporting.
     */
    private suspend fun executeWorkWithRetry(): Result {
        return try {
            // Initialize repositories
            val sentenceRepository = SentenceRepository.getInstance(applicationContext)
            val preferencesRepository = UserPreferencesRepository(applicationContext)
            val widgetCustomizationRepository = WidgetCustomizationRepository.getInstance(applicationContext)
            
            // Get user preferences with error handling
            val preferences = try {
                preferencesRepository.userPreferences.first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user preferences", e)
                return Result.failure(workDataOf("error" to "Failed to load preferences: ${e.message}"))
            }
            
            // Get widget customizations
            val customizations = try {
                widgetCustomizationRepository.allWidgetCustomizations.first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load widget customizations", e)
                return Result.failure(workDataOf("error" to "Failed to load widget customizations: ${e.message}"))
            }
            
            Log.d(TAG, "Loaded preferences: levels=${preferences.selectedGermanLevels}, primary=${preferences.primaryGermanLevel}, topics=${preferences.selectedTopics.size}")
            Log.d(TAG, "Widget frequencies: Main=${customizations.mainWidget.sentencesPerDay}/day, Bookmarks=${customizations.bookmarksWidget.sentencesPerDay}/day, Hero=${customizations.heroWidget.sentencesPerDay}/day")
            
            // Validate preferences - don't proceed if no proper user setup
            if (preferences.selectedGermanLevels.isEmpty()) {
                Log.w(TAG, "No German levels selected. User needs to complete learning preferences setup.")
                return Result.failure(workDataOf("error" to "No German levels selected - user preferences incomplete"))
            }
            
            if (preferences.selectedTopics.isEmpty()) {
                Log.w(TAG, "No topics selected. User needs to complete learning preferences setup.")
                return Result.failure(workDataOf("error" to "No topics selected - user preferences incomplete"))
            }
            
            // Update widgets that are due for updates
            val updateResults = updateWidgetsDueForUpdate(
                sentenceRepository = sentenceRepository,
                preferences = preferences,
                customizations = customizations
            )
            
            if (updateResults.isNotEmpty()) {
                val successCount = updateResults.count { it.isSuccess }
                val totalCount = updateResults.size
                
                Log.i(TAG, "Work completed: $successCount/$totalCount widget types updated successfully")
                
                val outputData = workDataOf(
                    "widgets_processed" to totalCount,
                    "widgets_successful" to successCount,
                    "timestamp" to System.currentTimeMillis()
                )
                
                return if (successCount > 0) {
                    Result.success(outputData)
                } else {
                    Result.failure(outputData)
                }
            } else {
                Log.d(TAG, "No widgets were due for updates at this time")
                return Result.success(workDataOf(
                    "message" to "No widgets due for update",
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Work execution failed", e)
            
            // Determine if we should retry
            val shouldRetry = runAttemptCount < MAX_RETRY_ATTEMPTS && isRetryableError(e)
            
            if (shouldRetry) {
                Log.w(TAG, "Retrying work (attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                Result.retry()
            } else {
                Log.e(TAG, "Work failed permanently after ${runAttemptCount + 1} attempts")
                Result.failure(workDataOf(
                    "error" to e.message,
                    "attempts" to runAttemptCount + 1
                ))
            }
        }
    }
    
    /**
     * Update widgets that are due for updates based on their individual schedules.
     */
    private suspend fun updateWidgetsDueForUpdate(
        sentenceRepository: SentenceRepository,
        preferences: com.germanleraningwidget.data.model.UserPreferences,
        customizations: com.germanleraningwidget.data.model.AllWidgetCustomizations
    ): List<WidgetUpdateResult> {
        val results = mutableListOf<WidgetUpdateResult>()
        val currentTime = System.currentTimeMillis()
        
        // Check each widget type
        val widgetConfigs = listOf(
            WidgetConfig(WidgetType.MAIN, customizations.mainWidget, GermanLearningWidget::class.java),
            WidgetConfig(WidgetType.BOOKMARKS, customizations.bookmarksWidget, BookmarksWidget::class.java),
            WidgetConfig(WidgetType.HERO, customizations.heroWidget, BookmarksHeroWidget::class.java)
        )
        
        for (config in widgetConfigs) {
            if (isWidgetDueForUpdate(config.customization, currentTime)) {
                Log.d(TAG, "Updating ${config.widgetType.displayName} widget (${config.customization.sentencesPerDay} sentences/day)")
                
                // Get a sentence for this widget
                val sentence = sentenceRepository.getRandomSentenceFromLevels(
                    levels = preferences.selectedGermanLevels,
                    topics = preferences.selectedTopics.toList()
                )
                
                sentence?.let { validSentence ->
                    val updateResult = updateSpecificWidgetType(config, validSentence)
                    results.add(updateResult)
                    
                    // Record the update time
                    recordWidgetUpdate(config.widgetType, currentTime)
                } ?: run {
                    Log.w(TAG, "No sentence found for ${config.widgetType.displayName} widget")
                    results.add(WidgetUpdateResult.failure(config.widgetType, "No matching sentences found"))
                }
            } else {
                Log.d(TAG, "${config.widgetType.displayName} widget not due for update yet")
            }
        }
        
        return results
    }
    
    /**
     * Check if a widget is due for an update based on its customization settings.
     */
    private fun isWidgetDueForUpdate(customization: WidgetCustomization, currentTime: Long): Boolean {
        val lastUpdateKey = "${LAST_UPDATE_KEY_PREFIX}${customization.widgetType.key}"
        val lastUpdateTime = inputData.getLong(lastUpdateKey, 0L)
        
        if (lastUpdateTime == 0L) {
            // First run, update all widgets
            return true
        }
        
        val intervalMs = WidgetCustomization.getUpdateIntervalMinutes(customization.sentencesPerDay) * 60 * 1000L
        val nextUpdateTime = lastUpdateTime + intervalMs
        
        return currentTime >= nextUpdateTime
    }
    
    /**
     * Record when a widget was last updated.
     */
    private fun recordWidgetUpdate(widgetType: WidgetType, timestamp: Long) {
        // Note: In a real implementation, you'd want to persist this data
        // For now, we'll use SharedPreferences or a similar mechanism
        val prefs = applicationContext.getSharedPreferences("widget_updates", Context.MODE_PRIVATE)
        prefs.edit {
            putLong("${LAST_UPDATE_KEY_PREFIX}${widgetType.key}", timestamp)
        }
    }
    
    /**
     * Update a specific widget type with the given sentence.
     */
    private suspend fun updateSpecificWidgetType(
        config: WidgetConfig,
        sentence: GermanSentence
    ): WidgetUpdateResult {
        return withContext(Dispatchers.Main) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(applicationContext, config.widgetClass)
                ) ?: intArrayOf() // Handle potential null return
                
                Log.d(TAG, "Found ${appWidgetIds.size} ${config.widgetType.displayName} widgets to update")
                
                if (appWidgetIds.isEmpty()) {
                    Log.w(TAG, "No ${config.widgetType.displayName} widgets found to update")
                    return@withContext WidgetUpdateResult.success(config.widgetType, 0)
                }
                
                // Create update intent with sentence data
                val intent = Intent(applicationContext, config.widgetClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    putExtra("german_text", sentence.germanText)
                    putExtra("translation", sentence.translation)
                    putExtra("topic", sentence.topic)
                    putExtra("sentence_id", sentence.id)
                    putExtra("level", sentence.level)
                }
                
                // Send broadcast to update widgets
                applicationContext.sendBroadcast(intent)
                
                Log.i(TAG, "${config.widgetType.displayName} widget update broadcast sent to ${appWidgetIds.size} widgets")
                WidgetUpdateResult.success(config.widgetType, appWidgetIds.size)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update ${config.widgetType.displayName} widgets", e)
                WidgetUpdateResult.failure(config.widgetType, "Widget update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Determine if an error is retryable.
     */
    private fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException,
            is java.io.IOException -> true
            else -> false
        }
    }
    
    /**
     * Configuration for a widget type.
     */
    private data class WidgetConfig(
        val widgetType: WidgetType,
        val customization: WidgetCustomization,
        val widgetClass: Class<*>
    )
    
    /**
     * Data class for widget update results.
     */
    private data class WidgetUpdateResult(
        val widgetType: WidgetType,
        val isSuccess: Boolean,
        val widgetCount: Int = 0,
        val error: String? = null
    ) {
        companion object {
            fun success(widgetType: WidgetType, count: Int) = WidgetUpdateResult(widgetType, true, count)
            fun failure(widgetType: WidgetType, error: String) = WidgetUpdateResult(widgetType, false, 0, error)
        }
    }
    
    /**
     * Custom exception for worker-related errors.
     */
    class WorkerException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

/**
 * Worker responsible for dynamically scheduling the main SentenceDeliveryWorker
 * based on current widget customization settings.
 */
class DynamicSchedulingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "DynamicSchedulingWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Calculating optimal scheduling based on widget customizations")
            
            val widgetCustomizationRepository = WidgetCustomizationRepository.getInstance(applicationContext)
            val allCustomizations = widgetCustomizationRepository.allWidgetCustomizations.first()
            
            val minIntervalMinutes = minOf(
                WidgetCustomization.getUpdateIntervalMinutes(allCustomizations.mainWidget.sentencesPerDay),
                WidgetCustomization.getUpdateIntervalMinutes(allCustomizations.bookmarksWidget.sentencesPerDay),
                WidgetCustomization.getUpdateIntervalMinutes(allCustomizations.heroWidget.sentencesPerDay)
            )
            
            Log.i(TAG, "Calculated optimal interval: $minIntervalMinutes minutes")
            
            // Re-schedule the main worker with the calculated interval
            val workManager = WorkManager.getInstance(applicationContext)
            val workRequest = SentenceDeliveryWorker.createWorkRequest(minIntervalMinutes)
            
            workManager.enqueueUniquePeriodicWork(
                "sentence_delivery_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.i(TAG, "Successfully rescheduled main worker with ${minIntervalMinutes}-minute intervals")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate dynamic scheduling", e)
            Result.failure()
        }
    }
} 