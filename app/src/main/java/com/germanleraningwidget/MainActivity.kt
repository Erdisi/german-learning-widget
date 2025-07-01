package com.germanleraningwidget

import android.content.Intent
import android.os.Bundle
import com.germanleraningwidget.util.DebugUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.germanleraningwidget.data.repository.AppSettingsRepository
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.di.AppModule
import com.germanleraningwidget.di.rememberRepositoryContainer
import com.germanleraningwidget.ui.screen.*
import com.germanleraningwidget.ui.theme.GermanLearningWidgetTheme
import com.germanleraningwidget.ui.viewmodel.OnboardingViewModel
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.launch

/**
 * Bottom Navigation destination configuration.
 * Immutable data class for type safety and consistency.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String = title
)

/**
 * Navigation configuration object.
 * Centralized navigation setup for easier maintenance.
 */
object NavigationConfig {
    const val ROUTE_ONBOARDING = "onboarding"
    const val ROUTE_HOME = "home"
    const val ROUTE_BOOKMARKS = "bookmarks"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_LEARNING_PREFERENCES = "learning_preferences"
    const val ROUTE_WIDGET_CUSTOMIZATION = "widget_customization"
    const val ROUTE_WIDGET_DETAILS = "widget_details"

    
    val bottomNavItems = listOf(
        BottomNavItem(
            route = ROUTE_HOME,
            title = "Learn",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            contentDescription = "Learn German"
        ),
        BottomNavItem(
            route = ROUTE_BOOKMARKS,
            title = "Saved",
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.BookmarkBorder,
            contentDescription = "Saved sentences"
        ),
        BottomNavItem(
            route = ROUTE_SETTINGS,
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            contentDescription = "Learning settings"
        )
    )
    
    val routesWithBottomNav = setOf(ROUTE_HOME, ROUTE_BOOKMARKS, ROUTE_SETTINGS)
}

