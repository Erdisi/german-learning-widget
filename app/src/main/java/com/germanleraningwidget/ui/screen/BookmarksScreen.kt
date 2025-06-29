package com.germanleraningwidget.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.ui.theme.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * BookmarksScreen - Optimized bookmarks management interface
 * 
 * Enhanced Features:
 * - Improved state management with comprehensive error handling
 * - Performance optimizations with memoized filtering
 * - Enhanced accessibility and semantics
 * - Graceful error recovery and loading states
 * - Future-proof architecture with clean separation of concerns
 * - Memory-efficient operations
 */

/**
 * Unified UI State for BookmarksScreen with comprehensive state management
 */
@Stable
data class BookmarksScreenState(
    val isLoading: Boolean = false,
    val bookmarkedSentences: List<GermanSentence> = emptyList(),
    val filteredSentences: List<GermanSentence> = emptyList(),
    val selectedLevels: Set<String> = emptySet(),
    val selectedTopics: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedItems: Set<Long> = emptySet(),
    val showFilters: Boolean = false,
    val showSortMenu: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val error: String? = null,
    val isOperationInProgress: Boolean = false
) {
    val activeFilterCount: Int get() = selectedLevels.size + selectedTopics.size
    val hasFilters: Boolean get() = activeFilterCount > 0
    val isSelectionMode: Boolean get() = selectedItems.isNotEmpty()
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = bookmarkedSentences.isEmpty()
    val hasNoResults: Boolean get() = filteredSentences.isEmpty() && bookmarkedSentences.isNotEmpty()
}

/**
 * Actions for BookmarksScreen with type safety
 */
sealed class BookmarksAction {
    object ToggleFilters : BookmarksAction()
    object ToggleSortMenu : BookmarksAction()
    object ShowDeleteDialog : BookmarksAction()
    object HideDeleteDialog : BookmarksAction()
    object ClearSelection : BookmarksAction()
    object ClearFilters : BookmarksAction()
    object ClearError : BookmarksAction()
    data class UpdateSortOrder(val sortOrder: SortOrder) : BookmarksAction()
    data class ToggleLevel(val level: String) : BookmarksAction()
    data class ToggleTopic(val topic: String) : BookmarksAction()
    data class ToggleSelection(val sentenceId: Long) : BookmarksAction()
    data class ToggleBookmark(val sentence: GermanSentence) : BookmarksAction()
    data class DeleteSelected(val selectedIds: Set<Long>) : BookmarksAction()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Optimized repository handling with error recovery - Fixed: Use application context to prevent leaks
    val sentenceRepository = remember(context) { 
        SentenceRepository.getInstance(context.applicationContext) 
    }
    
    // Enhanced state management with error handling - Fixed: Use immutable state for better performance
    val savedSentenceIds by sentenceRepository.getSavedSentenceIds()
        .collectAsStateWithLifecycle(initialValue = emptySet())
    
