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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.data.model.*
import com.germanleraningwidget.data.repository.AppSettingsRepository
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import kotlinx.coroutines.launch

/**
 * Widget Details Customization Screen - Detailed customization for a specific widget.
 * 
 * Features:
 * - Background color selection with color palette
 * - German text size adjustment
 * - Translated text size adjustment
 * - Text contrast options
 * - Live preview of changes
 * - Immediate updates to home screen widgets
 * - Reset to defaults functionality
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
    
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var pendingCustomization by remember { mutableStateOf<WidgetCustomization?>(null) }
    
    // Track if there are unsaved changes
    val hasUnsavedChanges = pendingCustomization != null && pendingCustomization != currentCustomization
    
    // Auto-hide messages after 3 seconds
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
    }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
    }
    
    // Initialize pending customization when current customization loads
    LaunchedEffect(currentCustomization) {
        if (pendingCustomization == null) {
            pendingCustomization = currentCustomization
        }
    }
    
    // Function to apply changes to widgets
    fun applyChanges() {
        pendingCustomization?.let { customization ->
            scope.launch {
                isApplying = true
                try {
                    if (appSettings.hapticFeedbackEnabled) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    
                    val result = widgetCustomizationRepository.updateWidgetCustomization(customization)
                    if (result.isSuccess) {
                        successMessage = "✅ Widget updated successfully!"
                        pendingCustomization = customization // Sync pending with applied
                    } else {
                        errorMessage = "❌ Failed to update widget: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    errorMessage = "❌ Error updating widget: ${e.message}"
                } finally {
                    isApplying = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = widgetType.displayName,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (appSettings.hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
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
                    // Apply Changes Button
                    Button(
                        onClick = { applyChanges() },
                        enabled = hasUnsavedChanges && !isApplying,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasUnsavedChanges) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isApplying) "Applying..." else "Apply Changes",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    // Reset Button
                    IconButton(
                        onClick = {
                            if (appSettings.hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            showResetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset to Default"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Widget Preview Section
            item {
                WidgetPreviewCard(
                    widgetType = widgetType,
                    customization = pendingCustomization ?: currentCustomization
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
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Unsaved Changes Indicator
            if (hasUnsavedChanges) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You have unsaved changes. Tap 'Apply Changes' to update your widget.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            // Background Color Section
            item {
                BackgroundColorSection(
                    currentColor = (pendingCustomization ?: currentCustomization).backgroundColor,
                    onColorSelected = { color ->
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        pendingCustomization = (pendingCustomization ?: currentCustomization).copy(backgroundColor = color)
                    }
                )
            }
            
            // Text Size Sections
            item {
                TextSizeSection(
                    title = "German Text Size",
                    description = "Adjust the size of German text in the widget",
                    currentSize = (pendingCustomization ?: currentCustomization).germanTextSize,
                    onSizeSelected = { size ->
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        pendingCustomization = (pendingCustomization ?: currentCustomization).copy(germanTextSize = size)
                    }
                )
            }
            
            item {
                TextSizeSection(
                    title = "Translation Text Size",
                    description = "Adjust the size of translated text in the widget",
                    currentSize = (pendingCustomization ?: currentCustomization).translatedTextSize,
                    onSizeSelected = { size ->
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        pendingCustomization = (pendingCustomization ?: currentCustomization).copy(translatedTextSize = size)
                    }
                )
            }
            
            // Text Contrast Section
            item {
                TextContrastSection(
                    currentContrast = (pendingCustomization ?: currentCustomization).textContrast,
                    onContrastSelected = { contrast ->
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        pendingCustomization = (pendingCustomization ?: currentCustomization).copy(textContrast = contrast)
                    }
                )
            }
        }
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Widget to Default?") },
            text = { 
                Text("This will reset the ${widgetType.displayName} customization to default settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        isResetting = true
                        
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        
                        scope.launch {
                            try {
                                val result = widgetCustomizationRepository.resetWidgetToDefault(widgetType)
                                if (result.isSuccess) {
                                    successMessage = "✅ Widget reset to default settings"
                                    // Reset pending customization to default as well
                                    pendingCustomization = WidgetCustomization.createDefault(widgetType)
                                } else {
                                    errorMessage = "❌ Failed to reset widget: ${result.exceptionOrNull()?.message}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "❌ Error resetting widget: ${e.message}"
                            } finally {
                                isResetting = false
                            }
                        }
                    },
                    enabled = !isResetting
                ) { 
                    if (isResetting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Reset")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) { Text("Cancel") }
            }
        )
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
            modifier = Modifier.padding(16.dp)
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
                    .padding(16.dp)
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
        
        // German text
        Text(
            text = "Guten Morgen!",
            fontSize = (18 * customization.germanTextSize.scaleFactor * 0.9f).sp,
            fontWeight = FontWeight.Bold,
            color = getContrastingTextColor(customization.backgroundColor.centerColor),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Translation
        Text(
            text = "Good morning!",
            fontSize = (14 * customization.translatedTextSize.scaleFactor * 0.9f).sp,
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
            
            // Save button placeholder
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
                    text = "♡",
                    fontSize = 12.sp,
                    color = getContrastingTextColor(customization.backgroundColor.centerColor)
                )
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
                text = "📚 Bookmarks",
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
        
        // German text
        Text(
            text = "Wie geht's?",
            fontSize = (18 * customization.germanTextSize.scaleFactor * 0.9f).sp,
            fontWeight = FontWeight.Bold,
            color = getContrastingTextColor(customization.backgroundColor.centerColor),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Translation
        Text(
            text = "How are you?",
            fontSize = (14 * customization.translatedTextSize.scaleFactor * 0.9f).sp,
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
            // German text
            Text(
                text = "Schönen Tag!",
                fontSize = (22 * customization.germanTextSize.scaleFactor * 0.7f).sp,
                fontWeight = FontWeight.Bold,
                color = getContrastingTextColor(customization.backgroundColor.centerColor),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Translation
            Text(
                text = "Have a nice day!",
                fontSize = (16 * customization.translatedTextSize.scaleFactor * 0.7f).sp,
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
    onColorSelected: (WidgetBackgroundColor) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a background color for your widget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onClick = { onColorSelected(color) }
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
                color.centerColor
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = getContrastingTextColor(color.centerColor),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Text Size Selection Component.
 */
@Composable
private fun TextSizeSection(
    title: String,
    description: String,
    currentSize: WidgetTextSize,
    onSizeSelected: (WidgetTextSize) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Size Options
            WidgetTextSize.getAllSizes().forEach { size ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = size == currentSize,
                            onClick = { onSizeSelected(size) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = size == currentSize,
                        onClick = { onSizeSelected(size) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = size.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (size == currentSize) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = size.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Text Contrast Selection Component.
 */
@Composable
private fun TextContrastSection(
    currentContrast: WidgetTextContrast,
    onContrastSelected: (WidgetTextContrast) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Contrast,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Text Contrast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Adjust text contrast for better readability",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Contrast Options
            WidgetTextContrast.getAllContrasts().forEach { contrast ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = contrast == currentContrast,
                            onClick = { onContrastSelected(contrast) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = contrast == currentContrast,
                        onClick = { onContrastSelected(contrast) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = contrast.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (contrast == currentContrast) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = contrast.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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