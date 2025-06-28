package com.germanleraningwidget.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.germanleraningwidget.data.model.*
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import com.germanleraningwidget.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningSetupScreen(
    userPreferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Form state
    var selectedLevels by remember { mutableStateOf(userPreferences.selectedGermanLevels) }
    var primaryLevel by remember { mutableStateOf(userPreferences.primaryGermanLevel) }
    var selectedTopics by remember { mutableStateOf(userPreferences.selectedTopics) }
    var selectedFrequency by remember { mutableStateOf(userPreferences.deliveryFrequency) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Auto-hide success after 2 seconds
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(2000)
            showSuccess = false
        }
    }
    
    // Form validation
    val isFormValid = selectedLevels.isNotEmpty() && 
                     selectedTopics.isNotEmpty() && 
                     primaryLevel.isNotBlank() &&
                     selectedLevels.contains(primaryLevel)
    
    // Check if preferences have changed
    val hasChanges = selectedLevels != userPreferences.selectedGermanLevels ||
                    primaryLevel != userPreferences.primaryGermanLevel ||
                    selectedTopics != userPreferences.selectedTopics ||
                    selectedFrequency != userPreferences.deliveryFrequency
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Learning Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = UnifiedDesign.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap),
                contentPadding = PaddingValues(
                    top = UnifiedDesign.ContentPadding,
                    bottom = 144.dp
                )
            ) {
                // Header Section
                item {
                    ElevatedUnifiedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(UnifiedDesign.ContentPadding)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Customize Your Learning",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (hasChanges) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.tertiary
                                    ) {}
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (hasChanges) {
                                    "You have unsaved changes. Tap Save to apply them."
                                } else {
                                    "Adjust settings to get the most relevant German sentences for your level and interests."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasChanges) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                
                // Error message if any
                errorMessage?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // German Level Section
                item {
                    SettingsSection(
                        title = "German Level",
                        subtitle = "${selectedLevels.size} ${if (selectedLevels.size == 1) "level" else "levels"} selected · Primary: $primaryLevel"
                    ) {
                        GermanLevelSelector(
                            selectedLevels = selectedLevels,
                            primaryLevel = primaryLevel,
                            onLevelToggled = { level ->
                                selectedLevels = if (selectedLevels.contains(level)) {
                                    selectedLevels.toMutableSet().apply { remove(level) }
                                } else {
                                    selectedLevels.toMutableSet().apply { add(level) }
                                }
                                errorMessage = null
                            },
                            onPrimaryLevelChanged = { level ->
                                if (selectedLevels.contains(level)) {
                                    primaryLevel = level
                                    errorMessage = null
                                } else {
                                    selectedLevels = selectedLevels.toMutableSet().apply { add(level) }
                                    primaryLevel = level
                                    errorMessage = null
                                }
                            }
                        )
                    }
                }
                
                // Topics Section
                item {
                    SettingsSection(
                        title = "Topics of Interest",
                        subtitle = "${selectedTopics.size} ${if (selectedTopics.size == 1) "topic" else "topics"} selected"
                    ) {
                        TopicsSelector(
                            selectedTopics = selectedTopics,
                            onTopicToggled = { topic ->
                                selectedTopics = if (selectedTopics.contains(topic)) {
                                    selectedTopics.toMutableSet().apply { remove(topic) }
                                } else {
                                    selectedTopics.toMutableSet().apply { add(topic) }
                                }
                                errorMessage = null
                            }
                        )
                    }
                }
                
                // Delivery Frequency Section
                item {
                    SettingsSection(
                        title = "Delivery Frequency",
                        subtitle = "How often to receive new sentences"
                    ) {
                        FrequencySelector(
                            selectedFrequency = selectedFrequency,
                            onFrequencySelected = { selectedFrequency = it }
                        )
                    }
                }
            }
            
            // Success Animation
            AnimatedVisibility(
                visible = showSuccess,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Preferences saved!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Floating Save Button
            AnimatedVisibility(
                visible = hasChanges && isFormValid && !showSuccess && !isLoading,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight }
                ) + fadeOut(
                    animationSpec = tween(200)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isLoading) return@FloatingActionButton
                        
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        
                        isLoading = true
                        errorMessage = null
                        
                        val updatedPreferences = UserPreferences(
                            selectedGermanLevels = selectedLevels.filter { it.isNotBlank() }.toSet(),
                            primaryGermanLevel = primaryLevel,
                            selectedTopics = selectedTopics.filter { it.isNotBlank() }.toSet(),
                            deliveryFrequency = selectedFrequency,
                            isOnboardingCompleted = true
                        )
                        
                        scope.launch {
                            try {
                                if (!updatedPreferences.isValid()) {
                                    errorMessage = "Invalid preferences. Please check your selections."
                                    isLoading = false
                                    return@launch
                                }
                                
                                preferencesRepository.updateUserPreferences(updatedPreferences)
                                
                                try {
                                    SentenceDeliveryWorker.scheduleWork(context, selectedFrequency)
                                } catch (workError: Exception) {
                                    // Don't fail the entire save operation if work scheduling fails
                                }
                                
                                isLoading = false
                                showSuccess = true
                            } catch (e: Exception) {
                                errorMessage = "Failed to save preferences: ${e.message ?: "Unknown error"}"
                                isLoading = false
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp,
                        hoveredElevation = 10.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Save preferences",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Save",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        content()
    }
}

