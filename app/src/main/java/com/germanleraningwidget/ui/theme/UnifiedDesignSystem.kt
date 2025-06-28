package com.germanleraningwidget.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Unified Design System for German Learning Widget
 * 
 * Implements consistent styling across all UI components with:
 * - Border radius: 16dp
 * - Padding: 20dp
 * - Subtle shadow effect
 * - Light border
 * - Clean white/surface background
 */
object UnifiedDesign {
    
    // Core design constants
    val BorderRadius = 16.dp
    val ContentPadding = 20.dp
    val ContentGap = 20.dp // Standard gap between elements
    val Elevation = 8.dp
    val BorderWidth = 1.dp
    
    // Colors
    val BorderColor = Color(0xFFE5E7EB) // rgb(229, 231, 235)
    val ShadowColor = Color(0x66E4E8F7) // rgba(228, 232, 247, 0.4)
    
    // Shape
    val CardShape = RoundedCornerShape(BorderRadius)
    
    /**
     * Standard card modifier with unified styling
     */
    fun Modifier.unifiedCard(
        backgroundColor: Color? = null
    ): Modifier = this
        .shadow(
            elevation = Elevation,
            shape = CardShape,
            ambientColor = ShadowColor,
            spotColor = ShadowColor
        )
        .border(
            width = BorderWidth,
            color = BorderColor,
            shape = CardShape
        )
    
    /**
     * Standard card colors with unified theming
     */
    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surface
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor
    )
    
    /**
     * Elevated card colors for special emphasis
     */
    @Composable
    fun elevatedCardColors(
        containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor
    )
    
    /**
     * Surface variant card colors for secondary content
     */
    @Composable
    fun surfaceVariantCardColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}

/**
 * Unified Card Composable
 * 
 * Standard card component with consistent styling applied
 */
@Composable
fun UnifiedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = UnifiedDesign.cardColors(),
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.unifiedCard(),
            shape = UnifiedDesign.CardShape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // We handle shadow in modifier
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.unifiedCard(),
            shape = UnifiedDesign.CardShape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // We handle shadow in modifier
        ) {
            content()
        }
    }
}

/**
 * Elevated Unified Card for special emphasis
 */
@Composable
fun ElevatedUnifiedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    UnifiedCard(
        modifier = modifier,
        onClick = onClick,
        colors = UnifiedDesign.elevatedCardColors(),
        content = content
    )
}

/**
 * Surface Variant Unified Card for secondary content
 */
@Composable
fun SurfaceVariantUnifiedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    UnifiedCard(
        modifier = modifier,
        onClick = onClick,
        colors = UnifiedDesign.surfaceVariantCardColors(),
        content = content
    )
}

/**
 * Extension function for applying unified card modifier
 */
fun Modifier.unifiedCard(backgroundColor: Color? = null): Modifier = 
    UnifiedDesign.run { unifiedCard(backgroundColor) } 