package com.germanleraningwidget.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.data.model.*
import com.germanleraningwidget.data.repository.AppSettingsRepository
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import com.germanleraningwidget.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Widget Details Customization Screen - SIMPLIFIED VERSION
 * 
 * Features:
 * - Background color selection with color palette
 * - Live preview of changes
 * - AUTOMATIC SAVING: Changes are saved immediately when made
 * - Real-time feedback with success/error messages
 * - Reset to defaults functionality
 * 
 * Simplified Approach:
 * - REMOVED: Text contrast options (now fixed at Normal)
 * - REMOVED: Sentences per day configuration (now fixed at 10/day)
 * - REMOVED: Update interval settings (now fixed at 90 minutes)
 * - REMOVED: Complex text sizing controls (auto-sizing only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetDetailsCustomizationScreen(
    widgetType: WidgetType,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Repositories
    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    val widgetCustomizationRepository = remember { WidgetCustomizationRepository.getInstance(context) }
    
    // State
    val appSettings by appSettingsRepository.appSettings.collectAsStateWithLifecycle(
        initialValue = AppSettings.createDefault()
    )
    val currentCustomization by widgetCustomizationRepository.getWidgetCustomization(widgetType).collectAsStateWithLifecycle(
        initialValue = WidgetCustomization.createDefault(widgetType)
    )
    
    // UI State for auto-save feedback
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Loading state for background color operations
    var isSavingBackground by remember { mutableStateOf(false) }
    
    // Auto-hide success messages after 2 seconds
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(2000)
            successMessage = null
        }
    }
    
    // Auto-hide error messages after 4 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(4000)
            errorMessage = null
        }
    }
    
    // Auto-save function for background color
    fun saveBackgroundColor(color: WidgetBackgroundColor) {
        scope.launch {
            isSavingBackground = true
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                
                val updatedCustomization = currentCustomization.copy(backgroundColor = color)
                val result = widgetCustomizationRepository.updateWidgetCustomization(updatedCustomization)
                
                if (result.isSuccess) {
                    // CRITICAL FIX: Pass the fresh customization data to ensure immediate update
                    com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateWidgetUpdateWithData(context, widgetType, updatedCustomization)
                    successMessage = "✅ Background color updated!"
                } else {
                    errorMessage = "❌ Failed to update background color"
                }
            } catch (e: Exception) {
                errorMessage = "❌ Error updating background: ${e.message}"
            } finally {
                isSavingBackground = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = widgetType.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSavingBackground) {
                            Text(
                                text = "Saving changes...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Auto-save indicator
                    if (isSavingBackground) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(UnifiedDesign.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap)
        ) {
            // Simplified Auto-Save Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(UnifiedDesign.ContentPadding)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Save Enabled: Changes are saved automatically",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Updates every 90 minutes • 10 sentences per day • Normal text contrast",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Widget Preview Section
            item {
                WidgetPreviewCard(
                    widgetType = widgetType,
                    customization = currentCustomization
                )
            }
            
            // Success/Error Messages
            successMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(UnifiedDesign.ContentPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            errorMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(UnifiedDesign.ContentPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Background Color Section
            item {
                BackgroundColorSection(
                    currentColor = currentCustomization.backgroundColor,
                    isLoading = isSavingBackground,
                    onColorSelected = { color ->
                        if (!isSavingBackground) {
                            saveBackgroundColor(color)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Widget Preview Card Component.
 */
@Composable
private fun WidgetPreviewCard(
    widgetType: WidgetType,
    customization: WidgetCustomization
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(UnifiedDesign.ContentPadding)
        ) {
            Text(
                text = "Live Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Widget Preview Box with accurate styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (widgetType == WidgetType.HERO) 180.dp else 140.dp)
                    .clip(RoundedCornerShape(if (widgetType == WidgetType.HERO) 20.dp else 16.dp))
                    .background(
                        // Use solid center color to match actual widget appearance
                        customization.backgroundColor.centerColor
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(if (widgetType == WidgetType.HERO) 20.dp else 16.dp)
                    )
                    .padding(UnifiedDesign.ContentPadding)
            ) {
                when (widgetType) {
                    WidgetType.MAIN -> MainWidgetPreview(customization)
                    WidgetType.BOOKMARKS -> BookmarksWidgetPreview(customization)
                    WidgetType.HERO -> HeroWidgetPreview(customization)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = widgetType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main Widget Preview Layout
 */
@Composable
private fun MainWidgetPreview(customization: WidgetCustomization) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎓 Learn German",
                fontSize = (14 * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                color = getContrastingTextColor(customization.backgroundColor.centerColor)
            )
            Text(
                text = "A1",
                fontSize = (12 * 0.9f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.7f),
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // German text (using automatic sizing)
        Text(
            text = "Guten Morgen!",
            fontSize = (18 * 0.9f).sp, // Preview uses standard size
            fontWeight = FontWeight.Bold,
            color = getContrastingTextColor(customization.backgroundColor.centerColor),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Translation (using automatic sizing)
        Text(
            text = "Good morning!",
            fontSize = (14 * 0.9f).sp, // Preview uses standard size
            color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Greetings",
                fontSize = (12 * 0.9f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.7f),
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♥",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
            }
        }
    }
}

/**
 * Bookmarks Widget Preview Layout
 */
@Composable
private fun BookmarksWidgetPreview(customization: WidgetCustomization) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💙 Bookmarks",
                fontSize = (14 * 0.9f).sp,
                fontWeight = FontWeight.Bold,
                color = getContrastingTextColor(customization.backgroundColor.centerColor)
            )
            Text(
                text = "1/5",
                fontSize = (12 * 0.9f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // German text (using automatic sizing)
        Text(
            text = "Wie geht's?",
            fontSize = (18 * 0.9f).sp, // Preview uses standard size
            fontWeight = FontWeight.Bold,
            color = getContrastingTextColor(customization.backgroundColor.centerColor),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Translation (using automatic sizing)
        Text(
            text = "How are you?",
            fontSize = (14 * 0.9f).sp, // Preview uses standard size
            color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations",
                fontSize = (12 * 0.9f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.7f),
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♥",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
            }
        }
    }
}

/**
 * Hero Widget Preview Layout
 */
@Composable
private fun HeroWidgetPreview(customization: WidgetCustomization) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⭐ Hero Bookmarks",
                fontSize = (16 * 0.8f).sp,
                fontWeight = FontWeight.Bold,
                color = getContrastingTextColor(customization.backgroundColor.centerColor)
            )
            Text(
                text = "1/5",
                fontSize = (14 * 0.8f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Preview dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "● ● ●",
                fontSize = 10.sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // German text (using automatic sizing)
            Text(
                text = "Schönen Tag!",
                fontSize = (22 * 0.7f).sp, // Preview uses standard size
                fontWeight = FontWeight.Bold,
                color = getContrastingTextColor(customization.backgroundColor.centerColor),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Translation (using automatic sizing)
            Text(
                text = "Have a nice day!",
                fontSize = (16 * 0.7f).sp, // Preview uses standard size
                color = getContrastingTextColor(customization.backgroundColor.centerColor).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Topic badge
            Text(
                text = "Daily Phrases",
                fontSize = (12 * 0.8f).sp,
                color = getContrastingTextColor(customization.backgroundColor.centerColor),
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 12.sp,
                        color = getContrastingTextColor(customization.backgroundColor.centerColor)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    fontSize = 14.sp,
                    color = getContrastingTextColor(customization.backgroundColor.centerColor)
                )
            }
        }
    }
}

/**
 * Background Color Selection Component.
 */
@Composable
private fun BackgroundColorSection(
    currentColor: WidgetBackgroundColor,
    isLoading: Boolean,
    onColorSelected: (WidgetBackgroundColor) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoading) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else CardDefaults.cardColors().containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (isLoading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isLoading) "Saving background color..." else "Choose a background color for your widget",
                style = MaterialTheme.typography.bodySmall,
                color = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Color Grid
            val colors = WidgetBackgroundColor.getAllColors()
            val chunkedColors = colors.chunked(5)
            
            chunkedColors.forEach { colorRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colorRow.forEach { color ->
                        ColorSelectionCircle(
                            color = color,
                            isSelected = color == currentColor,
                            isEnabled = !isLoading,
                            onClick = { if (!isLoading) onColorSelected(color) }
                        )
                    }
                    // Fill remaining spots in row if needed
                    repeat(5 - colorRow.size) {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Color Selection Circle Component.
 */
@Composable
private fun ColorSelectionCircle(
    color: WidgetBackgroundColor,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "scale_animation"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                // Use solid center color to match actual widget appearance
                if (isEnabled) color.centerColor else color.centerColor.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = getContrastingTextColor(color.centerColor).copy(alpha = if (isEnabled) 1f else 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Utility function to get contrasting text color.
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
    return if (luminance > 0.5) Color.Black else Color.White
}