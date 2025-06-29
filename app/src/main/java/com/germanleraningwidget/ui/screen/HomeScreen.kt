package com.germanleraningwidget.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.R
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

/**
 * HomeScreen - Main dashboard for German Learning Widget app
 * 
 * Optimized Features:
 * - Enhanced state management with comprehensive error handling
 * - Performance optimizations with memoization
 * - Comprehensive accessibility support
 * - Error recovery and loading states
 * - Future-proof architecture
 * - Memory-efficient repository handling
 */

@Composable
fun HomeScreen(
    userPreferences: UserPreferences,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToLearningSetup: () -> Unit,
    onNavigateToWidgetCustomization: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Optimized repository handling with error recovery - Fixed: Use application context to prevent leaks
    val sentenceRepository = remember(context) { 
        SentenceRepository.getInstance(context.applicationContext) 
    }
    
    // Enhanced state management with error handling - Fixed: Use rememberSaveable for error state
    val savedSentenceIds by sentenceRepository.getSavedSentenceIds()
        .collectAsStateWithLifecycle(initialValue = emptySet())
    
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Memoized validity check to prevent unnecessary recompositions - Optimized: Added error handling
    val isValidPreferences = remember(
        userPreferences.selectedGermanLevels,
        userPreferences.selectedTopics
    ) {
        try {
            userPreferences.selectedGermanLevels.isNotEmpty() && 
            userPreferences.selectedTopics.isNotEmpty()
        } catch (e: Exception) {
            false // Safe fallback
        }
    }
    
    // Auto-hide error after delay - Fixed: Check for null before setting delay
    LaunchedEffect(error) {
        error?.let {
            kotlinx.coroutines.delay(5000)
            error = null
        }
    }
    
    // Error recovery mechanism - Fixed: Removed redundant showErrorDialog state
    val hasError = error != null
    
    // Enhanced data loading with error handling - Optimized: Better error messages
    LaunchedEffect(savedSentenceIds) {
        try {
            isLoading = true
            error = null
            // Any additional loading operations can go here
        } catch (e: Exception) {
            error = when (e) {
                is java.net.UnknownHostException -> "No internet connection"
                is java.util.concurrent.TimeoutException -> "Request timed out"
                else -> "Failed to load data: ${e.message}"
            }
        } finally {
            isLoading = false
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .semantics {
                contentDescription = "Home screen with learning dashboard"
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 96.dp // Bottom navigation height
        )
    ) {
        // Welcome Header with enhanced state
        item("welcome_header") {
            OptimizedWelcomeHeaderCard(
                isValidPreferences = isValidPreferences,
                isLoading = isLoading,
                hasError = hasError
            )
        }
        
        // Widget Preview Section with error handling
        item("widget_preview") {
            OptimizedWidgetPreviewSection(
                onNavigateToWidgetCustomization = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToWidgetCustomization()
                },
                isEnabled = !isLoading
            )
        }
        
        // Learning Profile with enhanced data
        item("learning_profile") {
            OptimizedLearningProfileCard(
                userPreferences = userPreferences,
                bookmarkedCount = savedSentenceIds.size,
                isLoading = isLoading,
                onNavigateToLearningSetup = onNavigateToLearningSetup
            )
        }
        
        // Widget Setup Instructions
        item("widget_setup") {
            OptimizedWidgetSetupCard(
                onNavigateToWidgetCustomization = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToWidgetCustomization()
                },
                isEnabled = !isLoading
            )
        }
    }
}

/**
 * Optimized Welcome Header Card with enhanced state handling
 */
@Composable
private fun OptimizedWelcomeHeaderCard(
    isValidPreferences: Boolean,
    isLoading: Boolean,
    hasError: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isValidPreferences) {
                    "Welcome message: Ready to learn German today"
                } else {
                    "Welcome message: Setup required to start learning"
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                hasError -> {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error loading data",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.WavingHand,
                        contentDescription = "Welcome greeting icon",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        hasError -> "Welcome Back"
                        isLoading -> "Loading..."
                        else -> stringResource(R.string.welcome_greeting)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        hasError -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when {
                        hasError -> "Some features may be limited due to a connection issue"
                        isLoading -> "Setting up your learning environment..."
                        isValidPreferences -> stringResource(R.string.ready_to_learn_today)
                        else -> "Please complete your learning setup first"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        hasError -> MaterialTheme.colorScheme.error
                        isValidPreferences && !isLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            if (!isValidPreferences && !isLoading && !hasError) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Setup required warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Optimized Widget Preview Section with error handling
 */
@Composable
private fun OptimizedWidgetPreviewSection(
    onNavigateToWidgetCustomization: () -> Unit,
    isEnabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Available widgets section with preview cards"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Widgets,
                    contentDescription = "Widgets section icon",
                    modifier = Modifier.size(24.dp),
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.available_widgets),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = stringResource(R.string.daily_sentences_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Widget previews with memoized content
            val widgetTypes = remember { WidgetType.getAllTypes() }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.semantics {
                    contentDescription = "Horizontal scrollable list of widget previews"
                }
            ) {
                items(
                    items = widgetTypes,
                    key = { it.key }
                ) { widgetType ->
                    OptimizedWidgetPreviewCard(
                        widgetType = widgetType,
                        onClick = onNavigateToWidgetCustomization,
                        isEnabled = isEnabled
                    )
                }
            }
        }
    }
}

/**
 * Optimized Learning Profile Card with enhanced loading states
 */
