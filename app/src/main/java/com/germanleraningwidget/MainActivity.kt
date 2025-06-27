package com.germanleraningwidget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
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
        // Install splash screen for smooth launch experience
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "MainActivity onCreate")
            
            setContent {
                GermanLearningWidgetTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AnimatedApp()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set content", e)
            // Don't crash - let the system handle it
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "New intent received: ${intent?.getStringExtra(EXTRA_NAVIGATE_TO)}")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity onPause")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
    }
}

/**
 * Animated wrapper for the main app with launch animations.
 */
@Composable
fun AnimatedApp() {
    var isVisible by remember { mutableStateOf(false) }
    
    // Launch animation
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Animated scale and fade for smooth app launch
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "app_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "app_alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
    ) {
        GermanLearningApp(alpha = alpha)
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
 * - Enhanced animations for smooth user experience
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GermanLearningApp(alpha: Float = 1f) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Error handling state
    var appError by remember { mutableStateOf<String?>(null) }
    
    // Handle widget navigation intents
    val activity = context as? MainActivity
    val navigateTo = activity?.intent?.getStringExtra(MainActivity.EXTRA_NAVIGATE_TO)
    
    LaunchedEffect(navigateTo) {
        try {
            when (navigateTo) {
                "bookmarks" -> {
                    navController.navigate(NavigationConfig.ROUTE_BOOKMARKS) {
                        popUpTo(NavigationConfig.ROUTE_HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                    Log.d("Navigation", "Navigated to bookmarks from widget")
                }
                "home" -> {
                    navController.navigate(NavigationConfig.ROUTE_HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                        launchSingleTop = true
                    }
                    Log.d("Navigation", "Navigated to home from widget")
                }
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Failed to handle widget navigation", e)
            appError = "Navigation error: ${e.message}"
        }
    }
    
    // Initialize repositories with error handling
    val repositoryState = remember {
        try {
            RepositoryContainer(
                sentenceRepository = SentenceRepository.getInstance(context),
                preferencesRepository = UserPreferencesRepository(context)
            )
        } catch (e: Exception) {
            Log.e("Repository", "Failed to initialize repositories", e)
            appError = "Failed to initialize app: ${e.message}"
            null
        }
    }
    
    // Show error screen if initialization failed
    if (appError != null || repositoryState == null) {
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
        OnboardingViewModel(repositoryState.preferencesRepository)
    }
    
    // User preferences with lifecycle-aware collection
    val userPreferences by repositoryState.preferencesRepository.userPreferences
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
    
    Scaffold(
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
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
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(NavigationConfig.ROUTE_ONBOARDING) {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingComplete = { frequency ->
                        handleOnboardingComplete(context, navController, frequency)
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
                        navController.navigate(NavigationConfig.ROUTE_SETTINGS)
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_BOOKMARKS) {
                BookmarksScreen(
                    onNavigateBack = { 
                        navigateBackToHome(navController)
                    }
                )
            }
            
            composable(NavigationConfig.ROUTE_SETTINGS) {
                LearningSetupScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = repositoryState.preferencesRepository,
                    onNavigateBack = { 
                        navigateBackToHome(navController)
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
    navController: androidx.navigation.NavController,
    frequency: com.germanleraningwidget.data.model.DeliveryFrequency
) {
    try {
        // Schedule work when onboarding is completed
        SentenceDeliveryWorker.scheduleWork(context, frequency)
        Log.i("Onboarding", "Work scheduled for frequency: ${frequency.displayName}")
        
        navController.navigate(NavigationConfig.ROUTE_HOME) {
            popUpTo(NavigationConfig.ROUTE_ONBOARDING) { inclusive = true }
        }
    } catch (e: Exception) {
        Log.e("Onboarding", "Failed to schedule work", e)
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

/**
 * Container for repositories to ensure proper initialization.
 */
private data class RepositoryContainer(
    val sentenceRepository: SentenceRepository,
    val preferencesRepository: UserPreferencesRepository
)