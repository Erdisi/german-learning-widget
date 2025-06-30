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
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.SentenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Hero-style bookmarks widget implementing Material Design 3 Hero carousel layout.
 * Features a prominent central bookmark with smaller preview items on the sides,
 * providing an elegant and immersive bookmark browsing experience.
 */
class BookmarksHeroWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_NEXT_BOOKMARK = "com.germanleraningwidget.hero.ACTION_NEXT_BOOKMARK"
        private const val ACTION_PREVIOUS_BOOKMARK = "com.germanleraningwidget.hero.ACTION_PREVIOUS_BOOKMARK"
        private const val ACTION_REMOVE_BOOKMARK = "com.germanleraningwidget.hero.ACTION_REMOVE_BOOKMARK"
        private const val ACTION_SELECT_BOOKMARK = "com.germanleraningwidget.hero.ACTION_SELECT_BOOKMARK"
        private const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val EXTRA_SENTENCE_ID = "extra_sentence_id"
        private const val EXTRA_TARGET_INDEX = "extra_target_index"
        
        // Track current sentence index for each widget
        private val currentIndices = ConcurrentHashMap<Int, Int>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BookmarksHeroWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf() // Handle potential null return
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BookmarksHeroWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("BookmarksHeroWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_bookmarks_hero)
                
                // Apply customizations using centralized helper
                val customization = WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.HERO, R.id.widget_hero_container
                )
                
                // Set up main container click
                setupMainClick(context, views, appWidgetId)
                
                // Load bookmarks with null safety
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksHeroWidget", "Error loading bookmarks", e)
                    emptyList()
                }
                
                if (bookmarkedSentences.isEmpty()) {
                    showEmptyState(views, customization)
                } else {
                    // Get current index for this widget
                    val currentIndex = currentIndices.getOrDefault(appWidgetId, 0)
                    val validIndex = if (currentIndex >= bookmarkedSentences.size) 0 else currentIndex.coerceAtLeast(0)
                    currentIndices[appWidgetId] = validIndex
                    
                    updateHeroContent(context, views, bookmarkedSentences, validIndex, appWidgetId, customization)
                }
                
                // Update the widget on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error updating widget $appWidgetId", e)
                showErrorState(context, appWidgetManager, appWidgetId)
            }
        }
    }
    
    /**
     * Set up main container click to open bookmarks screen.
     */
    private fun setupMainClick(context: Context, views: RemoteViews, appWidgetId: Int) {
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
        views.setOnClickPendingIntent(R.id.widget_hero_container, pendingIntent)
    }
    
    /**
     * Update hero content with current sentence and navigation.
     */
    private fun updateHeroContent(
        context: Context,
        views: RemoteViews,
        bookmarkedSentences: List<GermanSentence>,
        currentIndex: Int,
        appWidgetId: Int,
        customization: com.germanleraningwidget.data.model.WidgetCustomization
    ) {
        // Safe array access with bounds checking
        val currentSentence = bookmarkedSentences.getOrNull(currentIndex) ?: run {
            android.util.Log.w("BookmarksHeroWidget", "Invalid index $currentIndex for ${bookmarkedSentences.size} bookmarks")
            showEmptyState(views, customization)
            return
        }
        val totalCount = bookmarkedSentences.size
        
        // Update counter
        views.setTextViewText(R.id.widget_hero_counter, "${currentIndex + 1}/$totalCount")
        
        // Update main content
        views.setTextViewText(R.id.widget_hero_german_text, currentSentence.germanText)
        views.setTextViewText(R.id.widget_hero_translation, currentSentence.translation)
        views.setTextViewText(R.id.widget_hero_topic, currentSentence.topic)
        
        // Apply automatic text customizations for hero widget
        WidgetCustomizationHelper.applyAutoTextCustomizations(
            views, customization,
            R.id.widget_hero_german_text, R.id.widget_hero_translation,
            currentSentence.germanText, currentSentence.translation,
            isHeroWidget = true
        )
        
        // Update preview texts and dots
        updatePreviewTexts(views, bookmarkedSentences, currentIndex)
        updatePreviewDots(views, currentIndex, totalCount)
        
        // Set up navigation buttons
        setupNavigationButtons(context, views, appWidgetId, currentSentence.id)
    }
    
    /**
     * Set up navigation buttons for the hero widget.
     */
    private fun setupNavigationButtons(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        sentenceId: Long
    ) {
        // Previous button
        val prevIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_PREVIOUS_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_prev_button, prevPendingIntent)
        
        // Next button
        val nextIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_NEXT_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 1,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_next_button, nextPendingIntent)
        
        // Remove bookmark button
        val removeIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_REMOVE_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_SENTENCE_ID, sentenceId)
        }
        val removePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 2,
            removeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_remove_button, removePendingIntent)
    }
    
    /**
     * Show empty state when no bookmarks exist.
     */
    private fun showEmptyState(
        views: RemoteViews,
        customization: com.germanleraningwidget.data.model.WidgetCustomization
    ) {
        views.setTextViewText(R.id.widget_hero_counter, "0/0")
        views.setTextViewText(R.id.widget_hero_german_text, "No bookmarks yet")
        views.setTextViewText(R.id.widget_hero_translation, "Save sentences to see them here")
        views.setTextViewText(R.id.widget_hero_topic, "")
        
        // Apply automatic text customizations to empty state
        WidgetCustomizationHelper.applyAutoTextCustomizations(
            views, customization,
            R.id.widget_hero_german_text, R.id.widget_hero_translation,
            "No bookmarks yet", "Save sentences to see them here",
            isHeroWidget = true
        )
        
        // Clear preview texts and dots
        views.setTextViewText(R.id.widget_hero_prev_text, "")
        views.setTextViewText(R.id.widget_hero_next_text, "")
        views.setTextViewText(R.id.widget_hero_dots, "")
    }
    
    /**
     * Show error state.
     */
    private fun showErrorState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_bookmarks_hero)
                
                // Apply basic customization
                WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.HERO, R.id.widget_hero_container
                )
                
                views.setTextViewText(R.id.widget_hero_counter, "0/0")
                views.setTextViewText(R.id.widget_hero_german_text, "Error loading")
                views.setTextViewText(R.id.widget_hero_translation, "Tap to open app")
                views.setTextViewText(R.id.widget_hero_topic, "")
                views.setTextViewText(R.id.widget_hero_prev_text, "")
                views.setTextViewText(R.id.widget_hero_next_text, "")
                views.setTextViewText(R.id.widget_hero_dots, "")
                
                setupMainClick(context, views, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error showing error state", e)
            }
        }
    }
    
    /**
     * Update preview texts for adjacent bookmarks.
     */
    private fun updatePreviewTexts(
        views: RemoteViews,
        bookmarkedSentences: List<GermanSentence>,
        currentIndex: Int
    ) {
        val totalCount = bookmarkedSentences.size
        
        if (totalCount > 1) {
            // Previous text with safe array access
            val prevIndex = if (currentIndex == 0) totalCount - 1 else currentIndex - 1
            val prevSentence = bookmarkedSentences.getOrNull(prevIndex)
            val prevText = if (prevSentence != null) {
                prevSentence.germanText.take(20) + "..."
            } else {
                ""
            }
            views.setTextViewText(R.id.widget_hero_prev_text, prevText)
            
            // Next text with safe array access
            val nextIndex = if (currentIndex == totalCount - 1) 0 else currentIndex + 1
            val nextSentence = bookmarkedSentences.getOrNull(nextIndex)
            val nextText = if (nextSentence != null) {
                nextSentence.germanText.take(20) + "..."
            } else {
                ""
            }
            views.setTextViewText(R.id.widget_hero_next_text, nextText)
        } else {
            views.setTextViewText(R.id.widget_hero_prev_text, "")
            views.setTextViewText(R.id.widget_hero_next_text, "")
        }
    }
    
    private fun updatePreviewDots(views: RemoteViews, currentIndex: Int, totalCount: Int) {
        // Update progress dots display using text symbols
        val dotsText = when {
            totalCount <= 1 -> "●"
            totalCount <= 3 -> {
                (0 until totalCount).map { i ->
                    if (i == currentIndex) "●" else "○"
                }.joinToString("")
            }
            else -> {
                // Show position indicator for larger sets
                val position = ((currentIndex.toFloat() / (totalCount - 1)) * 4).toInt()
                (0..4).map { i ->
                    if (i == position) "●" else "○"
                }.joinToString("")
            }
        }
        views.setTextViewText(R.id.widget_hero_dots, dotsText)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("BookmarksHeroWidget", "Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_NEXT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handleNextBookmark(context, widgetId)
                }
            }
            ACTION_PREVIOUS_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handlePreviousBookmark(context, widgetId)
                }
            }
            ACTION_SELECT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val targetIndex = intent.getIntExtra(EXTRA_TARGET_INDEX, -1)
                if (widgetId != -1 && targetIndex != -1) {
                    handleSelectBookmark(context, widgetId, targetIndex)
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
        android.util.Log.d("BookmarksHeroWidget", "Next bookmark clicked for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksHeroWidget", "Error loading bookmarks in handleNextBookmark", e)
                    emptyList()
                }
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0).coerceAtLeast(0)
                    val nextIndex = ((currentIndex + 1) % bookmarkedSentences.size).coerceAtLeast(0)
                    currentIndices[widgetId] = nextIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Moving to next bookmark: $nextIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling next bookmark", e)
            }
        }
    }
    
    private fun handlePreviousBookmark(context: Context, widgetId: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Previous bookmark clicked for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksHeroWidget", "Error loading bookmarks in handlePreviousBookmark", e)
                    emptyList()
                }
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0).coerceAtLeast(0)
                    val prevIndex = if (currentIndex == 0) {
                        (bookmarkedSentences.size - 1).coerceAtLeast(0)
                    } else {
                        (currentIndex - 1).coerceAtLeast(0)
                    }
                    currentIndices[widgetId] = prevIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Moving to previous bookmark: $prevIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling previous bookmark", e)
            }
        }
    }
    
    private fun handleSelectBookmark(context: Context, widgetId: Int, targetIndex: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Select bookmark clicked: index $targetIndex for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                if (targetIndex >= 0 && targetIndex < bookmarkedSentences.size) {
                    currentIndices[widgetId] = targetIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Selected bookmark: $targetIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling select bookmark", e)
            }
        }
    }
    
    private fun handleRemoveBookmark(context: Context, widgetId: Int, sentenceId: Long) {
        android.util.Log.d("BookmarksHeroWidget", "Remove bookmark clicked for sentence $sentenceId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksHeroWidget", "Error loading bookmarks in handleRemoveBookmark", e)
                    emptyList()
                }
                val sentenceToRemove = bookmarkedSentences.find { it.id == sentenceId }
                
                if (sentenceToRemove != null) {
                    // Remove the bookmark
                    sentenceRepository.toggleSaveSentence(sentenceToRemove)
                    
                    // Adjust current index if needed
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0).coerceAtLeast(0)
                    val newBookmarkedSentences = try {
                        sentenceRepository.getSavedSentences() ?: emptyList()
                    } catch (e: Exception) {
                        android.util.Log.e("BookmarksHeroWidget", "Error loading new bookmarks", e)
                        emptyList()
                    }
                    
                    if (newBookmarkedSentences.isEmpty()) {
                        currentIndices[widgetId] = 0
                    } else if (currentIndex >= newBookmarkedSentences.size) {
                        currentIndices[widgetId] = 0
                    }
                    
                    android.util.Log.d("BookmarksHeroWidget", "Bookmark removed, updating widget")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                    
                    // Also update the main learning widget to reflect bookmark change
                    GermanLearningWidget.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error removing bookmark", e)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked indices when widgets are deleted
        appWidgetIds.forEach { currentIndices.remove(it) }
    }
} 