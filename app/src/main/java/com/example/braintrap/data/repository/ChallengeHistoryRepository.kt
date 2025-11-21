package com.example.braintrap.data.repository

import com.example.braintrap.data.database.dao.ChallengeHistoryDao
import com.example.braintrap.data.database.entity.ChallengeHistoryEntity
import com.example.braintrap.data.model.ChallengeDifficulty
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeHistoryRepository @Inject constructor(
    private val challengeHistoryDao: ChallengeHistoryDao
) {
    suspend fun recordChallenge(
        packageName: String,
        attemptsCount: Int,
        completionTimeSeconds: Int,
        difficulty: ChallengeDifficulty
    ) {
        challengeHistoryDao.insertChallengeHistory(
            ChallengeHistoryEntity(
                packageName = packageName,
                date = System.currentTimeMillis(),
                attemptsCount = attemptsCount,
                completionTimeSeconds = completionTimeSeconds,
                difficulty = difficulty.name
            )
        )
    }
    
    fun getRecentChallenges(): Flow<List<ChallengeHistoryEntity>> {
        return challengeHistoryDao.getRecentChallenges()
    }
    
    suspend fun getTotalChallengesCompleted(): Int {
        return challengeHistoryDao.getTotalChallengesCompleted()
    }
    
    suspend fun getFastChallengesCount(): Int {
        return challengeHistoryDao.getFastChallengesCount()
    }
    
    suspend fun getAverageAttempts(packageName: String): Float {
        return challengeHistoryDao.getAverageAttempts(packageName)
    }
    
    suspend fun getRecentChallengesForApp(packageName: String, limit: Int = 10): List<ChallengeHistoryEntity> {
        return challengeHistoryDao.getRecentChallengesForApp(packageName, limit)
    }
    
    // Progressive difficulty logic
    suspend fun getRecommendedDifficulty(packageName: String): ChallengeDifficulty {
        val recent = getRecentChallengesForApp(packageName, 5)
        
        if (recent.isEmpty()) {
            return ChallengeDifficulty.EASY
        }
        
        // Calculate average attempts in recent challenges
        val avgAttempts = recent.map { it.attemptsCount }.average()
        val avgTime = recent.map { it.completionTimeSeconds }.average()
        
        // If user is consistently solving quickly with few attempts, increase difficulty
        return when {
            avgAttempts <= 1.2 && avgTime < 20 -> ChallengeDifficulty.HARD
            avgAttempts <= 1.5 && avgTime < 25 -> ChallengeDifficulty.MEDIUM
            else -> ChallengeDifficulty.EASY
        }
    }
}
