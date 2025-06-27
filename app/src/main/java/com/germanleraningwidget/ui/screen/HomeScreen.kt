package com.germanleraningwidget.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.UserPreferencesRepository

@Composable
fun HomeScreen(
    userPreferences: UserPreferences,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToLearningSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Top spacing
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Willkommen!",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ready to learn German today?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Learning Setup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Learning Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Learning stats in a clean grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingRow("Level", userPreferences.germanLevel.displayName)
                    SettingRow("Language", userPreferences.nativeLanguage)
                    SettingRow("Topics", "${userPreferences.selectedTopics.size} selected")
                    SettingRow("Delivery", userPreferences.deliveryFrequency.displayName)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = onNavigateToLearningSetup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Preferences")
                }
            }
        }
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "🏠 Add Your Widget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "1. Long-press your home screen\n" +
                            "2. Select 'Widgets'\n" +
                            "3. Find 'German Learning Widget'\n" +
                            "4. Start learning with daily sentences!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
        
        // Spacer to push content up from bottom nav
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
} 