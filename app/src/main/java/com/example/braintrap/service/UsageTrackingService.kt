package com.example.braintrap.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.data.repository.UsageStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTrackingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeLimitRepository: TimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var isTracking = false
    
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        
        serviceScope.launch {
            // Track usage continuously
            while (isTracking) {
                trackUsageForToday()
                kotlinx.coroutines.delay(30000) // Update every 30 seconds for real-time accuracy
            }
        }
    }
    
    fun stopTracking() {
        isTracking = false
    }
    
    suspend fun getUsageForToday(packageName: String): Long {
        val today = getStartOfDay()
        // Get real-time usage from system UsageStats
        val endTime = System.currentTimeMillis()
        val startTime = today.time
        
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        val usageStat = usageStatsList.find { it.packageName == packageName }
        val systemUsageMinutes = (usageStat?.totalTimeInForeground ?: 0L) / 1000 / 60
        
        // Return the real system usage time
        return systemUsageMinutes
    }
    
    suspend fun getUsageForApp(packageName: String, date: Date): Long {
        return usageStatsRepository.getTotalUsageForDay(packageName, date)
    }
    
    private suspend fun trackUsageForToday() {
        val today = getStartOfDay()
        val endTime = System.currentTimeMillis()
        val startTime = today.time
        
        // Get all tracked apps
        val timeLimits = timeLimitRepository.getAllTimeLimitsOnce()
        
        // Use system's UsageStats for accurate real-time tracking
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Update stats for each tracked app
        timeLimits.forEach { timeLimit ->
            val usageStat = usageStatsList.find { it.packageName == timeLimit.packageName }
            if (usageStat != null) {
                val usageMinutes = usageStat.totalTimeInForeground / 1000 / 60
                
                // Update with actual system usage
                val existing = usageStatsRepository.getUsageStats(timeLimit.packageName, today)
                if (existing != null) {
                    usageStatsRepository.insertOrUpdateUsageStats(
                        existing.copy(usageTimeMinutes = usageMinutes)
                    )
                } else {
                    usageStatsRepository.insertOrUpdateUsageStats(
                        com.example.braintrap.data.model.UsageStats(
                            packageName = timeLimit.packageName,
                            date = today,
                            usageTimeMinutes = usageMinutes,
                            unlockCount = 0,
                            challengesCompleted = 0
                        )
                    )
                }
            }
        }
    }
    
    private suspend fun updateUsageStats(packageName: String, date: Date, additionalMinutes: Long) {
        val existing = usageStatsRepository.getUsageStats(packageName, date)
        if (existing != null) {
            usageStatsRepository.insertOrUpdateUsageStats(
                existing.copy(usageTimeMinutes = existing.usageTimeMinutes + additionalMinutes)
            )
        } else {
            usageStatsRepository.insertOrUpdateUsageStats(
                com.example.braintrap.data.model.UsageStats(
                    packageName = packageName,
                    date = date,
                    usageTimeMinutes = additionalMinutes,
                    unlockCount = 0,
                    challengesCompleted = 0
                )
            )
        }
    }
    
    private fun getStartOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}

