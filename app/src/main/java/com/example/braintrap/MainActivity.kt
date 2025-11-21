package com.example.braintrap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.braintrap.ui.screen.AchievementsScreen
import com.example.braintrap.ui.screen.AppSelectionScreen
import com.example.braintrap.ui.screen.DashboardScreen
import com.example.braintrap.ui.screen.OnboardingScreen
import com.example.braintrap.ui.screen.SettingsScreen
import com.example.braintrap.ui.screen.StatisticsScreen
import com.example.braintrap.ui.theme.BrainTrapTheme
import com.example.braintrap.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrainTrapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MindfulUsageApp(context = this@MainActivity)
                }
            }
        }
    }
}

@Composable
fun MindfulUsageApp(context: android.content.Context) {
    val navController = rememberNavController()
    val prefs = context.getSharedPreferences("mindful_usage_prefs", android.content.Context.MODE_PRIVATE)
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_complete", false)) }
    
    // Check if permissions are granted
    val hasAccessibility = PermissionManager.isAccessibilityServiceEnabled(
        context, 
        com.example.braintrap.service.AppBlockingService::class.java
    )
    val hasUsageStats = PermissionManager.isUsageStatsPermissionGranted(context)
    
    // Show onboarding if not completed or permissions missing
    if (showOnboarding || !hasAccessibility || !hasUsageStats) {
        OnboardingScreen(
            context = context,
            onComplete = {
                prefs.edit().putBoolean("onboarding_complete", true).apply()
                showOnboarding = false
            }
        )
        return
    }
    
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToAppSelection = {
                    navController.navigate("app_selection")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToStatistics = {
                    navController.navigate("statistics")
                },
                onNavigateToAchievements = {
                    navController.navigate("achievements")
                }
            )
        }
        
        composable("app_selection") {
            AppSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAchievements = {
                    navController.navigate("achievements")
                }
            )
        }
        
        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("achievements") {
            AchievementsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}