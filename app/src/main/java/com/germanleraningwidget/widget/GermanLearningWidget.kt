package com.germanleraningwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.germanleraningwidget.MainActivity
import com.germanleraningwidget.R
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GermanLearningWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_SAVE_SENTENCE = "com.germanleraningwidget.SAVE_SENTENCE"
        private const val EXTRA_WIDGET_ID = "widget_id"
        private const val EXTRA_SENTENCE_ID = "sentence_id"
        
        // Track current sentence for each widget instance
        private val currentSentences = mutableMapOf<Int, GermanSentence>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, GermanLearningWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, GermanLearningWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
        
        // Create intent to open the app when widget is tapped
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Load and display a sentence
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val preferencesRepository = UserPreferencesRepository(context)
                
                val preferences = preferencesRepository.userPreferences.first()
                val sentence = sentenceRepository.getRandomSentence(
                    level = preferences.germanLevel,
                    topics = preferences.selectedTopics.toList()
                )
                
                sentence?.let {
                    // Store the current sentence for this widget
                    currentSentences[appWidgetId] = it
                    
                    views.setTextViewText(R.id.widget_german_text, it.germanText)
                    views.setTextViewText(R.id.widget_translation, it.translation)
                    views.setTextViewText(R.id.widget_topic, it.topic)
                    views.setTextViewText(R.id.widget_level_indicator, preferences.germanLevel.displayName)
                    
                    // Set save button state
                    val isSaved = sentenceRepository.isSentenceSaved(it.id)
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    // Create save button click intent
                    val saveIntent = Intent(context, GermanLearningWidget::class.java).apply {
                        action = ACTION_SAVE_SENTENCE
                        putExtra(EXTRA_WIDGET_ID, appWidgetId)
                        putExtra(EXTRA_SENTENCE_ID, it.id)
                    }
                    val savePendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        saveIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_save_button, savePendingIntent)
                    
                    // Update the widget
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Show default text if there's an error
                views.setTextViewText(R.id.widget_german_text, "Guten Tag!")
                views.setTextViewText(R.id.widget_translation, "Good day!")
                views.setTextViewText(R.id.widget_topic, "Greetings")
                views.setTextViewText(R.id.widget_level_indicator, "A1")
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("GermanLearningWidget", "Widget received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_SAVE_SENTENCE -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                
                android.util.Log.d("GermanLearningWidget", "Save button clicked! Widget ID: $widgetId, Sentence ID: $sentenceId")
                
                if (widgetId != -1 && sentenceId != -1L) {
                    handleSaveSentence(context, widgetId, sentenceId)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                val germanText = intent.getStringExtra("german_text")
                val translation = intent.getStringExtra("translation")
                val topic = intent.getStringExtra("topic")
                val sentenceId = intent.getLongExtra("sentence_id", -1L)
                
                if (appWidgetIds != null && germanText != null && translation != null) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                        
                        // Create intent to open the app when widget is tapped
                        val openAppIntent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                        
                        // Update the text
                        views.setTextViewText(R.id.widget_german_text, germanText)
                        views.setTextViewText(R.id.widget_translation, translation)
                        views.setTextViewText(R.id.widget_topic, topic ?: "")
                        
                        // Set level indicator (get from preferences)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val preferencesRepository = UserPreferencesRepository(context)
                                val preferences = preferencesRepository.userPreferences.first()
                                views.setTextViewText(R.id.widget_level_indicator, preferences.germanLevel.displayName)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            } catch (e: Exception) {
                                views.setTextViewText(R.id.widget_level_indicator, "A1")
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                        
                        // Update save button state if we have a sentence ID
                        if (sentenceId != -1L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val sentenceRepository = SentenceRepository.getInstance(context)
                                    val isSaved = sentenceRepository.isSentenceSaved(sentenceId)
                                    
                                    views.setImageViewResource(
                                        R.id.widget_save_button,
                                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                                    )
                                    
                                    // Create save button click intent
                                    val saveIntent = Intent(context, GermanLearningWidget::class.java).apply {
                                        action = ACTION_SAVE_SENTENCE
                                        putExtra(EXTRA_WIDGET_ID, appWidgetId)
                                        putExtra(EXTRA_SENTENCE_ID, sentenceId)
                                    }
                                    val savePendingIntent = PendingIntent.getBroadcast(
                                        context,
                                        appWidgetId,
                                        saveIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    views.setOnClickPendingIntent(R.id.widget_save_button, savePendingIntent)
                                    
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun handleSaveSentence(context: Context, widgetId: Int, sentenceId: Long) {
        android.util.Log.d("GermanLearningWidget", "handleSaveSentence called for widget $widgetId, sentence $sentenceId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val currentSentence = currentSentences[widgetId]
                
                android.util.Log.d("GermanLearningWidget", "Current sentence: $currentSentence")
                
                if (currentSentence != null && currentSentence.id == sentenceId) {
                    val isNowSaved = sentenceRepository.toggleSaveSentence(currentSentence)
                    
                    android.util.Log.d("GermanLearningWidget", "Toggle result: $isNowSaved")
                    
                    // Update the widget button state
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Preserve existing content
                    views.setTextViewText(R.id.widget_german_text, currentSentence.germanText)
                    views.setTextViewText(R.id.widget_translation, currentSentence.translation)
                    views.setTextViewText(R.id.widget_topic, currentSentence.topic)
                    
                    // Update button state
                    val buttonResource = if (isNowSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    views.setImageViewResource(R.id.widget_save_button, buttonResource)
                    
                    android.util.Log.d("GermanLearningWidget", "Setting button resource: $buttonResource")
                    
                    // Create intent to open the app when widget is tapped
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    
                    // Recreate the save button click intent
                    val saveIntent = Intent(context, GermanLearningWidget::class.java).apply {
                        action = ACTION_SAVE_SENTENCE
                        putExtra(EXTRA_WIDGET_ID, widgetId)
                        putExtra(EXTRA_SENTENCE_ID, sentenceId)
                    }
                    val savePendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId,
                        saveIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_save_button, savePendingIntent)
                    
                    appWidgetManager.updateAppWidget(widgetId, views)
                    android.util.Log.d("GermanLearningWidget", "Widget updated successfully")
                } else {
                    android.util.Log.w("GermanLearningWidget", "Current sentence is null or ID doesn't match")
                }
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error in handleSaveSentence: ${e.message}", e)
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked sentences when widgets are deleted
        appWidgetIds.forEach { currentSentences.remove(it) }
    }
} 