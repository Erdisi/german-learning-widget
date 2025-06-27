package com.germanleraningwidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.widget.GermanLearningWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SentenceDeliveryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SentenceDeliveryWorker", "Starting work execution")
            
            val sentenceRepository = SentenceRepository.getInstance(applicationContext)
            val preferencesRepository = UserPreferencesRepository(applicationContext)
            
            // Get user preferences
            val preferences = preferencesRepository.userPreferences.first()
            android.util.Log.d("SentenceDeliveryWorker", "User preferences loaded: ${preferences.deliveryFrequency}")
            
            // Get a random sentence based on user preferences
            val sentence = sentenceRepository.getRandomSentence(
                level = preferences.germanLevel,
                topics = preferences.selectedTopics.toList()
            )
            
            sentence?.let {
                android.util.Log.d("SentenceDeliveryWorker", "Sentence loaded: ${it.germanText}")
                // Update widget
                updateWidget(it)
                
                android.util.Log.d("SentenceDeliveryWorker", "Work completed successfully")
                Result.success()
            } ?: run {
                android.util.Log.e("SentenceDeliveryWorker", "No sentence found")
                Result.failure()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SentenceDeliveryWorker", "Work failed", e)
            Result.failure()
        }
    }
    
    private fun updateWidget(sentence: com.germanleraningwidget.data.model.GermanSentence) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(applicationContext, GermanLearningWidget::class.java)
            )
            
            android.util.Log.d("SentenceDeliveryWorker", "Found ${appWidgetIds.size} widgets to update")
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(applicationContext, GermanLearningWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    putExtra("german_text", sentence.germanText)
                    putExtra("translation", sentence.translation)
                    putExtra("topic", sentence.topic)
                    putExtra("sentence_id", sentence.id)
                }
                
                applicationContext.sendBroadcast(intent)
                android.util.Log.d("SentenceDeliveryWorker", "Widget update broadcast sent")
            }
        } catch (e: Exception) {
            android.util.Log.e("SentenceDeliveryWorker", "Failed to update widget", e)
        }
    }
    
    companion object {
        private const val WORK_NAME = "sentence_delivery_work"
        
        fun scheduleWork(context: Context, frequency: DeliveryFrequency) {
            try {
                android.util.Log.d("SentenceDeliveryWorker", "Scheduling work for frequency: ${frequency.displayName}")
                
                val workManager = WorkManager.getInstance(context)
                
                // Cancel existing work
                workManager.cancelUniqueWork(WORK_NAME)
                
                // Handle 30-minute frequency with PeriodicWorkRequest (minimum 15 minutes)
                val workRequest = if (frequency == DeliveryFrequency.EVERY_30_MINUTES) {
                    // Use 30-minute periodic work
                    PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(
                        30, TimeUnit.MINUTES
                    ).setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    ).build()
                } else {
                    // Use hourly or longer intervals
                    PeriodicWorkRequestBuilder<SentenceDeliveryWorker>(
                        frequency.hours, TimeUnit.HOURS
                    ).setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    ).build()
                }
                
                // Enqueue unique periodic work
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
                
                android.util.Log.d("SentenceDeliveryWorker", "Work scheduled successfully for ${frequency.displayName}")
            } catch (e: Exception) {
                android.util.Log.e("SentenceDeliveryWorker", "Failed to schedule work", e)
                throw e
            }
        }
        
        fun cancelWork(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                android.util.Log.d("SentenceDeliveryWorker", "Work cancelled successfully")
            } catch (e: Exception) {
                android.util.Log.e("SentenceDeliveryWorker", "Failed to cancel work", e)
            }
        }
    }
} 