@Composable
private fun GermanLevelSelector(
    selectedLevels: Set<String>,
    primaryLevel: String,
    onLevelToggled: (String) -> Unit,
    onPrimaryLevelChanged: (String) -> Unit
) {
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    
    Column(verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap)) {
        levels.forEach { level ->
            LevelCard(
                level = level,
                isSelected = selectedLevels.contains(level),
                isPrimary = level == primaryLevel,
                onToggle = { onLevelToggled(level) },
                onSetPrimary = { onPrimaryLevelChanged(level) }
            )
        }
        
        if (selectedLevels.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🎯 Multi-Level Learning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You'll receive sentences from all selected levels, with more focus on your primary level (${primaryLevel}). This helps reinforce basics while challenging you with advanced content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: String,
    isSelected: Boolean,
    isPrimary: Boolean,
    onToggle: () -> Unit,
    onSetPrimary: () -> Unit
) {
    UnifiedCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = UnifiedDesign.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UnifiedDesign.ContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Primary level",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = getLevelDescription(level),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected && !isPrimary) {
                TextButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "Set Primary",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicsSelector(
    selectedTopics: Set<String>,
    onTopicToggled: (String) -> Unit
) {
    val chunkedTopics = AvailableTopics.topics.chunked(3)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunkedTopics.forEach { rowTopics ->
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rowTopics) { topic ->
                    TopicChip(
                        topic = topic,
                        isSelected = selectedTopics.contains(topic),
                        onClick = { onTopicToggled(topic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicChip(
    topic: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(topic) },
        selected = isSelected,
        enabled = true,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        leadingIcon = {
            Text(
                text = getTopicEmoji(topic),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
private fun FrequencySelector(
    selectedFrequency: DeliveryFrequency,
    onFrequencySelected: (DeliveryFrequency) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap)) {
        DeliveryFrequency.values().forEach { frequency ->
            FrequencyCard(
                frequency = frequency,
                isSelected = selectedFrequency == frequency,
                onClick = { onFrequencySelected(frequency) }
            )
        }
    }
}

@Composable
private fun FrequencyCard(
    frequency: DeliveryFrequency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    UnifiedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = UnifiedDesign.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UnifiedDesign.ContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = frequency.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = getFrequencyDescription(frequency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getTopicEmoji(topic: String): String {
    return when (topic) {
        "Greetings" -> "👋"
        "Introductions" -> "🤝"
        "Daily Life" -> "🏠"
        "Food" -> "🍽️"
        "Travel" -> "✈️"
        "Weather" -> "🌤️"
        "Health" -> "🏥"
        "Work" -> "💼"
        "Education" -> "📚"
        "Technology" -> "💻"
        "Entertainment" -> "🎭"
        "Sports" -> "⚽"
        "Language" -> "🗣️"
        "Family" -> "👨‍👩‍👧‍👦"
        "Culture" -> "🎨"
        "Business" -> "📈"
        "Science" -> "🔬"
        "Politics" -> "🏛️"
        "Art" -> "🎨"
        else -> "📝" // Default fallback emoji
    }
}

private fun getLevelDescription(level: String): String {
    return when (level) {
        "A1" -> "Basic phrases and vocabulary"
        "A2" -> "Simple conversations and expressions"
        "B1" -> "Everyday situations and opinions"
        "B2" -> "Complex topics and discussions"
        "C1" -> "Academic and professional contexts"
        "C2" -> "Native-like proficiency"
        else -> "Unknown level"
    }
}

private fun getFrequencyDescription(frequency: DeliveryFrequency): String {
    return when (frequency) {
        DeliveryFrequency.EVERY_30_MINUTES -> "Most frequent learning sessions"
        DeliveryFrequency.EVERY_HOUR -> "Perfect for intensive learning"
        DeliveryFrequency.EVERY_2_HOURS -> "Regular learning rhythm"
        DeliveryFrequency.EVERY_4_HOURS -> "Good for busy schedules"
        DeliveryFrequency.EVERY_6_HOURS -> "Balanced learning pace"
        DeliveryFrequency.EVERY_12_HOURS -> "Twice daily practice"
        DeliveryFrequency.DAILY -> "Recommended for steady progress"
    }
} 