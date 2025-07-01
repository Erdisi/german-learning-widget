package com.germanleraningwidget.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.min

/**
 * Automatic text sizing utility for widgets.
 * Calculates optimal text sizes based on content length and available space.
 * 
 * Since Android widgets run in RemoteViews with limited measurement capabilities,
 * we use character-based heuristics and pre-calculated sizing formulas.
 * 
 * ENHANCED: Increased font sizes to take advantage of optimized layout with 
 * topic/bookmark elements moved to bottom, providing more space for text content.
 */
object AutoTextSizer {
    
    // Base text sizes (SP) - INCREASED for better visibility with more space
    private const val BASE_GERMAN_SIZE = 22f        // Increased from 18f
    private const val BASE_TRANSLATION_SIZE = 17f   // Increased from 14f
    private const val HERO_BASE_GERMAN_SIZE = 26f   // Increased from 22f
    private const val HERO_BASE_TRANSLATION_SIZE = 19f // Increased from 16f
    
    // Size constraints - EXPANDED ranges for better text scaling
    private const val MIN_GERMAN_SIZE = 16f         // Increased from 14f
    private const val MAX_GERMAN_SIZE = 30f         // Increased from 24f
    private const val MIN_TRANSLATION_SIZE = 13f    // Increased from 11f
    private const val MAX_TRANSLATION_SIZE = 22f    // Increased from 18f
    
    // Hero widget constraints - ENHANCED for prominence
    private const val MIN_HERO_GERMAN_SIZE = 20f    // Increased from 18f
    private const val MAX_HERO_GERMAN_SIZE = 34f    // Increased from 28f
    private const val MIN_HERO_TRANSLATION_SIZE = 16f // Increased from 14f
    private const val MAX_HERO_TRANSLATION_SIZE = 24f // Increased from 20f
    
    // Character length thresholds for scaling (unchanged - work well)
    private const val SHORT_TEXT_THRESHOLD = 15
    private const val MEDIUM_TEXT_THRESHOLD = 30
    private const val LONG_TEXT_THRESHOLD = 50
    private const val VERY_LONG_TEXT_THRESHOLD = 80
    
    /**
     * Calculate optimal text sizes for regular widgets.
     * ENHANCED: Now uses larger base sizes for better readability.
     */
    fun calculateTextSizes(
        germanText: String,
        translationText: String,
        baseGermanSize: Float = BASE_GERMAN_SIZE,
        baseTranslationSize: Float = BASE_TRANSLATION_SIZE
    ): TextSizes {
        val germanLength = germanText.length
        val translationLength = translationText.length
        
        // Use the longer text to determine scaling factor
        val maxLength = max(germanLength, translationLength)
        
        // Calculate scaling factor based on content length
        val scaleFactor = when {
            maxLength <= SHORT_TEXT_THRESHOLD -> 1.3f  // Short text - make it larger
            maxLength <= MEDIUM_TEXT_THRESHOLD -> 1.1f  // Medium text - slightly larger
            maxLength <= LONG_TEXT_THRESHOLD -> 1.0f    // Normal size
            maxLength <= VERY_LONG_TEXT_THRESHOLD -> 0.85f // Long text - smaller
            else -> 0.7f  // Very long text - much smaller
        }
        
        // Apply scaling with constraints
        val germanSize = (baseGermanSize * scaleFactor).coerceIn(MIN_GERMAN_SIZE, MAX_GERMAN_SIZE)
        val translationSize = (baseTranslationSize * scaleFactor).coerceIn(MIN_TRANSLATION_SIZE, MAX_TRANSLATION_SIZE)
        
        // Ensure German text is always larger than translation (minimum 3sp difference)
        val finalTranslationSize = min(translationSize, germanSize - 3f)
        
        return TextSizes(
            germanSize = germanSize,
            translationSize = max(finalTranslationSize, MIN_TRANSLATION_SIZE)
        )
    }
    
