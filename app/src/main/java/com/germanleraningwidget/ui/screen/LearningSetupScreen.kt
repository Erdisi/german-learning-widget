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

    
    // UI state
    var isAutoSaving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Auto-hide success message after 3 seconds
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
    }
    
    // Auto-hide error message after 5 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
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
                    selectedTopics != userPreferences.selectedTopics
    
    // Auto-save function
    fun autoSavePreferences() {
        if (!isFormValid || !hasChanges || isAutoSaving) return
        
        scope.launch {
            isAutoSaving = true
            errorMessage = null
            
            try {
                val updatedPreferences = UserPreferences(
                    selectedGermanLevels = selectedLevels.filter { it.isNotBlank() }.toSet(),
                    primaryGermanLevel = primaryLevel,
                    selectedTopics = selectedTopics.filter { it.isNotBlank() }.toSet(),
                    isOnboardingCompleted = true
                )
                
                if (!updatedPreferences.isValid()) {
                    errorMessage = "Invalid preferences. Please check your selections."
                    isAutoSaving = false
                    return@launch
                }
                
                preferencesRepository.updateUserPreferences(updatedPreferences)
                successMessage = "✅ Preferences saved automatically!"
                isAutoSaving = false
                
            } catch (e: Exception) {
                errorMessage = "❌ Failed to save preferences: ${e.message ?: "Unknown error"}"
                isAutoSaving = false
            }
        }
    }
    
    // Auto-save when valid changes are made
    LaunchedEffect(selectedLevels, primaryLevel, selectedTopics) {
        if (hasChanges && isFormValid) {
            // Add small delay to avoid too frequent saves
            kotlinx.coroutines.delay(500)
            autoSavePreferences()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Learning Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAutoSaving) {
                            Spacer(modifier = Modifier.width(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = UnifiedDesign.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap),
            contentPadding = PaddingValues(
                top = UnifiedDesign.ContentPadding,
                bottom = UnifiedDesign.ContentPadding
            )
        ) {
            // Auto-Save Info Banner
            item {
                ElevatedUnifiedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(UnifiedDesign.ContentPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Save Enabled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your learning preferences are saved automatically as you make changes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Success Message
            successMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
                    subtitle = "${selectedLevels.size} ${if (selectedLevels.size == 1) "level" else "levels"} selected · Primary: $primaryLevel",
                    isLoading = isAutoSaving
                ) {
                    GermanLevelSelector(
                        selectedLevels = selectedLevels,
                        primaryLevel = primaryLevel,
                        isLoading = isAutoSaving,
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
                    subtitle = "${selectedTopics.size} ${if (selectedTopics.size == 1) "topic" else "topics"} selected",
                    isLoading = isAutoSaving
                ) {
                    TopicsSelector(
                        selectedTopics = selectedTopics,
                        isLoading = isAutoSaving,
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
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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
    isLoading: Boolean,
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
                isLoading = isLoading,
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
    isLoading: Boolean,
    onToggle: () -> Unit,
    onSetPrimary: () -> Unit
) {
    UnifiedCard(
        onClick = if (isLoading) { {} } else { onToggle },
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
                onCheckedChange = if (isLoading) null else { { onToggle() } },
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLoading) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
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
                    color = if (isLoading) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected && !isPrimary) {
                TextButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isLoading
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
    isLoading: Boolean,
    onTopicToggled: (String) -> Unit
) {
    val chunkedTopics = com.germanleraningwidget.data.model.AvailableTopics.topics.chunked(3)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunkedTopics.forEach { rowTopics ->
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rowTopics) { topic ->
                    TopicChip(
                        topic = topic,
                        isSelected = selectedTopics.contains(topic),
                        isLoading = isLoading,
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
    isLoading: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = if (isLoading) { {} } else { onClick },
        label = { 
            Text(
                topic,
                color = if (isLoading) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ) 
        },
        selected = isSelected,
        enabled = !isLoading,
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

 