package com.example.braintrap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.data.repository.UsageStatsRepository
import com.example.braintrap.ui.screen.DayActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ActivityGridViewModel @Inject constructor(
    private val timeLimitRepository: TimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) : ViewModel() {
    
    private val _activityData = MutableStateFlow<List<DayActivity>>(emptyList())
    val activityData: StateFlow<List<DayActivity>> = _activityData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadActivityData()
    }
    
    private fun loadActivityData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val calendar = Calendar.getInstance()
            val today = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -365)
            val startDate = calendar.time
            
            // Get all time limits
            val timeLimitsFlow = timeLimitRepository.getAllTimeLimits()
            
            timeLimitsFlow.collect { timeLimits ->
                val activities = mutableListOf<DayActivity>()
                calendar.time = startDate
                
                while (calendar.time <= today) {
                    val currentDate = calendar.time.clone() as Date
                    val dayStart = getStartOfDay(currentDate)
                    val dayEnd = getEndOfDay(currentDate)
                    
                    var totalBlockedApps = 0
                    var exceededApps = 0
                    var stayedWithinLimit = 0
                    var totalTimeSavedMinutes = 0L
                    
                    // Check each app with a time limit
                    for (limit in timeLimits) {
                        if (!limit.isEnabled) continue
                        
                        totalBlockedApps++
                        
                        // Get usage for this day
                        val usageMinutes = usageStatsRepository.getTotalUsageForDay(
                            limit.packageName,
                            currentDate
                        ) / 1000 / 60
                        
                        if (usageMinutes >= limit.dailyLimitMinutes) {
                            exceededApps++
                        } else {
                            stayedWithinLimit++
                            // Time saved = limit - actual usage
                            totalTimeSavedMinutes += (limit.dailyLimitMinutes - usageMinutes).coerceAtLeast(0)
                        }
                    }
                    
                    activities.add(
                        DayActivity(
                            date = currentDate,
                            totalBlockedApps = totalBlockedApps,
                            exceededApps = exceededApps,
                            stayedWithinLimit = stayedWithinLimit,
                            totalTimeSavedMinutes = totalTimeSavedMinutes
                        )
                    )
                    
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                _activityData.value = activities
                _isLoading.value = false
            }
        }
    }
    
    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
}
