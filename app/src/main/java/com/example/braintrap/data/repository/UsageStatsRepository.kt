package com.example.braintrap.data.repository

import com.example.braintrap.data.database.dao.UsageStatsDao
import com.example.braintrap.data.database.entity.UsageStatsEntity
import com.example.braintrap.data.model.UsageStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsRepository @Inject constructor(
    private val usageStatsDao: UsageStatsDao
) {
    suspend fun getUsageStats(packageName: String, date: Date): UsageStats? {
        val timestamp = date.time
        return usageStatsDao.getUsageStats(packageName, timestamp)?.toUsageStats()
    }
    
    fun getUsageStatsInRange(
        packageName: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<UsageStats>> {
        return usageStatsDao.getUsageStatsInRange(
            packageName,
            startDate.time,
            endDate.time
        ).map { entities ->
            entities.map { it.toUsageStats() }
        }
    }
    
    fun getAllUsageStatsInRange(
        startDate: Date,
        endDate: Date
    ): Flow<List<UsageStats>> {
        return usageStatsDao.getAllUsageStatsInRange(
            startDate.time,
            endDate.time
        ).map { entities ->
            entities.map { it.toUsageStats() }
        }
    }
    
    suspend fun getTotalUsageForDay(packageName: String, date: Date): Long {
        return usageStatsDao.getTotalUsageForDay(packageName, date.time) ?: 0L
    }
    
    suspend fun insertOrUpdateUsageStats(usageStats: UsageStats) {
        val existing = usageStatsDao.getUsageStats(
            usageStats.packageName,
            usageStats.date.time
        )
        
        if (existing != null) {
            usageStatsDao.updateUsageStats(usageStats.toEntity(existing.id))
        } else {
            usageStatsDao.insertUsageStats(usageStats.toEntity())
        }
    }
    
    suspend fun incrementUnlockCount(packageName: String, date: Date) {
        val timestamp = date.time
        val existing = usageStatsDao.getUsageStats(packageName, timestamp)
        
        if (existing != null) {
            usageStatsDao.updateUsageStats(
                existing.copy(unlockCount = existing.unlockCount + 1)
            )
        } else {
            usageStatsDao.insertUsageStats(
                UsageStatsEntity(
                    packageName = packageName,
                    date = timestamp,
                    usageTimeMinutes = 0,
                    unlockCount = 1,
                    challengesCompleted = 0
                )
            )
        }
    }
    
    suspend fun incrementChallengesCompleted(packageName: String, date: Date) {
        val timestamp = date.time
        val existing = usageStatsDao.getUsageStats(packageName, timestamp)
        
        if (existing != null) {
            usageStatsDao.updateUsageStats(
                existing.copy(challengesCompleted = existing.challengesCompleted + 1)
            )
        } else {
            usageStatsDao.insertUsageStats(
                UsageStatsEntity(
                    packageName = packageName,
                    date = timestamp,
                    usageTimeMinutes = 0,
                    unlockCount = 0,
                    challengesCompleted = 1
                )
            )
        }
    }
    
    suspend fun cleanupOldStats(daysToKeep: Int = 90) {
        val cutoffDate = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        usageStatsDao.deleteOldStats(cutoffDate)
    }
}

private fun UsageStatsEntity.toUsageStats() = UsageStats(
    packageName = packageName,
    date = Date(date),
    usageTimeMinutes = usageTimeMinutes,
    unlockCount = unlockCount,
    challengesCompleted = challengesCompleted
)

private fun UsageStats.toEntity(id: Long = 0) = UsageStatsEntity(
    id = id,
    packageName = packageName,
    date = date.time,
    usageTimeMinutes = usageTimeMinutes,
    unlockCount = unlockCount,
    challengesCompleted = challengesCompleted
)

