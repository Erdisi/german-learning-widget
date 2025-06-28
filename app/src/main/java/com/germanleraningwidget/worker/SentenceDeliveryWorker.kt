package com.germanleraningwidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.widget.GermanLearningWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Background worker for delivering German sentences to widgets.
 * 
 * This worker:
 * - Fetches user preferences
 * - Selects appropriate sentences based on user criteria
 * - Updates all German Learning Widgets
 * - Handles errors gracefully with retry policies
 * - Provides comprehensive logging for debugging
 * 
 * Thread Safety: Uses coroutines with proper context switching
 * Error Handling: Comprehensive error handling with retry logic
 * Performance: Efficient widget updates with timeout protection
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
        
        /**
         * Schedule periodic sentence delivery work.
         * 
         * @param context Application context
         * @param frequency Delivery frequency from user preferences
         */
        fun scheduleWork(context: Context, frequency: DeliveryFrequency) {
            try {
                Log.d(TAG, "Scheduling work for frequency: ${frequency.displayName}")
                
                val workManager = WorkManager.getInstance(context)
                
                // Cancel existing work to avoid duplicates
                workManager.cancelUniqueWork(WORK_NAME)
                
                val workRequest = createWorkRequest(frequency)
                
                // Enqueue unique periodic work
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
                
                Log.i(TAG, "Work scheduled successfully for ${frequency.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule work", e)
                throw WorkerException("Failed to schedule sentence delivery work", e)
            }
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
         */
        private fun createWorkRequest(frequency: DeliveryFrequency): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(true)
                .build()
            
            return if (frequency == DeliveryFrequency.EVERY_30_MINUTES) {
                PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        MIN_RETRY_DELAY_MS,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag(TAG)
                    .build()
            } else {
                PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(
                    frequency.hours, 
                    TimeUnit.HOURS
                ).setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        MIN_RETRY_DELAY_MS,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag(TAG)
                    .build()
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
            
            // Get user preferences with error handling
            val preferences = try {
                preferencesRepository.userPreferences.first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user preferences", e)
                return Result.failure(workDataOf("error" to "Failed to load preferences: ${e.message}"))
            }
            
            Log.d(TAG, "Loaded preferences: levels=${preferences.selectedGermanLevels}, primary=${preferences.primaryGermanLevel}, topics=${preferences.selectedTopics.size}")
            
            // Validate preferences
            if (preferences.selectedGermanLevels.isEmpty()) {
                Log.w(TAG, "No German levels selected, using default A1")
                return Result.failure(workDataOf("error" to "No German levels selected"))
            }
            
            if (preferences.selectedTopics.isEmpty()) {
                Log.w(TAG, "No topics selected, using default")
                return Result.failure(workDataOf("error" to "No topics selected"))
            }
            
            // Get random sentence using multi-level support
            val sentence = sentenceRepository.getRandomSentenceFromLevels(
                levels = preferences.selectedGermanLevels,
                topics = preferences.selectedTopics.toList()
            )
            
            sentence?.let { validSentence ->
                Log.d(TAG, "Selected sentence: ${validSentence.germanText} (ID: ${validSentence.id})")
                
                // Update widgets
                val updateResult = updateAllWidgets(validSentence)
                
                if (updateResult.isSuccess) {
                    Log.i(TAG, "Work completed successfully")
                    Result.success(workDataOf(
                        "sentence_id" to validSentence.id,
                        "sentence_text" to validSentence.germanText,
                        "widgets_updated" to updateResult.widgetCount
                    ))
                } else {
                    Log.e(TAG, "Widget update failed: ${updateResult.error}")
                    Result.failure(workDataOf("error" to updateResult.error))
                }
            } ?: run {
                Log.e(TAG, "No sentence found for levels ${preferences.selectedGermanLevels} and topics ${preferences.selectedTopics}")
                Result.failure(workDataOf("error" to "No matching sentences found"))
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
     * Update all German Learning Widgets with the new sentence.
     */
    private suspend fun updateAllWidgets(sentence: GermanSentence): WidgetUpdateResult {
        return withContext(Dispatchers.Main) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(applicationContext, GermanLearningWidget::class.java)
                )
                
                Log.d(TAG, "Found ${appWidgetIds.size} widgets to update")
                
                if (appWidgetIds.isEmpty()) {
                    Log.w(TAG, "No widgets found to update")
                    return@withContext WidgetUpdateResult.success(0)
                }
                
                // Create update intent with sentence data
                val intent = Intent(applicationContext, GermanLearningWidget::class.java).apply {
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
                
                Log.i(TAG, "Widget update broadcast sent to ${appWidgetIds.size} widgets")
                WidgetUpdateResult.success(appWidgetIds.size)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widgets", e)
                WidgetUpdateResult.failure("Widget update failed: ${e.message}")
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
     * Data class for widget update results.
     */
    private data class WidgetUpdateResult(
        val isSuccess: Boolean,
        val widgetCount: Int = 0,
        val error: String? = null
    ) {
        companion object {
            fun success(count: Int) = WidgetUpdateResult(true, count)
            fun failure(error: String) = WidgetUpdateResult(false, 0, error)
        }
    }
    
    /**
     * Custom exception for worker-related errors.
     */
    class WorkerException(message: String, cause: Throwable? = null) : Exception(message, cause)
} 