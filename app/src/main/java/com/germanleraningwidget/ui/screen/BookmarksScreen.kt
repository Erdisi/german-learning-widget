package com.germanleraningwidget.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.repository.SentenceRepository
import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sentenceRepository = remember { SentenceRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // Get saved sentences directly from repository
    val savedSentenceIds by sentenceRepository.getSavedSentenceIds().collectAsState(initial = emptySet())
    
    // Get the actual saved sentences - this will automatically update when savedSentenceIds changes
    var bookmarkedSentences by remember { mutableStateOf(listOf<GermanSentence>()) }
    
    // Update bookmarked sentences when saved IDs change
    LaunchedEffect(savedSentenceIds) {
        bookmarkedSentences = sentenceRepository.getSavedSentences()
        android.util.Log.d("BookmarksScreen", "Updated bookmarked sentences: ${bookmarkedSentences.map { "${it.id}: ${it.germanText}" }}")
    }
    
    android.util.Log.d("BookmarksScreen", "Saved sentence IDs: $savedSentenceIds")
    android.util.Log.d("BookmarksScreen", "Bookmarked sentences count: ${bookmarkedSentences.size}")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Saved Sentences",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${bookmarkedSentences.size} sentences saved",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (bookmarkedSentences.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "No saved sentences yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Bookmark sentences from your widget\nto save them here for later review",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = bookmarkedSentences,
                    key = { sentence -> sentence.id }
                ) { sentence ->
                    BookmarkedSentenceCard(
                        sentence = sentence,
                        onRemoveBookmark = {
                            scope.launch {
                                android.util.Log.d("BookmarksScreen", "Removing bookmark for sentence ${sentence.id}")
                                sentenceRepository.toggleSaveSentence(sentence)
                            }
                        }
                    )
                }
                
                // Bottom spacing for navigation bar
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BookmarkedSentenceCard(
    sentence: GermanSentence,
    onRemoveBookmark: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = sentence.germanText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = sentence.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = sentence.topic,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = sentence.level.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                IconButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Remove bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

 