/**
 * Main activity for the German Learning Widget app.
 * 
 * Features:
 * - Proper lifecycle management
 * - Intent handling for widget navigation
 * - Error handling and logging
 * - Theme application
 * 
 * Thread Safety: UI operations are main-thread safe
 * Error Handling: Graceful handling of initialization errors
 * Performance: Efficient composition and state management
 */
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for smooth launch experience (Android 12+)
        try {
            installSplashScreen()
        } catch (e: Exception) {
            DebugUtils.logWarning(TAG, "Splash screen not available on this Android version", e)
        }
        
        super.onCreate(savedInstanceState)
        
        setContent {
            val appSettingsRepository = remember { AppSettingsRepository(this@MainActivity) }
            val appSettings by appSettingsRepository.appSettings.collectAsStateWithLifecycle(
                initialValue = com.germanleraningwidget.data.model.AppSettings()
            )
            
            val darkTheme = when (appSettings.isDarkModeEnabled) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }
            
            GermanLearningWidgetTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GermanLearningApp()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DebugUtils.logInfo(TAG, "New intent received: ${intent.getStringExtra(EXTRA_NAVIGATE_TO)}")
        
        // Handle immediate widget navigation for new intents
        intent.getStringExtra(EXTRA_NAVIGATE_TO)?.let { navigationTarget ->
            when (navigationTarget) {
                "home", "bookmarks" -> {
                    DebugUtils.logInfo(TAG, "Handling immediate navigation to: $navigationTarget")
                    // The LaunchedEffect in GermanLearningApp will handle this
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        DebugUtils.logInfo(TAG, "MainActivity onDestroy")
    }
}


/**
 * Main app composable with navigation and state management.
 * 
 * Features:
 * - Centralized navigation logic
 * - Repository and ViewModel management
 * - Intent-based navigation handling
 * - Error boundary for composition errors
 * - Optimized performance without complex animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GermanLearningApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Error handling state
    var appError by remember { mutableStateOf<String?>(null) }
    
    // Handle widget navigation intents
    val activity = context as? MainActivity
    val navigateTo = activity?.intent?.getStringExtra(MainActivity.EXTRA_NAVIGATE_TO)
    
    // Initialize repositories with error handling using dependency injection
    val repositoryContainer = remember {
        try {
            AppModule.createRepositoryContainer(context)
        } catch (e: Exception) {
            DebugUtils.logError("Repository", "Failed to initialize repositories", e)
            appError = "Failed to initialize app: ${e.message}"
            null
        }
    }
    
    // Show error screen if initialization failed
    if (appError != null || repositoryContainer == null) {
        ErrorScreen(
            error = appError ?: "Failed to initialize app",
            onRetry = {
                appError = null
                // Restart activity
                (context as? ComponentActivity)?.recreate()
            }
        )
        return
    }
    
    // ViewModel with error handling
    val onboardingViewModel: OnboardingViewModel = viewModel {
        OnboardingViewModel(repositoryContainer.userPreferencesRepository)
    }
    
    // User preferences with lifecycle-aware collection
    val userPreferences by repositoryContainer.userPreferencesRepository.userPreferences
        .collectAsStateWithLifecycle(
            initialValue = com.germanleraningwidget.data.model.UserPreferences()
        )
    
    // Navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    // Determine start destination
    val startDestination = if (userPreferences.isOnboardingCompleted) {
        NavigationConfig.ROUTE_HOME
    } else {
        NavigationConfig.ROUTE_ONBOARDING
    }
    
    // Bottom navigation visibility
    val showBottomNav = remember(currentRoute) {
        NavigationConfig.routesWithBottomNav.contains(currentRoute)
    }
    
    // Handle widget navigation intents after user preferences are loaded
    LaunchedEffect(navigateTo, userPreferences.isOnboardingCompleted) {
        try {
            // Only handle widget navigation if onboarding is completed
            if (userPreferences.isOnboardingCompleted && !navigateTo.isNullOrEmpty()) {
                when (navigateTo) {
                    "bookmarks" -> {
                        navController.navigate(NavigationConfig.ROUTE_BOOKMARKS) {
                            popUpTo(NavigationConfig.ROUTE_HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    "home" -> {
                        navController.navigate(NavigationConfig.ROUTE_HOME) {
                            popUpTo(NavigationConfig.ROUTE_HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
                
                // Clear the intent after handling it
                activity?.intent?.removeExtra(MainActivity.EXTRA_NAVIGATE_TO)
            }
                        } catch (e: Exception) {
                    DebugUtils.logError("Navigation", "Failed to handle widget navigation", e)
            appError = "Navigation error: ${e.message}"
        }
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    items = NavigationConfig.bottomNavItems,
                    currentDestination = currentDestination,
                    onNavigate = { item ->
                        navigateToBottomNavDestination(navController, item, currentRoute)
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
            composable(NavigationConfig.ROUTE_ONBOARDING) {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingComplete = {
                        navController.navigate(NavigationConfig.ROUTE_HOME) {
                            popUpTo(NavigationConfig.ROUTE_ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_HOME) {
                HomeScreen(
                    userPreferences = userPreferences,
                    onNavigateToBookmarks = { 
                        navController.navigate(NavigationConfig.ROUTE_BOOKMARKS)
                    },
                    onNavigateToLearningSetup = { 
                        navController.navigate(NavigationConfig.ROUTE_LEARNING_PREFERENCES)
                    },
                    onNavigateToWidgetCustomization = {
                        navController.navigate(NavigationConfig.ROUTE_WIDGET_CUSTOMIZATION)
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_BOOKMARKS) {
                BookmarksScreen(
                    userPreferences = userPreferences,
                    onNavigateBack = { 
                        navigateBackToHome(navController)
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_SETTINGS) {
                SettingsScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = repositoryContainer.userPreferencesRepository,
                    onNavigateBack = { 
                        navigateBackToHome(navController)
                    },
                    onNavigateToLearningPreferences = {
                        navController.navigate(NavigationConfig.ROUTE_LEARNING_PREFERENCES)
                    },
                    onNavigateToWidgetCustomization = {
                        navController.navigate(NavigationConfig.ROUTE_WIDGET_CUSTOMIZATION)
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_LEARNING_PREFERENCES) {
                LearningSetupScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = repositoryContainer.userPreferencesRepository,
                    onNavigateBack = { 
                        navController.popBackStack()
                    }
                )
            }
            

            
            composable(NavigationConfig.ROUTE_WIDGET_CUSTOMIZATION) {
                WidgetCustomizationScreen(
                    onNavigateBack = { 
                        navController.popBackStack()
                    },
                    onNavigateToWidgetDetails = { widgetType ->
                        navController.navigate("${NavigationConfig.ROUTE_WIDGET_DETAILS}/${widgetType.key}")
                    }
                )
            }
            
            composable("${NavigationConfig.ROUTE_WIDGET_DETAILS}/{widgetTypeKey}") { backStackEntry ->
                val widgetTypeKey = backStackEntry.arguments?.getString("widgetTypeKey") ?: "main_widget"
                val widgetType = com.germanleraningwidget.data.model.WidgetType.fromKey(widgetTypeKey)
                
                WidgetDetailsCustomizationScreen(
                    widgetType = widgetType,
                    onNavigateBack = { 
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Handle onboarding completion with proper error handling.
 */
private fun handleOnboardingComplete(
    context: android.content.Context,
    navController: androidx.navigation.NavController
) {
    try {
        // Schedule work when onboarding is completed
        SentenceDeliveryWorker.scheduleWork(context)
                            DebugUtils.logInfo("Onboarding", "Work scheduled for daily delivery")
        
        navController.navigate(NavigationConfig.ROUTE_HOME) {
            popUpTo(NavigationConfig.ROUTE_ONBOARDING) { inclusive = true }
        }
                    } catch (e: Exception) {
                    DebugUtils.logError("Onboarding", "Failed to schedule work", e)
        // Still navigate to home even if scheduling fails
        navController.navigate(NavigationConfig.ROUTE_HOME) {
            popUpTo(NavigationConfig.ROUTE_ONBOARDING) { inclusive = true }
        }
    }
}

/**
 * Navigate to bottom navigation destination with proper state management.
 */
private fun navigateToBottomNavDestination(
    navController: androidx.navigation.NavController,
    item: BottomNavItem,
    currentRoute: String?
) {
    if (currentRoute != item.route) {
        navController.navigate(item.route) {
            // Pop up to home to maintain clean back stack
            popUpTo(NavigationConfig.ROUTE_HOME) {
                saveState = true
            }
            // Avoid multiple copies of same destination
            launchSingleTop = true
            // Restore state when reselecting previously selected item
            restoreState = true
        }
    }
}

/**
 * Navigate back to home screen safely.
 */
private fun navigateBackToHome(navController: androidx.navigation.NavController) {
    navController.navigate(NavigationConfig.ROUTE_HOME) {
        popUpTo(NavigationConfig.ROUTE_HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * Bottom navigation bar with improved accessibility and styling.
 */
@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onNavigate: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { 
                it.route == item.route 
            } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.contentDescription,
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

/**
 * Error screen for handling app-level errors.
 */
@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Retry")
        }
    }
}

// Repository container is now provided by AppModule dependency injection