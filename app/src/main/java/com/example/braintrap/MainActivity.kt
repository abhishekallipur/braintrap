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
    
    private var hasCheckedFocusMode = false
    
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
    
    override fun onResume() {
        super.onResume()
        
        // Check focus mode only once per launch
        if (!hasCheckedFocusMode) {
            hasCheckedFocusMode = true
            checkFocusModeAccess()
        }
    }
    
    private fun checkFocusModeAccess() {
        // First check if focus mode is active
        val focusPrefs = getSharedPreferences("focus_mode_prefs", MODE_PRIVATE)
        val isFocusModeActive = focusPrefs.getBoolean("is_focus_mode_active", false)
        
        // If focus mode is NOT active, no need to verify
        if (!isFocusModeActive) {
            return
        }
        
        // Focus mode IS active - check unlock method
        val prefs = getSharedPreferences("braintrap_prefs", MODE_PRIVATE)
        val unlockMethod = prefs.getString("unlock_method", "math") ?: "math"
        
        // If NFC unlock method is set, require NFC verification to access BrainTrap
        if (unlockMethod == "nfc") {
            // Launch NFC verification
            val intent = android.content.Intent(this, com.example.braintrap.ui.NfcVerificationActivity::class.java)
            startActivityForResult(intent, 1001)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode != RESULT_OK) {
            // NFC not verified, close app
            finish()
        }
    }
}

@Composable
fun MindfulUsageApp(context: android.content.Context) {
    val navController = rememberNavController()
    val prefs = context.getSharedPreferences("mindful_usage_prefs", android.content.Context.MODE_PRIVATE)
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_complete", false)) }
    
    // Check if permissions are granted - recompose when permissions change
    var hasAccessibility by remember { 
        mutableStateOf(
            PermissionManager.isAccessibilityServiceEnabled(
                context, 
                com.example.braintrap.service.AppBlockingService::class.java
            )
        )
    }
    var hasUsageStats by remember { 
        mutableStateOf(PermissionManager.isUsageStatsPermissionGranted(context))
    }
    
    // Recheck permissions periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasAccessibility = PermissionManager.isAccessibilityServiceEnabled(
                context, 
                com.example.braintrap.service.AppBlockingService::class.java
            )
            hasUsageStats = PermissionManager.isUsageStatsPermissionGranted(context)
        }
    }
    
    // Show onboarding if not completed or permissions missing
    if (showOnboarding || !hasAccessibility || !hasUsageStats) {
        OnboardingScreen(
            context = context,
            onComplete = {
                prefs.edit().putBoolean("onboarding_complete", true).apply()
                showOnboarding = false
                // Force recheck permissions after onboarding
                hasAccessibility = PermissionManager.isAccessibilityServiceEnabled(
                    context, 
                    com.example.braintrap.service.AppBlockingService::class.java
                )
                hasUsageStats = PermissionManager.isUsageStatsPermissionGranted(context)
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
                },
                onNavigateToActivityGrid = {
                    navController.navigate("activity_grid")
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
        
        composable("activity_grid") {
            com.example.braintrap.ui.screen.ActivityGridScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}