@Composable
private fun OptimizedLearningProfileCard(
    userPreferences: UserPreferences,
    bookmarkedCount: Int,
    isLoading: Boolean,
    onNavigateToLearningSetup: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Learning profile with statistics and settings"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile section icon",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = stringResource(R.string.your_learning_profile),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Learning stats with optimized string building
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Memoized level display string
                val levelDisplayString = remember(
                    userPreferences.selectedGermanLevels,
                    userPreferences.primaryGermanLevel,
                    userPreferences.hasMultipleLevels
                ) {
                    if (userPreferences.hasMultipleLevels) {
                        "${userPreferences.selectedGermanLevels.size} levels (Primary: ${userPreferences.primaryGermanLevel})"
                    } else {
                        userPreferences.primaryGermanLevel
                    }
                }
                
                OptimizedLearningStatRow(
                    label = stringResource(R.string.level_label),
                    value = levelDisplayString,
                    isLoading = false
                )
                
                OptimizedLearningStatRow(
                    label = stringResource(R.string.topics_label),
                    value = stringResource(R.string.topics_selected, userPreferences.selectedTopics.size),
                    isLoading = false
                )
                

                
                OptimizedLearningStatRow(
                    label = "Bookmarks",
                    value = if (isLoading) "Loading..." else stringResource(R.string.bookmarks_count, bookmarkedCount),
                    isLoading = isLoading
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FilledTonalButton(
                onClick = onNavigateToLearningSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Edit learning preferences button"
                    },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.edit_preferences))
            }
        }
    }
}

/**
 * Optimized Widget Setup Card with enhanced accessibility
 */
@Composable
private fun OptimizedWidgetSetupCard(
    onNavigateToWidgetCustomization: () -> Unit,
    isEnabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Widget setup instructions and customization access"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add widget icon",
                        modifier = Modifier.size(24.dp),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = stringResource(R.string.add_your_widget),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                FilledTonalButton(
                    onClick = onNavigateToWidgetCustomization,
                    enabled = isEnabled,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Open widget customization screen"
                    }
                ) {
                    Text(stringResource(R.string.setup_button))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Setup instructions"
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quick_setup_instructions),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = stringResource(R.string.setup_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }
            }
        }
    }
}

/**
 * Optimized Widget Preview Card with enhanced error handling
 */
@Composable
private fun OptimizedWidgetPreviewCard(
    widgetType: WidgetType,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    val cardWidth = 280.dp
    val cardHeight = 160.dp
    
    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (widgetType) {
                WidgetType.MAIN -> OptimizedMainWidgetPreview(isEnabled)
                WidgetType.BOOKMARKS -> OptimizedBookmarksWidgetPreview(isEnabled)
                WidgetType.HERO -> OptimizedHeroWidgetPreview(isEnabled)
            }
        }
    }
    
    if (isEnabled) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .semantics {
                    contentDescription = "Preview of ${widgetType.displayName}. Tap to customize"
                },
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            cardContent()
        }
    } else {
        ElevatedCard(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .semantics {
                    contentDescription = "Preview of ${widgetType.displayName}. Loading"
                },
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            cardContent()
        }
    }
}

/**
 * Optimized Learning Stat Row with loading state
 */
@Composable
private fun OptimizedLearningStatRow(
    label: String,
    value: String,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label: $value"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Optimized widget preview components with enabled state and updated colors
@Composable
private fun OptimizedMainWidgetPreview(isEnabled: Boolean) {
    // Use actual Main Widget background color (Cream)
    val backgroundColor = Color(0xFFFFFCF2)
    val textColor = if (isEnabled) Color(0xFF2B2675) else Color(0xFF2B2675).copy(alpha = 0.6f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header similar to actual widget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎓 Learn German",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Surface(
                    color = textColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "A1",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // German text
            Text(
                text = stringResource(R.string.demo_german_greeting),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Translation
            Text(
                text = stringResource(R.string.demo_english_greeting),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = textColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.demo_topic_greetings),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
                
                Icon(
                    imageVector = Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark button",
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
            }
        }
    }
}

@Composable
private fun OptimizedBookmarksWidgetPreview(isEnabled: Boolean) {
    // Use actual Bookmarks Widget background color (Orange)
    val backgroundColor = Color(0xFFE88E3D)
    val textColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header similar to actual widget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 Bookmarks",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "1/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // German text
            Text(
                text = stringResource(R.string.demo_german_question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Translation
            Text(
                text = stringResource(R.string.demo_english_question),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.demo_topic_daily_life),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous sentence",
                        modifier = Modifier.size(14.dp),
                        tint = textColor.copy(alpha = 0.7f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next sentence",
                        modifier = Modifier.size(14.dp),
                        tint = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizedHeroWidgetPreview(isEnabled: Boolean) {
    // Use actual Hero Widget background color (Navy)
    val backgroundColor = Color(0xFF2B2675)
    val textColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header similar to actual widget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐ Hero Bookmarks",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "1/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Preview dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier.size(4.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (index == 1) {
                            textColor
                        } else {
                            textColor.copy(alpha = 0.4f)
                        }
                    ) {}
                    if (index < 2) Spacer(modifier = Modifier.width(3.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.demo_german_weather),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(3.dp))
                    
                    Text(
                        text = stringResource(R.string.demo_english_weather),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.demo_topic_weather),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Bottom navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "←",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.size(20.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor
                            )
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.size(22.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
} 