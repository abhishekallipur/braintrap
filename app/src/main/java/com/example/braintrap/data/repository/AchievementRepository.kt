package com.example.braintrap.data.repository

import com.example.braintrap.data.database.dao.AchievementDao
import com.example.braintrap.data.database.entity.AchievementEntity
import com.example.braintrap.data.model.Achievement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {
    fun getAllUnlockedAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements().map { entities ->
            entities.mapNotNull { entity ->
                Achievement.values().find { it.id == entity.achievementId }
            }
        }
    }
    
    suspend fun unlockAchievement(achievement: Achievement) {
        achievementDao.insertAchievement(
            AchievementEntity(
                achievementId = achievement.id,
                unlockedAt = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun isAchievementUnlocked(achievementId: String): Boolean {
        return achievementDao.getAchievement(achievementId) != null
    }
    
    suspend fun updateProgress(achievementId: String, progress: Int) {
        achievementDao.updateProgress(achievementId, progress)
    }
    
    suspend fun getUnlockedCount(): Int {
        return achievementDao.getUnlockedCount()
    }
}
