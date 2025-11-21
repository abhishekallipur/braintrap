package com.example.braintrap.data.database.dao

import androidx.room.*
import com.example.braintrap.data.database.entity.StreakEntity

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun getStreak(): StreakEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)
    
    @Update
    suspend fun updateStreak(streak: StreakEntity)
}

