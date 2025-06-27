package com.germanleraningwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.ui.screen.*
import com.germanleraningwidget.ui.theme.GermanLearningWidgetTheme
import com.germanleraningwidget.ui.viewmodel.OnboardingViewModel
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.launch

// Bottom Navigation destinations
data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

object NavigationItems {
    val items = listOf(
        BottomNavItem(
            route = "home",
            title = "Learn",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = "bookmarks",
            title = "Saved",
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.BookmarkBorder
        ),
        BottomNavItem(
            route = "settings",
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GermanLearningWidgetTheme {
                GermanLearningApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GermanLearningApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Handle navigation from widget
    val activity = context as? MainActivity
    val navigateTo = activity?.intent?.getStringExtra("navigate_to")
    
    LaunchedEffect(navigateTo) {
        if (navigateTo == "bookmarks") {
            navController.navigate("bookmarks") {
                popUpTo("home") { inclusive = false }
            }
        }
    }
    
    // Repositories
    val sentenceRepository = remember { SentenceRepository.getInstance(context) }
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    
    // ViewModel
    val onboardingViewModel: OnboardingViewModel = remember {
        OnboardingViewModel(preferencesRepository)
    }
    
    // Check if onboarding is completed
    val userPreferences by preferencesRepository.userPreferences.collectAsState(
        initial = com.germanleraningwidget.data.model.UserPreferences()
    )
    
    // Determine start destination based on onboarding status
    val startDestination = if (userPreferences.isOnboardingCompleted) "home" else "onboarding"
    
    // Current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Check if we should show bottom navigation
    val showBottomNav = remember(currentDestination?.route) {
        when (currentDestination?.route) {
            "onboarding" -> false
            "home", "bookmarks", "settings" -> true
            else -> false
        }
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    items = NavigationItems.items,
                    currentDestination = currentDestination,
                    onNavigate = { item ->
                        // Only navigate if not already on the selected route
                        if (currentDestination?.route != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to home to maintain a clean back stack
                                popUpTo("home") {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingComplete = { frequency ->
                        try {
                            // Schedule work when onboarding is completed
                            SentenceDeliveryWorker.scheduleWork(context, frequency)
                            
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            // Handle scheduling error gracefully
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                )
            }
            
            composable("home") {
                HomeScreen(
                    userPreferences = userPreferences,
                    onNavigateToBookmarks = { navController.navigate("bookmarks") },
                    onNavigateToLearningSetup = { navController.navigate("settings") }
                )
            }
            
            composable("bookmarks") {
                BookmarksScreen(
                    onNavigateBack = { 
                        // Navigate back to home instead of using navigateUp
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }
            
            composable("settings") {
                LearningSetupScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = preferencesRepository,
                    onNavigateBack = { 
                        // Navigate back to home instead of using navigateUp
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.route == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                alwaysShowLabel = true
            )
        }
    }
}