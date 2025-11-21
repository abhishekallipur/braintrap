package com.example.braintrap.data.database.dao

import androidx.room.*
import com.example.braintrap.data.database.entity.UsageStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageStatsDao {
    @Query("SELECT * FROM usage_stats WHERE packageName = :packageName AND date = :date")
    suspend fun getUsageStats(packageName: String, date: Long): UsageStatsEntity?
    
    @Query("SELECT * FROM usage_stats WHERE packageName = :packageName AND date >= :startDate AND date <= :endDate")
    fun getUsageStatsInRange(packageName: String, startDate: Long, endDate: Long): Flow<List<UsageStatsEntity>>
    
    @Query("SELECT * FROM usage_stats WHERE date >= :startDate AND date <= :endDate")
    fun getAllUsageStatsInRange(startDate: Long, endDate: Long): Flow<List<UsageStatsEntity>>
    
    @Query("SELECT SUM(usageTimeMinutes) FROM usage_stats WHERE packageName = :packageName AND date = :date")
    suspend fun getTotalUsageForDay(packageName: String, date: Long): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(usageStats: UsageStatsEntity)
    
    @Update
    suspend fun updateUsageStats(usageStats: UsageStatsEntity)
    
    @Query("DELETE FROM usage_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: Long)
}