    /**
     * Calculate optimal text sizes for hero widgets (larger display).
     * ENHANCED: Now uses significantly larger base sizes for hero prominence.
     */
    fun calculateHeroTextSizes(
        germanText: String,
        translationText: String
    ): TextSizes {
        val germanLength = germanText.length
        val translationLength = translationText.length
        
        // Use the longer text to determine scaling factor
        val maxLength = max(germanLength, translationLength)
        
        // Hero widgets have more space, so we can be more generous with sizing
        val scaleFactor = when {
            maxLength <= SHORT_TEXT_THRESHOLD -> 1.2f  // Short text - larger
            maxLength <= MEDIUM_TEXT_THRESHOLD -> 1.0f  // Medium text - normal
            maxLength <= LONG_TEXT_THRESHOLD -> 0.9f    // Long text - slightly smaller
            maxLength <= VERY_LONG_TEXT_THRESHOLD -> 0.8f // Very long text - smaller
            else -> 0.65f  // Extremely long text - much smaller
        }
        
        // Apply scaling with hero widget constraints
        val germanSize = (HERO_BASE_GERMAN_SIZE * scaleFactor).coerceIn(MIN_HERO_GERMAN_SIZE, MAX_HERO_GERMAN_SIZE)
        val translationSize = (HERO_BASE_TRANSLATION_SIZE * scaleFactor).coerceIn(MIN_HERO_TRANSLATION_SIZE, MAX_HERO_TRANSLATION_SIZE)
        
        // Ensure German text is always larger than translation (minimum 4sp difference for hero)
        val finalTranslationSize = min(translationSize, germanSize - 4f)
        
        return TextSizes(
            germanSize = germanSize,
            translationSize = max(finalTranslationSize, MIN_HERO_TRANSLATION_SIZE)
        )
    }
    
    /**
     * Calculate text sizes with advanced heuristics considering word complexity.
     * ENHANCED: Works with the new larger base sizes and improved constraints.
     */
    fun calculateAdvancedTextSizes(
        germanText: String,
        translationText: String,
        isHeroWidget: Boolean = false
    ): TextSizes {
        // Count words and average word length for better sizing decisions
        val germanWords = germanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val translationWords = translationText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        val germanWordComplexity = if (germanWords.isNotEmpty()) {
            germanWords.sumOf { it.length } / germanWords.size.toFloat()
        } else 0f
        
        val translationWordComplexity = if (translationWords.isNotEmpty()) {
            translationWords.sumOf { it.length } / translationWords.size.toFloat()
        } else 0f
        
        // Adjust scaling based on word complexity
        val complexityFactor = when {
            germanWordComplexity > 8 || translationWordComplexity > 8 -> 0.9f  // Complex words - smaller
            germanWordComplexity > 6 || translationWordComplexity > 6 -> 0.95f // Medium complexity
            else -> 1.0f  // Simple words - normal
        }
        
        return if (isHeroWidget) {
            val baseSizes = calculateHeroTextSizes(germanText, translationText)
            TextSizes(
                germanSize = (baseSizes.germanSize * complexityFactor).coerceIn(MIN_HERO_GERMAN_SIZE, MAX_HERO_GERMAN_SIZE),
                translationSize = (baseSizes.translationSize * complexityFactor).coerceIn(MIN_HERO_TRANSLATION_SIZE, MAX_HERO_TRANSLATION_SIZE)
            )
        } else {
            val baseSizes = calculateTextSizes(germanText, translationText)
            TextSizes(
                germanSize = (baseSizes.germanSize * complexityFactor).coerceIn(MIN_GERMAN_SIZE, MAX_GERMAN_SIZE),
                translationSize = (baseSizes.translationSize * complexityFactor).coerceIn(MIN_TRANSLATION_SIZE, MAX_TRANSLATION_SIZE)
            )
        }
    }
    
    /**
     * Get sizing preview for customization screens.
     */
    fun getPreviewSizes(sampleGermanText: String, sampleTranslationText: String): TextSizes {
        return calculateAdvancedTextSizes(sampleGermanText, sampleTranslationText, false)
    }
    
    /**
     * Data class to hold calculated text sizes.
     */
    data class TextSizes(
        val germanSize: Float,
        val translationSize: Float
    ) {
        /**
         * Validate that German text is larger than translation text.
         */
        fun isValid(): Boolean = germanSize > translationSize
        
        /**
         * Get display description of the sizing.
         * UPDATED: Adjusted thresholds for new larger font sizes.
         */
        fun getDescription(): String {
            return when {
                germanSize >= 26f -> "Large text (${germanSize.toInt()}sp / ${translationSize.toInt()}sp)"
                germanSize >= 22f -> "Medium text (${germanSize.toInt()}sp / ${translationSize.toInt()}sp)"
                germanSize >= 18f -> "Standard text (${germanSize.toInt()}sp / ${translationSize.toInt()}sp)"
                else -> "Small text (${germanSize.toInt()}sp / ${translationSize.toInt()}sp)"
            }
        }
    }
} 