package com.germanleraningwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.germanleraningwidget.MainActivity
import com.germanleraningwidget.R
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.repository.SentenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BookmarksWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_NEXT_BOOKMARK = "com.germanleraningwidget.ACTION_NEXT_BOOKMARK"
        private const val ACTION_REMOVE_BOOKMARK = "com.germanleraningwidget.ACTION_REMOVE_BOOKMARK"
        private const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val EXTRA_SENTENCE_ID = "extra_sentence_id"
        
        // Track current sentence index for each widget
        private val currentIndices = ConcurrentHashMap<Int, Int>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BookmarksWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BookmarksWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("BookmarksWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        android.util.Log.d("BookmarksWidget", "Updating bookmarks widget $appWidgetId")
        
        val views = RemoteViews(context.packageName, R.layout.widget_bookmarks)
        
        // Create intent to open the app when widget is tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "bookmarks")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bookmarks_container, pendingIntent)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                android.util.Log.d("BookmarksWidget", "Found ${bookmarkedSentences.size} bookmarked sentences")
                
                if (bookmarkedSentences.isEmpty()) {
                    // Show empty state
                    views.setTextViewText(R.id.widget_bookmarks_german_text, "No bookmarks yet")
                    views.setTextViewText(R.id.widget_bookmarks_translation, "Save sentences from the learning widget")
                    views.setTextViewText(R.id.widget_bookmarks_topic, "")
                    views.setTextViewText(R.id.widget_bookmarks_counter, "0/0")
                    
                    // Hide action buttons
                    views.setInt(R.id.widget_bookmarks_next_button, "setVisibility", android.view.View.GONE)
                    views.setInt(R.id.widget_bookmarks_remove_button, "setVisibility", android.view.View.GONE)
                } else {
                    // Get current index for this widget
                    val currentIndex = currentIndices.getOrDefault(appWidgetId, 0)
                    val validIndex = if (currentIndex >= bookmarkedSentences.size) 0 else currentIndex
                    currentIndices[appWidgetId] = validIndex
                    
                    val currentSentence = bookmarkedSentences[validIndex]
                    
                    // Update text content
                    views.setTextViewText(R.id.widget_bookmarks_german_text, currentSentence.germanText)
                    views.setTextViewText(R.id.widget_bookmarks_translation, currentSentence.translation)
                    views.setTextViewText(R.id.widget_bookmarks_topic, currentSentence.topic)
                    views.setTextViewText(R.id.widget_bookmarks_counter, "${validIndex + 1}/${bookmarkedSentences.size}")
                    
                    // Show action buttons
                    views.setInt(R.id.widget_bookmarks_next_button, "setVisibility", android.view.View.VISIBLE)
                    views.setInt(R.id.widget_bookmarks_remove_button, "setVisibility", android.view.View.VISIBLE)
                    
                    // Set up next button
                    val nextIntent = Intent(context, BookmarksWidget::class.java).apply {
                        action = ACTION_NEXT_BOOKMARK
                        putExtra(EXTRA_WIDGET_ID, appWidgetId)
                    }
                    val nextPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100, // Unique request code
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_bookmarks_next_button, nextPendingIntent)
                    
                    // Set up remove button
                    val removeIntent = Intent(context, BookmarksWidget::class.java).apply {
                        action = ACTION_REMOVE_BOOKMARK
                        putExtra(EXTRA_WIDGET_ID, appWidgetId)
                        putExtra(EXTRA_SENTENCE_ID, currentSentence.id)
                    }
                    val removePendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100 + 1, // Unique request code
                        removeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_bookmarks_remove_button, removePendingIntent)
                }
                
                // Update the widget on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    android.util.Log.d("BookmarksWidget", "Widget $appWidgetId updated successfully")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error updating widget $appWidgetId", e)
                
                // Show error state
                views.setTextViewText(R.id.widget_bookmarks_german_text, "Error loading bookmarks")
                views.setTextViewText(R.id.widget_bookmarks_translation, "Tap to open app")
                views.setTextViewText(R.id.widget_bookmarks_topic, "")
                views.setTextViewText(R.id.widget_bookmarks_counter, "")
                
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("BookmarksWidget", "Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_NEXT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handleNextBookmark(context, widgetId)
                }
            }
            ACTION_REMOVE_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                if (widgetId != -1 && sentenceId != -1L) {
                    handleRemoveBookmark(context, widgetId, sentenceId)
                }
            }
        }
    }
    
    private fun handleNextBookmark(context: Context, widgetId: Int) {
        android.util.Log.d("BookmarksWidget", "Next bookmark clicked for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0)
                    val nextIndex = (currentIndex + 1) % bookmarkedSentences.size
                    currentIndices[widgetId] = nextIndex
                    
                    android.util.Log.d("BookmarksWidget", "Moving to next bookmark: $nextIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error handling next bookmark", e)
            }
        }
    }
    
    private fun handleRemoveBookmark(context: Context, widgetId: Int, sentenceId: Long) {
        android.util.Log.d("BookmarksWidget", "Remove bookmark clicked for sentence $sentenceId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                val sentenceToRemove = bookmarkedSentences.find { it.id == sentenceId }
                
                if (sentenceToRemove != null) {
                    // Remove the bookmark
                    sentenceRepository.toggleSaveSentence(sentenceToRemove)
                    
                    // Adjust current index if needed
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0)
                    val newBookmarkedSentences = sentenceRepository.getSavedSentences()
                    
                    if (newBookmarkedSentences.isEmpty()) {
                        currentIndices[widgetId] = 0
                    } else if (currentIndex >= newBookmarkedSentences.size) {
                        currentIndices[widgetId] = 0
                    }
                    
                    android.util.Log.d("BookmarksWidget", "Bookmark removed, updating widget")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                    
                    // Also update the main learning widget and hero widget to reflect bookmark change
                    GermanLearningWidget.updateAllWidgets(context)
                    BookmarksHeroWidget.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error removing bookmark", e)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked indices when widgets are deleted
        appWidgetIds.forEach { currentIndices.remove(it) }
    }
} 