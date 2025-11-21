package com.example.braintrap.data.database.dao

import androidx.room.*
import com.example.braintrap.data.database.entity.TimeLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeLimitDao {
    @Query("SELECT * FROM time_limits")
    fun getAllTimeLimits(): Flow<List<TimeLimitEntity>>
    
    @Query("SELECT * FROM time_limits WHERE packageName = :packageName")
    suspend fun getTimeLimit(packageName: String): TimeLimitEntity?
    
    @Query("SELECT * FROM time_limits WHERE isEnabled = 1")
    fun getEnabledTimeLimits(): Flow<List<TimeLimitEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLimit(timeLimit: TimeLimitEntity)
    
    @Update
    suspend fun updateTimeLimit(timeLimit: TimeLimitEntity)
    
    @Delete
    suspend fun deleteTimeLimit(timeLimit: TimeLimitEntity)
    
    @Query("DELETE FROM time_limits WHERE packageName = :packageName")
    suspend fun deleteTimeLimitByPackage(packageName: String)
}

