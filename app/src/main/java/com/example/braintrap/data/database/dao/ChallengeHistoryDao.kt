package com.example.braintrap.data.database.dao

import androidx.room.*
import com.example.braintrap.data.database.entity.ChallengeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeHistoryDao {
    @Insert
    suspend fun insertChallengeHistory(history: ChallengeHistoryEntity)
    
    @Query("SELECT * FROM challenge_history ORDER BY date DESC LIMIT 100")
    fun getRecentChallenges(): Flow<List<ChallengeHistoryEntity>>
    
    @Query("SELECT COUNT(*) FROM challenge_history")
    suspend fun getTotalChallengesCompleted(): Int
    
    @Query("SELECT COUNT(*) FROM challenge_history WHERE completionTimeSeconds <= 15")
    suspend fun getFastChallengesCount(): Int
    
    @Query("SELECT AVG(attemptsCount) FROM challenge_history WHERE packageName = :packageName")
    suspend fun getAverageAttempts(packageName: String): Float
    
    @Query("SELECT * FROM challenge_history WHERE packageName = :packageName ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentChallengesForApp(packageName: String, limit: Int = 10): List<ChallengeHistoryEntity>
}
