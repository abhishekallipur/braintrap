package com.example.braintrap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.braintrap.data.model.AppUsage
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.data.repository.UsageStatsRepository
import com.example.braintrap.service.AppBlockingService
import com.example.braintrap.service.UsageTrackingService
import com.example.braintrap.util.AppInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar
import javax.inject.Inject

data class TimeSavedStats(
    val todayMinutes: Long = 0,
    val weekMinutes: Long = 0,
    val monthMinutes: Long = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val timeLimitRepository: TimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val usageTrackingService: UsageTrackingService,
    private val appInfoProvider: AppInfoProvider
) : ViewModel() {
    
    private val _isFocusModeActive = MutableStateFlow(false)
    val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()
    
    private val _appUsageList = MutableStateFlow<List<AppUsage>>(emptyList())
    val appUsageList: StateFlow<List<AppUsage>> = _appUsageList.asStateFlow()
    
    private val _timeSavedStats = MutableStateFlow(TimeSavedStats())
    val timeSavedStats: StateFlow<TimeSavedStats> = _timeSavedStats.asStateFlow()
    
    private var currentTimeLimits: List<com.example.braintrap.data.model.TimeLimit> = emptyList()
    
    init {
        viewModelScope.launch {
            usageTrackingService.startTracking()
            
            // Load blocked packages into the service and keep it updated
            timeLimitRepository.getEnabledTimeLimits().collect { timeLimits ->
                currentTimeLimits = timeLimits
                val blockedPackages = timeLimits.map { it.packageName }.toSet()
                AppBlockingService.instance?.let { service ->
                    service.setBlockedPackages(blockedPackages)
                    // Refresh blocking status to ensure it's up to date
                    service.refreshBlockingStatus()
                }
                updateAppUsageList(timeLimits)
                updateTimeSavedStats(timeLimits)
            }
        }
        
        // Periodically refresh to ensure service has latest data and update UI
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000) // Every 2 seconds for faster updates
                AppBlockingService.instance?.refreshBlockingStatus()
                // Refresh app usage list to update remaining time
                updateAppUsageList(currentTimeLimits)
                updateTimeSavedStats(currentTimeLimits)
            }
        }
    }
    
    private suspend fun updateAppUsageList(timeLimits: List<com.example.braintrap.data.model.TimeLimit>) {
        val today = getStartOfDay()
        val apps = appInfoProvider.getInstalledApps()
        val currentTime = System.currentTimeMillis()
        
        val usageList = timeLimits.map { timeLimit ->
            // Get real-time usage from the tracking service
            val usedTime = usageTrackingService.getUsageForToday(timeLimit.packageName)
            
            // Check if app is temporarily whitelisted
            val whitelistExpiry = AppBlockingService.instance?.let { service ->
                service.getWhitelistExpiry(timeLimit.packageName)
            } ?: 0L
            
            val remainingTime = if (whitelistExpiry > currentTime) {
                // App is whitelisted, show remaining whitelist time in minutes
                ((whitelistExpiry - currentTime) / 1000 / 60).coerceAtLeast(0)
            } else {
                // Normal time calculation
                (timeLimit.dailyLimitMinutes - usedTime).coerceAtLeast(0)
            }
            
            AppUsage(
                packageName = timeLimit.packageName,
                appName = apps.find { it.packageName == timeLimit.packageName }?.appName 
                    ?: timeLimit.packageName,
                timeLimitMinutes = timeLimit.dailyLimitMinutes,
                usedTimeMinutes = usedTime,
                remainingTimeMinutes = remainingTime,
                isBlocked = remainingTime == 0L && whitelistExpiry <= currentTime,
                isInFocusMode = _isFocusModeActive.value
            )
        }
        
        _appUsageList.value = usageList
    }
    
    fun toggleFocusMode() {
        _isFocusModeActive.value = !_isFocusModeActive.value
        // Update blocking service
        AppBlockingService.instance?.setFocusMode(_isFocusModeActive.value)
        // Refresh app usage list to update focus mode status
        viewModelScope.launch {
            timeLimitRepository.getEnabledTimeLimits().collect { timeLimits ->
                updateAppUsageList(timeLimits)
            }
        }
    }
    
    fun removeApp(packageName: String) {
        viewModelScope.launch {
            // Delete the time limit for this app
            timeLimitRepository.deleteTimeLimit(packageName)
            // The Flow collection in init will automatically update the UI
        }
    }
    
    private fun getStartOfDay(): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private fun updateTimeSavedStats(timeLimits: List<com.example.braintrap.data.model.TimeLimit>) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val today = getStartOfDay()
            
            // Calculate week start (last Sunday or Monday)
            val weekStart = Calendar.getInstance().apply {
                time = today
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }.time
            
            // Calculate month start
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            var todaySaved = 0L
            var weekSaved = 0L
            var monthSaved = 0L
            
            timeLimits.forEach { timeLimit ->
                val todayUsed = usageTrackingService.getUsageForToday(timeLimit.packageName)
                val timeSavedToday = (timeLimit.dailyLimitMinutes - todayUsed).coerceAtLeast(0)
                todaySaved += timeSavedToday
                
                // For week and month, sum up all days' saved time
                // This is simplified - you could enhance with historical data from UsageStats
                weekSaved += timeSavedToday * 7 // Approximate
                monthSaved += timeSavedToday * 30 // Approximate
            }
            
            _timeSavedStats.value = TimeSavedStats(
                todayMinutes = todaySaved,
                weekMinutes = weekSaved,
                monthMinutes = monthSaved
            )
        }
    }
}

