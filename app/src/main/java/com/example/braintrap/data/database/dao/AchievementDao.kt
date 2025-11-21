package com.example.braintrap.data.database.dao

import androidx.room.*
import com.example.braintrap.data.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId")
    suspend fun getAchievement(achievementId: String): AchievementEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)
    
    @Query("UPDATE achievements SET progress = :progress WHERE achievementId = :achievementId")
    suspend fun updateProgress(achievementId: String, progress: Int)
    
    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getUnlockedCount(): Int
}