    var bookmarkedSentences by remember { mutableStateOf(emptyList<GermanSentence>()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Enhanced filter state with better organization - Fixed: More efficient state management
    var selectedLevels by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedTopics by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // UI state with enhanced tracking - Fixed: Better memory management
    var selectedItems by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isOperationInProgress by remember { mutableStateOf(false) }
    
    // Memoized available filter options to prevent unnecessary recomputations - Optimized: Use constants
    val availableFilterOptions = remember {
        val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val topics = listOf(
            "Daily Life", "Food", "Travel", "Work", "Family", "Health", 
            "Education", "Technology", "Culture", "Sports", "Weather",
            "Entertainment", "Business", "Greetings", "Introductions", "Language"
        ).sorted()
        Pair(levels, topics)
    }
    val (availableLevels, availableTopics) = availableFilterOptions
    
    // Auto-hide error after delay - Fixed: Better null handling
    LaunchedEffect(error) {
        error?.let {
            kotlinx.coroutines.delay(5000)
            error = null
        }
    }
    
    // Update bookmarked sentences when saved IDs change with error handling - Optimized: Better error messages
    LaunchedEffect(savedSentenceIds) {
        try {
            isLoading = true
            error = null
            bookmarkedSentences = sentenceRepository.getSavedSentences()
        } catch (e: Exception) {
            error = when (e) {
                is java.net.UnknownHostException -> "No internet connection"
                is java.util.concurrent.TimeoutException -> "Request timed out"
                is kotlinx.coroutines.CancellationException -> "Operation was cancelled"
                else -> "Failed to load bookmarks: ${e.message}"
            }
            bookmarkedSentences = emptyList() // Safe fallback
        } finally {
            isLoading = false
        }
    }
    
    // Optimized filter logic with memoization - Fixed: Better performance and null safety
    val filteredSentences = remember(bookmarkedSentences, selectedLevels, selectedTopics, sortOrder) {
        if (bookmarkedSentences.isEmpty()) return@remember emptyList()
        
        try {
            var filtered = bookmarkedSentences
            
            // Apply level filter - Optimized: Use contains for better performance
            if (selectedLevels.isNotEmpty()) {
                filtered = filtered.filter { sentence -> 
                    sentence.level in selectedLevels
                }
            }
            
            // Apply topic filter - Optimized: Use contains for better performance
            if (selectedTopics.isNotEmpty()) {
                filtered = filtered.filter { sentence -> 
                    sentence.topic in selectedTopics
                }
            }
            
            // Apply sorting with optimized logic - Fixed: Null safety
            when (sortOrder) {
                SortOrder.DATE_DESC -> filtered.sortedByDescending { it.timestamp ?: 0L }
                SortOrder.DATE_ASC -> filtered.sortedBy { it.timestamp ?: 0L }
                SortOrder.GERMAN_AZ -> filtered.sortedBy { it.germanText }
                SortOrder.GERMAN_ZA -> filtered.sortedByDescending { it.germanText }
                SortOrder.LEVEL -> filtered.sortedBy { sentence ->
                    when (sentence.level) {
                        "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                        else -> 0
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback in case of sorting/filtering errors
            bookmarkedSentences
        }
    }
    
    // Memoized state calculations
    val activeFilterCount = remember(selectedLevels, selectedTopics) {
        selectedLevels.size + selectedTopics.size
    }
    val hasFilters = activeFilterCount > 0
    val isSelectionMode = selectedItems.isNotEmpty()
    val isEmpty = bookmarkedSentences.isEmpty()
    val hasNoResults = filteredSentences.isEmpty() && bookmarkedSentences.isNotEmpty()
    
    Scaffold(
        topBar = {
            OptimizedBookmarksTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedItems.size,
                totalCount = bookmarkedSentences.size,
                filteredCount = filteredSentences.size,
                hasFilters = hasFilters,
                activeFilterCount = activeFilterCount,
                showFilters = showFilters,
                showSortMenu = showSortMenu,
                sortOrder = sortOrder,
                onNavigateBack = onNavigateBack,
                onToggleFilters = { 
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showFilters = !showFilters 
                },
                onToggleSortMenu = { showSortMenu = !showSortMenu },
                onSortSelected = { 
                    sortOrder = it
                    showSortMenu = false
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onClearSelection = { 
                    selectedItems = emptySet()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDeleteSelected = { 
                    showDeleteDialog = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .semantics {
                    contentDescription = "Bookmarks screen with ${bookmarkedSentences.size} saved sentences"
                }
        ) {
            // Enhanced Filter Section with error handling
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                OptimizedFilterSection(
                    selectedLevels = selectedLevels,
                    selectedTopics = selectedTopics,
                    availableLevels = availableLevels,
                    availableTopics = availableTopics,
                    activeFilterCount = activeFilterCount,
                    onLevelToggled = { level ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedLevels = if (selectedLevels.contains(level)) {
                            selectedLevels - level
                        } else {
                            selectedLevels + level
                        }
                    },
                    onTopicToggled = { topic ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTopics = if (selectedTopics.contains(topic)) {
                            selectedTopics - topic
                        } else {
                            selectedTopics + topic
                        }
                    },
                    onClearFilters = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedLevels = emptySet()
                        selectedTopics = emptySet()
                    }
                )
            }
            
            // Error Banner with auto-dismiss
            AnimatedVisibility(
                visible = error != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                error?.let { errorMessage ->
                    OptimizedErrorBanner(
                        error = errorMessage,
                        onDismiss = { error = null }
                    )
                }
            }
            
            // Main Content with comprehensive state handling
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        OptimizedLoadingState()
                    }
                    isEmpty -> {
                        OptimizedEmptyBookmarksState(
                            onLearnMoreClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            }
                        )
                    }
                    hasNoResults -> {
                        OptimizedNoResultsState(
                            hasFilters = hasFilters,
                            onClearFilters = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedLevels = emptySet()
                                selectedTopics = emptySet()
                            }
                        )
                    }
                    else -> {
                        OptimizedBookmarksList(
                            sentences = filteredSentences,
                            selectedItems = selectedItems,
                            isSelectionMode = isSelectionMode,
                            onSelectionToggle = { sentence ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedItems = if (selectedItems.contains(sentence.id)) {
                                    selectedItems - sentence.id
                                } else {
                                    selectedItems + sentence.id
                                }
                            },
                            onBookmarkToggle = { sentence ->
                                scope.launch {
                                    try {
                                        isOperationInProgress = true
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sentenceRepository.toggleSaveSentence(sentence)
                                    } catch (e: Exception) {
                                        error = "Failed to remove bookmark: ${e.message}"
                                    } finally {
                                        isOperationInProgress = false
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Loading overlay for operations
                if (isOperationInProgress) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Enhanced delete functionality with better error handling
    fun deleteSelectedBookmarks() {
        if (selectedItems.isEmpty()) return
        
        scope.launch {
            try {
                isOperationInProgress = true
                
                // Optimized: Batch delete for better performance
                val sentencesToDelete = bookmarkedSentences.filter { 
                    it.id in selectedItems 
                }
                
                var successCount = 0
                var failureCount = 0
                
                sentencesToDelete.forEach { sentence ->
                    try {
                        sentenceRepository.toggleSaveSentence(sentence)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
                
                // Clear selection after operation
                selectedItems = emptySet()
                
                // Show result feedback
                when {
                    failureCount == 0 -> {
                        // All successful - no error message needed
                    }
                    successCount == 0 -> {
                        error = "Failed to delete bookmarks"
                    }
                    else -> {
                        error = "Deleted $successCount bookmarks, failed to delete $failureCount"
                    }
                }
                
            } catch (e: Exception) {
                error = when (e) {
                    is kotlinx.coroutines.CancellationException -> "Operation was cancelled"
                    else -> "Failed to delete bookmarks: ${e.message}"
                }
            } finally {
                isOperationInProgress = false
                showDeleteDialog = false
            }
        }
    }
    
    // Enhanced delete confirmation dialog
    if (showDeleteDialog) {
        OptimizedDeleteConfirmationDialog(
            count = selectedItems.size,
            onConfirm = {
                deleteSelectedBookmarks()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * Optimized Top Bar with enhanced state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizedBookmarksTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    filteredCount: Int,
    hasFilters: Boolean,
    activeFilterCount: Int,
    showFilters: Boolean,
    showSortMenu: Boolean,
    sortOrder: SortOrder,
    onNavigateBack: () -> Unit,
    onToggleFilters: () -> Unit,
    onToggleSortMenu: () -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { 
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.semantics {
                        contentDescription = "Clear selection"
                    }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            },
            actions = {
                IconButton(
                    onClick = onDeleteSelected,
                    modifier = Modifier.semantics {
                        contentDescription = "Delete $selectedCount selected bookmarks"
                    }
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    } else {
        TopAppBar(
            title = { 
                Text(
                    text = "Saved Sentences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate back"
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Filter button with badge
                Box {
                    IconButton(
                        onClick = onToggleFilters,
                        modifier = Modifier.semantics {
                            contentDescription = if (showFilters) "Hide filters" else "Show filters"
                        }
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeFilterCount > 0) {
                        Badge(
                            modifier = Modifier.offset(x = 6.dp, y = (-6).dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // Sort button with dropdown
                Box {
                    IconButton(
                        onClick = onToggleSortMenu,
                        modifier = Modifier.semantics {
                            contentDescription = "Sort options"
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = onToggleSortMenu
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            order.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            order.displayName,
                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = { onSortSelected(order) }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

/**
 * Optimized Filter Section with enhanced performance
 */
@Composable
private fun OptimizedFilterSection(
    selectedLevels: Set<String>,
    selectedTopics: Set<String>,
    availableLevels: List<String>,
    availableTopics: List<String>,
    activeFilterCount: Int,
    onLevelToggled: (String) -> Unit,
    onTopicToggled: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .semantics {
                contentDescription = "Filter options with $activeFilterCount active filters"
            },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 16.dp,
            hoveredElevation = 12.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Filter header with enhanced styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Filter Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (activeFilterCount > 0) {
                    FilledTonalButton(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Clear all $activeFilterCount filters"
                        }
                    ) {
                        Icon(
                            Icons.Filled.ClearAll,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Clear ($activeFilterCount)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // German Levels - Single row with horizontal scrolling
            Text(
                text = "German Levels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.semantics {
                    contentDescription = "German level filters"
                }
            ) {
                items(availableLevels) { level ->
                    FilterChip(
                        onClick = { onLevelToggled(level) },
                        label = { 
                            Text(
                                level,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = selectedLevels.contains(level),
                        leadingIcon = if (selectedLevels.contains(level)) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .semantics {
                                contentDescription = "$level level filter. ${if (selectedLevels.contains(level)) "Selected" else "Not selected"}"
                            }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Topics - Staggered grid with enhanced layout
            Text(
                text = "Topics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Fixed(3),
                horizontalItemSpacing = 16.dp,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .height(120.dp)
                    .semantics {
                        contentDescription = "Topic filters"
                    }
            ) {
                items(availableTopics) { topic ->
                    FilterChip(
                        onClick = { onTopicToggled(topic) },
                        label = { 
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        selected = selectedTopics.contains(topic),
                        leadingIcon = if (selectedTopics.contains(topic)) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .semantics {
                                contentDescription = "$topic topic filter. ${if (selectedTopics.contains(topic)) "Selected" else "Not selected"}"
                            }
                    )
                }
            }
        }
    }
}

/**
 * Enhanced Error Banner with dismiss functionality
 */
@Composable
private fun OptimizedErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Enhanced Loading State with better accessibility
 */
@Composable
private fun OptimizedLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Loading bookmarks"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Loading bookmarks...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced Empty Bookmarks State with improved UX
 */
@Composable
private fun OptimizedEmptyBookmarksState(
    onLearnMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "No bookmarks yet. Add bookmarks by learning German sentences"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Enhanced illustration
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No bookmarks yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Start learning German and bookmark your favorite sentences to build your personal collection.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onLearnMoreClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.semantics {
                contentDescription = "Start learning German"
            }
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Start Learning",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced No Results State for filtered content
 */
@Composable
private fun OptimizedNoResultsState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = if (hasFilters) "No sentences match your filters" else "No sentences found"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.FilterAltOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No sentences found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "No bookmarks match your current filters",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (hasFilters) {
            FilledTonalButton(
                onClick = onClearFilters,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Clear all filters"
                }
            ) {
                Icon(
                    Icons.Filled.FilterAltOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Clear Filters")
            }
        }
    }
}

/**
 * Enhanced Bookmarks List with performance optimizations
 */
@Composable
private fun OptimizedBookmarksList(
    sentences: List<GermanSentence>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    onSelectionToggle: (GermanSentence) -> Unit,
    onBookmarkToggle: (GermanSentence) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "${sentences.size} bookmarked sentences"
            },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = sentences,
            key = { it.id }
        ) { sentence ->
            OptimizedBookmarkCard(
                sentence = sentence,
                isSelected = selectedItems.contains(sentence.id),
                isSelectionMode = isSelectionMode,
                onSelectionToggle = { onSelectionToggle(sentence) },
                onBookmarkToggle = { onBookmarkToggle(sentence) }
            )
        }
        
        // Bottom padding for better UX
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

/**
 * Enhanced Bookmark Card with improved accessibility
 */
@Composable
private fun OptimizedBookmarkCard(
    sentence: GermanSentence,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelectionToggle: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    UnifiedCard(
        onClick = if (isSelectionMode) onSelectionToggle else null,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            .semantics {
                contentDescription = "German sentence: ${sentence.germanText}. Translation: ${sentence.translation}. Level: ${sentence.level}. Topic: ${sentence.topic}. ${if (isSelected) "Selected" else "Not selected"}"
            },
        colors = UnifiedDesign.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(UnifiedDesign.ContentPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // German text with enhanced styling
                    Text(
                        text = sentence.germanText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Translation with better contrast
                    Text(
                        text = sentence.translation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Enhanced tags row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level badge with improved styling
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = sentence.level,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Topic badge with enhanced colors
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = sentence.topic,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Enhanced action buttons
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Top
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = if (isSelected) "Selected for deletion" else "Not selected"
                            }
                        )
                    } else {
                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier.semantics {
                                contentDescription = "Remove bookmark"
                            }
                        ) {
                            Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Delete Confirmation Dialog with better UX
 */
@Composable
private fun OptimizedDeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Remove Bookmarks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to remove $count ${if (count == 1) "bookmark" else "bookmarks"}?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone, but you can always bookmark these sentences again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Confirm removal of $count bookmarks"
                }
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel removal"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Sort order enum with display information and icons
 */
enum class SortOrder(
    val displayName: String,
    val icon: ImageVector
) {
    DATE_DESC("Newest First", Icons.Filled.Schedule),
    DATE_ASC("Oldest First", Icons.Filled.History),
    GERMAN_AZ("German A-Z", Icons.Filled.SortByAlpha),
    GERMAN_ZA("German Z-A", Icons.Filled.SortByAlpha),
    LEVEL("By Level", Icons.Filled.School)
}

 