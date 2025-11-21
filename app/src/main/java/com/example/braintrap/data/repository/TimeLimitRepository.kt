package com.example.braintrap.data.repository

import com.example.braintrap.data.database.dao.TimeLimitDao
import com.example.braintrap.data.database.entity.TimeLimitEntity
import com.example.braintrap.data.model.TimeLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeLimitRepository @Inject constructor(
    private val timeLimitDao: TimeLimitDao
) {
    fun getAllTimeLimits(): Flow<List<TimeLimit>> {
        return timeLimitDao.getAllTimeLimits().map { entities ->
            entities.map { it.toTimeLimit() }
        }
    }
    
    suspend fun getAllTimeLimitsOnce(): List<TimeLimit> {
        return timeLimitDao.getAllTimeLimits().map { entities ->
            entities.map { it.toTimeLimit() }
        }.first()
    }
    
    suspend fun getTimeLimit(packageName: String): TimeLimit? {
        return timeLimitDao.getTimeLimit(packageName)?.toTimeLimit()
    }
    
    fun getEnabledTimeLimits(): Flow<List<TimeLimit>> {
        return timeLimitDao.getEnabledTimeLimits().map { entities ->
            entities.map { it.toTimeLimit() }
        }
    }
    
    suspend fun insertTimeLimit(timeLimit: TimeLimit) {
        timeLimitDao.insertTimeLimit(timeLimit.toEntity())
    }
    
    suspend fun updateTimeLimit(timeLimit: TimeLimit) {
        timeLimitDao.updateTimeLimit(timeLimit.toEntity())
    }
    
    suspend fun updateTimeLimit(packageName: String, dailyLimitMinutes: Int) {
        val existing = getTimeLimit(packageName)
        if (existing != null) {
            timeLimitDao.updateTimeLimit(
                existing.copy(dailyLimitMinutes = dailyLimitMinutes).toEntity()
            )
        }
    }
    
    suspend fun deleteTimeLimit(packageName: String) {
        timeLimitDao.deleteTimeLimitByPackage(packageName)
    }
}

private fun TimeLimitEntity.toTimeLimit() = TimeLimit(
    packageName = packageName,
    dailyLimitMinutes = dailyLimitMinutes,
    weekdayLimitMinutes = weekdayLimitMinutes,
    weekendLimitMinutes = weekendLimitMinutes,
    isEnabled = isEnabled
)

private fun TimeLimit.toEntity() = TimeLimitEntity(
    packageName = packageName,
    dailyLimitMinutes = dailyLimitMinutes,
    weekdayLimitMinutes = weekdayLimitMinutes,
    weekendLimitMinutes = weekendLimitMinutes,
    isEnabled = isEnabled
)

