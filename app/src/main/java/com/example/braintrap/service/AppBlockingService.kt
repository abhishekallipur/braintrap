package com.example.braintrap.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.data.repository.UsageStatsRepository
import com.example.braintrap.ui.BlockingActivity
import com.example.braintrap.ui.ChallengeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockingService : AccessibilityService() {
    
    @Inject
    lateinit var timeLimitRepository: TimeLimitRepository
    
    @Inject
    lateinit var usageStatsRepository: UsageStatsRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var isFocusModeActive = false
    private val blockedPackages = mutableSetOf<String>()
    private val temporaryWhitelist = mutableMapOf<String, Long>() // packageName to expiry time
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 0
        }
        setServiceInfo(info)
        
        // Load focus mode state from SharedPreferences
        val focusPrefs = getSharedPreferences("focus_mode_prefs", MODE_PRIVATE)
        isFocusModeActive = focusPrefs.getBoolean("is_focus_mode_active", false)
        
        instance = this
    }
    
    private val lastBlockedPackage = mutableSetOf<String>()
    private var lastBlockTime = 0L
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != this.packageName) {
                val currentTime = System.currentTimeMillis()
                
                // Check if app is temporarily whitelisted
                val whitelistExpiry = temporaryWhitelist[packageName]
                if (whitelistExpiry != null && currentTime < whitelistExpiry) {
                    // App is whitelisted, allow access
                    return
                }
                
                // Clean up expired whitelist entries and remove from lastBlockedPackage
                val expiredPackages = temporaryWhitelist.filter { it.value < currentTime }.keys
                expiredPackages.forEach { pkg ->
                    temporaryWhitelist.remove(pkg)
                    lastBlockedPackage.remove(pkg) // Allow re-blocking
                }
                
                // Prevent duplicate blocks for the same app within 1 second
                if (lastBlockedPackage.contains(packageName) && 
                    (currentTime - lastBlockTime) < 1000) {
                    return
                }
                
                // Check synchronously first for faster blocking
                if (isFocusModeActive && blockedPackages.contains(packageName)) {
                    // Immediate blocking for focus mode
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    lastBlockedPackage.add(packageName)
                    lastBlockTime = currentTime
                    serviceScope.launch {
                        kotlinx.coroutines.delay(300)
                        handleBlockedApp(packageName)
                    }
                    return
                }
                
                // For time-based blocking, check asynchronously
                serviceScope.launch {
                    if (isAppBlocked(packageName)) {
                        lastBlockedPackage.add(packageName)
                        lastBlockTime = currentTime
                        
                        // Immediately go back/home to prevent app from opening
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        
                        // Small delay then show challenge
                        kotlinx.coroutines.delay(300)
                        handleBlockedApp(packageName)
                    } else {
                        lastBlockedPackage.remove(packageName)
                    }
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // Service interrupted
    }
    
    private suspend fun isAppBlocked(packageName: String): Boolean {
        // Skip our own app
        if (packageName == this.packageName) {
            return false
        }
        
        if (isFocusModeActive) {
            return blockedPackages.contains(packageName)
        }
        
        val timeLimit = timeLimitRepository.getTimeLimit(packageName)
        if (timeLimit == null || !timeLimit.isEnabled) {
            return false
        }
        
        // Get REAL-TIME usage from system UsageStats
        val today = getStartOfDay()
        val endTime = System.currentTimeMillis()
        val startTime = today.time
        
        val usageStatsManager = getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val usageStatsList = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        val usageStat = usageStatsList.find { it.packageName == packageName }
        val usedTimeMinutes = (usageStat?.totalTimeInForeground ?: 0L) / 1000 / 60
        
        // Block if used time is greater than or equal to limit
        val isBlocked = usedTimeMinutes >= timeLimit.dailyLimitMinutes
        
        return isBlocked
    }
    
    private fun getStartOfDay(): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private suspend fun handleBlockedApp(packageName: String) {
        // Immediately go to home screen to close the app
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Try back action as well for better blocking
        kotlinx.coroutines.delay(100)
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // Small delay to ensure app is closed
        kotlinx.coroutines.delay(400)
        
        // Check unlock method preference
        val prefs = getSharedPreferences("braintrap_prefs", MODE_PRIVATE)
        val unlockMethod = prefs.getString("unlock_method", "math") ?: "math"
        
        // In focus mode with NFC, use special blocking activity
        if (isFocusModeActive && unlockMethod == "nfc") {
            val intent = Intent(this, com.example.braintrap.ui.FocusModeBlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("PACKAGE_NAME", packageName)
            }
            startActivity(intent)
            return
        }
        
        // Launch appropriate unlock activity based on user preference
        val intent = when (unlockMethod) {
            "nfc" -> Intent(this, com.example.braintrap.ui.NfcUnlockActivity::class.java)
            "both" -> {
                // For "both" mode, let user choose in a dialog or default to math
                Intent(this, ChallengeActivity::class.java)
            }
            else -> Intent(this, ChallengeActivity::class.java) // Default to math
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("PACKAGE_NAME", packageName)
            putExtra("from_blocking", true)
        }
        startActivity(intent)
    }
    
    fun setBlockedPackages(packages: Set<String>) {
        blockedPackages.clear()
        blockedPackages.addAll(packages)
        // Force refresh of blocking status
        lastBlockedPackage.clear()
    }
    
    fun setFocusMode(active: Boolean) {
        isFocusModeActive = active
        if (!active) {
            lastBlockedPackage.clear()
        }
    }
    
    fun refreshBlockingStatus() {
        // Clear last blocked to allow re-checking
        lastBlockedPackage.clear()
    }
    
    fun temporarilyAllowApp(packageName: String, durationMinutes: Int = 10) {
        val expiryTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        temporaryWhitelist[packageName] = expiryTime
        lastBlockedPackage.remove(packageName)
    }
    
    fun getWhitelistExpiry(packageName: String): Long {
        return temporaryWhitelist[packageName] ?: 0L
    }
    
    companion object {
        var instance: AppBlockingService? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Load focus mode state from SharedPreferences
        val focusPrefs = getSharedPreferences("focus_mode_prefs", MODE_PRIVATE)
        isFocusModeActive = focusPrefs.getBoolean("is_focus_mode_active", false)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}

