package com.germanleraningwidget.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.widget.BookmarksHeroWidget
import com.germanleraningwidget.widget.BookmarksWidget
import com.germanleraningwidget.widget.GermanLearningWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Comprehensive widget debugging utility for testing all widget functionality.
 * 
 * Features:
 * - Widget installation verification
 * - Data flow testing
 * - Error detection and reporting
 * - Performance monitoring
 * - Manual widget updates
 */
object WidgetDebugUtils {
    
    private const val TAG = "WidgetDebugUtils"
    
    /**
     * Comprehensive widget debug report
     */
    data class WidgetDebugReport(
        val widgetType: WidgetType,
        val isInstalled: Boolean,
        val installedCount: Int,
        val lastUpdateStatus: String,
        val dataAvailable: Boolean,
        val hasValidPreferences: Boolean,
        val errorMessages: List<String>,
        val recommendations: List<String>
    )
    
    /**
     * Run comprehensive widget debugging for all widget types
     */
    fun runFullWidgetDebug(context: Context, showToasts: Boolean = true): List<WidgetDebugReport> {
        val reports = mutableListOf<WidgetDebugReport>()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Test all widget types
                val widgetTypes = listOf(WidgetType.MAIN, WidgetType.BOOKMARKS, WidgetType.HERO)
                
                for (widgetType in widgetTypes) {
                    val report = debugSingleWidget(context, widgetType)
                    reports.add(report)
                    
                    if (showToasts) {
                        withContext(Dispatchers.Main) {
                            val status = if (report.errorMessages.isEmpty()) "✅ OK" else "❌ Issues Found"
                            Toast.makeText(context, "${widgetType.displayName}: $status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                // Generate summary report
                val overallStatus = reports.all { it.errorMessages.isEmpty() }
                val totalWidgets = reports.sumOf { it.installedCount }
                
                DebugUtils.logInfo(TAG, "=== WIDGET DEBUG SUMMARY ===")
                DebugUtils.logInfo(TAG, "Overall Status: ${if (overallStatus) "HEALTHY" else "ISSUES FOUND"}")
                DebugUtils.logInfo(TAG, "Total Widgets Installed: $totalWidgets")
                
                reports.forEach { report ->
                    DebugUtils.logInfo(TAG, "${report.widgetType.displayName}: ${report.installedCount} installed, ${report.errorMessages.size} errors")
                    report.errorMessages.forEach { error ->
                        DebugUtils.logWarning(TAG, "  ERROR: $error")
                    }
                    report.recommendations.forEach { rec ->
                        DebugUtils.logInfo(TAG, "  RECOMMENDATION: $rec")
                    }
                }
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error running widget debug", e)
                if (showToasts) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Debug failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        return reports
    }
    
    /**
     * Debug a single widget type comprehensively
     */
    private suspend fun debugSingleWidget(context: Context, widgetType: WidgetType): WidgetDebugReport {
        val errors = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        try {
            // Check widget installation
            val (isInstalled, installedCount) = checkWidgetInstallation(context, widgetType)
            
            if (!isInstalled) {
                errors.add("Widget not installed on home screen")
                recommendations.add("Add ${widgetType.displayName} widget to home screen for testing")
            }
            
            // Check data availability
            val dataAvailable = checkDataAvailability(context, widgetType)
            if (!dataAvailable) {
                errors.add("No data available for widget display")
                recommendations.add("Ensure user has completed onboarding and has valid preferences")
            }
            
            // Check user preferences
            val hasValidPreferences = checkUserPreferences(context)
            if (!hasValidPreferences) {
                errors.add("User preferences are invalid or incomplete")
                recommendations.add("Complete learning setup with valid levels and topics")
            }
            
            // Test widget update mechanism
            val updateStatus = testWidgetUpdate(context, widgetType)
            
            // Additional widget-specific checks
            when (widgetType) {
                WidgetType.MAIN -> {
                    if (!hasValidPreferences) {
                        recommendations.add("Main widget requires valid German level and topic selection")
                    }
                }
                WidgetType.BOOKMARKS, WidgetType.HERO -> {
                    val hasBookmarks = checkBookmarksAvailable(context)
                    if (!hasBookmarks) {
                        errors.add("No bookmarked sentences available")
                        recommendations.add("Bookmark some sentences from the main widget first")
                    }
                }
            }
            
            return WidgetDebugReport(
                widgetType = widgetType,
                isInstalled = isInstalled,
                installedCount = installedCount,
                lastUpdateStatus = updateStatus,
                dataAvailable = dataAvailable,
                hasValidPreferences = hasValidPreferences,
                errorMessages = errors,
                recommendations = recommendations
            )
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error debugging ${widgetType.displayName}", e)
            errors.add("Debug error: ${e.message}")
            
            return WidgetDebugReport(
                widgetType = widgetType,
                isInstalled = false,
                installedCount = 0,
                lastUpdateStatus = "ERROR",
                dataAvailable = false,
                hasValidPreferences = false,
                errorMessages = errors,
                recommendations = recommendations
            )
        }
    }
    
    /**
     * Check if widget is installed on home screen
     */
    private fun checkWidgetInstallation(context: Context, widgetType: WidgetType): Pair<Boolean, Int> {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = when (widgetType) {
                WidgetType.MAIN -> ComponentName(context, GermanLearningWidget::class.java)
                WidgetType.BOOKMARKS -> ComponentName(context, BookmarksWidget::class.java)
                WidgetType.HERO -> ComponentName(context, BookmarksHeroWidget::class.java)
            }
            
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf()
            val count = widgetIds.size
            
            DebugUtils.logInfo(TAG, "${widgetType.displayName} widgets installed: $count")
            Pair(count > 0, count)
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error checking widget installation for ${widgetType.displayName}", e)
            Pair(false, 0)
        }
    }
    
    /**
     * Check if data is available for widget display
     */
    private suspend fun checkDataAvailability(context: Context, widgetType: WidgetType): Boolean {
        return try {
            val sentenceRepository = SentenceRepository.getInstance(context)
            
            when (widgetType) {
                WidgetType.MAIN -> {
                    // Check if sample sentences are available
                    val sampleSentences = SentenceRepository.SAMPLE_SENTENCES
                    sampleSentences.isNotEmpty()
                }
                WidgetType.BOOKMARKS, WidgetType.HERO -> {
                    // Check if bookmarked sentences are available
                    val bookmarkedSentences = sentenceRepository.getSavedSentences()
                    bookmarkedSentences.isNotEmpty()
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error checking data availability for ${widgetType.displayName}", e)
            false
        }
    }
    
    /**
     * Check if user preferences are valid
     */
    private suspend fun checkUserPreferences(context: Context): Boolean {
        return try {
            val preferencesRepository = UserPreferencesRepository(context)
            val preferences = preferencesRepository.userPreferences.first()
            
            val isValid = preferences.selectedGermanLevels.isNotEmpty() && 
                         preferences.selectedTopics.isNotEmpty() &&
                         preferences.isOnboardingCompleted
            
            DebugUtils.logInfo(TAG, "User preferences valid: $isValid")
            DebugUtils.logInfo(TAG, "  Levels: ${preferences.selectedGermanLevels}")
            DebugUtils.logInfo(TAG, "  Topics: ${preferences.selectedTopics}")
            DebugUtils.logInfo(TAG, "  Onboarding completed: ${preferences.isOnboardingCompleted}")
            
            isValid
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error checking user preferences", e)
            false
        }
    }
    
    /**
     * Check if bookmarked sentences are available
     */
    private suspend fun checkBookmarksAvailable(context: Context): Boolean {
        return try {
            val sentenceRepository = SentenceRepository.getInstance(context)
            val bookmarks = sentenceRepository.getSavedSentences()
            bookmarks.isNotEmpty()
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error checking bookmarks availability", e)
            false
        }
    }
    
    /**
     * Test widget update mechanism
     */
    private fun testWidgetUpdate(context: Context, widgetType: WidgetType): String {
        return try {
            when (widgetType) {
                WidgetType.MAIN -> {
                    GermanLearningWidget.updateAllWidgets(context)
                    "Update broadcast sent successfully"
                }
                WidgetType.BOOKMARKS -> {
                    BookmarksWidget.updateAllWidgets(context)
                    "Update broadcast sent successfully"
                }
                WidgetType.HERO -> {
                    BookmarksHeroWidget.updateAllWidgets(context)
                    "Update broadcast sent successfully"
                }
            }
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error testing widget update for ${widgetType.displayName}", e)
            "Update failed: ${e.message}"
        }
    }
    
    /**
     * Force update all widgets manually
     */
    fun forceUpdateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DebugUtils.logInfo(TAG, "Forcing update of all widgets...")
                
                // Update all widget types
                GermanLearningWidget.updateAllWidgets(context)
                BookmarksWidget.updateAllWidgets(context)
                BookmarksHeroWidget.updateAllWidgets(context)
                
                // Also trigger immediate refresh
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateAllWidgetUpdates(context)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "All widgets updated!", Toast.LENGTH_SHORT).show()
                }
                
                DebugUtils.logInfo(TAG, "All widgets updated successfully")
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error forcing widget updates", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Widget update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Test specific widget functionality
     */
    fun testWidgetFunctionality(context: Context, widgetType: WidgetType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DebugUtils.logInfo(TAG, "Testing ${widgetType.displayName} functionality...")
                
                when (widgetType) {
                    WidgetType.MAIN -> testMainWidgetFunctionality(context)
                    WidgetType.BOOKMARKS -> testBookmarksWidgetFunctionality(context)
                    WidgetType.HERO -> testHeroWidgetFunctionality(context)
                }
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error testing ${widgetType.displayName} functionality", e)
            }
        }
    }
    
    private suspend fun testMainWidgetFunctionality(context: Context) {
        // Test sentence loading
        val preferencesRepository = UserPreferencesRepository(context)
        val preferences = preferencesRepository.userPreferences.first()
        
        val sentenceRepository = SentenceRepository.getInstance(context)
        val sentence = sentenceRepository.getRandomSentenceFromLevels(
            levels = preferences.selectedGermanLevels,
            topics = preferences.selectedTopics
        )
        
        if (sentence != null) {
            DebugUtils.logInfo(TAG, "Main widget test - sentence loaded: ${sentence.germanText}")
            
            // Test bookmark functionality
            val wasBookmarked = sentenceRepository.isSentenceSaved(sentence.id)
            sentenceRepository.toggleSaveSentence(sentence)
            val isNowBookmarked = sentenceRepository.isSentenceSaved(sentence.id)
            
            DebugUtils.logInfo(TAG, "Main widget test - bookmark toggle: $wasBookmarked -> $isNowBookmarked")
        } else {
            DebugUtils.logWarning(TAG, "Main widget test - no sentence available")
        }
    }
    
    private suspend fun testBookmarksWidgetFunctionality(context: Context) {
        val sentenceRepository = SentenceRepository.getInstance(context)
        val bookmarks = sentenceRepository.getSavedSentences()
        
        DebugUtils.logInfo(TAG, "Bookmarks widget test - ${bookmarks.size} bookmarks available")
        
        if (bookmarks.isNotEmpty()) {
            // Test cycling through bookmarks
            DebugUtils.logInfo(TAG, "First bookmark: ${bookmarks.first().germanText}")
        }
    }
    
    private suspend fun testHeroWidgetFunctionality(context: Context) {
        val sentenceRepository = SentenceRepository.getInstance(context)
        val bookmarks = sentenceRepository.getSavedSentences()
        
        DebugUtils.logInfo(TAG, "Hero widget test - ${bookmarks.size} bookmarks for carousel")
        
        if (bookmarks.size >= 3) {
            DebugUtils.logInfo(TAG, "Hero widget test - sufficient bookmarks for carousel display")
        } else {
            DebugUtils.logWarning(TAG, "Hero widget test - recommend at least 3 bookmarks for optimal display")
        }
    }
    
    /**
     * Create test bookmark sentences for widget testing
     */
    fun createTestBookmarks(context: Context, count: Int = 5) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val sampleSentences = SentenceRepository.SAMPLE_SENTENCES.take(count)
                
                var bookmarkedCount = 0
                for (sentence in sampleSentences) {
                    if (!sentenceRepository.isSentenceSaved(sentence.id)) {
                        sentenceRepository.saveBookmark(sentence)
                        bookmarkedCount++
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Created $bookmarkedCount test bookmarks", Toast.LENGTH_SHORT).show()
                }
                
                DebugUtils.logInfo(TAG, "Created $bookmarkedCount test bookmark sentences")
                
                // Update bookmark widgets
                BookmarksWidget.updateAllWidgets(context)
                BookmarksHeroWidget.updateAllWidgets(context)
                
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error creating test bookmarks", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create test bookmarks: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Check widget performance and memory usage
     */
    fun checkWidgetPerformance(context: Context): String {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val runtime = Runtime.getRuntime()
            
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            val memoryUsagePercent = (usedMemory.toFloat() / maxMemory * 100).toInt()
            
            val performanceReport = buildString {
                appendLine("=== WIDGET PERFORMANCE REPORT ===")
                appendLine("Memory Usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB ($memoryUsagePercent%)")
                appendLine("Available Memory: ${(maxMemory - usedMemory) / 1024 / 1024}MB")
                
                // Check each widget type
                listOf(
                    Triple(WidgetType.MAIN, GermanLearningWidget::class.java, "german_learning_widget_info"),
                    Triple(WidgetType.BOOKMARKS, BookmarksWidget::class.java, "bookmarks_widget_info"),
                    Triple(WidgetType.HERO, BookmarksHeroWidget::class.java, "bookmarks_hero_widget_info")
                ).forEach { (type, clazz, info) ->
                    val componentName = ComponentName(context, clazz)
                    val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf()
                    appendLine("${type.displayName}: ${widgetIds.size} instances")
                }
            }
            
            DebugUtils.logInfo(TAG, performanceReport)
            performanceReport
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Error checking widget performance", e)
            "Performance check failed: ${e.message}"
        }
    }
} 