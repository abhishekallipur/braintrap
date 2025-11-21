package com.example.braintrap.data.repository

import com.example.braintrap.data.database.dao.StreakDao
import com.example.braintrap.data.database.entity.StreakEntity
import com.example.braintrap.data.model.Streak
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val streakDao: StreakDao
) {
    suspend fun getStreak(): Streak? {
        return streakDao.getStreak()?.toStreak()
    }
    
    suspend fun updateStreak(streak: Streak) {
        streakDao.insertStreak(streak.toEntity())
    }
    
    suspend fun incrementStreak() {
        val existing = streakDao.getStreak()
        if (existing != null) {
            val newStreak = existing.currentStreak + 1
            streakDao.updateStreak(
                existing.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(existing.longestStreak, newStreak),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            streakDao.insertStreak(
                StreakEntity(
                    currentStreak = 1,
                    longestStreak = 1,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
    
    suspend fun resetStreak() {
        val existing = streakDao.getStreak()
        if (existing != null) {
            streakDao.updateStreak(
                existing.copy(
                    currentStreak = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
}

private fun StreakEntity.toStreak() = Streak(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastUpdated = Date(lastUpdated)
)

private fun Streak.toEntity() = StreakEntity(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastUpdated = lastUpdated.time
)

