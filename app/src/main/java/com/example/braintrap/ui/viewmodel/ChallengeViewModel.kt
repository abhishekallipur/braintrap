package com.example.braintrap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.braintrap.data.model.Achievement
import com.example.braintrap.data.model.Challenge
import com.example.braintrap.data.repository.AchievementRepository
import com.example.braintrap.data.repository.ChallengeHistoryRepository
import com.example.braintrap.data.repository.TimeLimitRepository
import com.example.braintrap.data.repository.UsageStatsRepository
import com.example.braintrap.util.ChallengeGenerator
import com.example.braintrap.util.MotivationalQuotes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ChallengeUiState(
    val currentChallenge: Challenge? = null,
    val currentProblem: Int = 0,
    val totalProblems: Int = ChallengeGenerator.getProblemsRequired(),
    val timeRemaining: Int = 30,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val unlockAttempt: Int = 1,
    val streak: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
    val speedBonus: Boolean = false,
    val motivationalQuote: String = MotivationalQuotes.getRandomEncouragement(),
    val newAchievement: Achievement? = null,
    val totalAttempts: Int = 0
)

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val timeLimitRepository: TimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val challengeHistoryRepository: ChallengeHistoryRepository,
    private val achievementRepository: AchievementRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()
    
    private var packageName: String = ""
    private var timerJob: kotlinx.coroutines.Job? = null
    private val prefs = context.getSharedPreferences("braintrap_prefs", Context.MODE_PRIVATE)
    private var challengeStartTime: Long = 0
    private var currentAttempts = 0
    
    fun startChallenge(packageName: String) {
        this.packageName = packageName
        challengeStartTime = System.currentTimeMillis()
        currentAttempts = 0
        loadNextChallenge()
    }
    
    private fun loadNextChallenge() {
        val currentState = _uiState.value
        
        viewModelScope.launch {
            // Use progressive difficulty based on history
            val difficulty = challengeHistoryRepository.getRecommendedDifficulty(packageName)
            val challenge = ChallengeGenerator.generateChallenge(difficulty)
            
            _uiState.value = currentState.copy(
                currentChallenge = challenge,
                currentProblem = currentState.currentProblem + 1,
                timeRemaining = challenge.timeLimitSeconds,
                error = null,
                motivationalQuote = MotivationalQuotes.getRandomEncouragement()
            )
            
            startTimer()
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && !_uiState.value.isCompleted) {
                delay(1000)
                val currentState = _uiState.value
                if (currentState.timeRemaining > 0) {
                    _uiState.value = currentState.copy(timeRemaining = currentState.timeRemaining - 1)
                } else {
                    // Time's up
                    _uiState.value = currentState.copy(
                        error = "Time's up! Challenge failed.",
                        currentChallenge = null
                    )
                }
            }
        }
    }
    
    fun submitAnswer(answer: Int) {
        val currentState = _uiState.value
        val challenge = currentState.currentChallenge ?: return
        
        timerJob?.cancel()
        currentAttempts++
        
        if (answer == challenge.answer) {
            // Correct answer
            val newStreak = currentState.streak + 1
            val speedBonus = currentState.timeRemaining > 20 // Answered quickly
            
            // Show correct feedback briefly
            _uiState.value = currentState.copy(
                lastAnswerCorrect = true,
                streak = newStreak,
                speedBonus = speedBonus,
                totalAttempts = currentAttempts
            )
            
            viewModelScope.launch {
                delay(500) // Brief pause to show feedback
                
                if (currentState.currentProblem >= currentState.totalProblems) {
                    // All problems solved
                    completeChallenge()
                } else {
                    // Load next problem
                    _uiState.value = currentState.copy(
                        unlockAttempt = currentState.unlockAttempt + 1,
                        streak = newStreak,
                        lastAnswerCorrect = null,
                        speedBonus = false
                    )
                    loadNextChallenge()
                }
            }
        } else {
            // Wrong answer - show feedback then restart
            _uiState.value = currentState.copy(
                lastAnswerCorrect = false,
                error = "❌ " + MotivationalQuotes.getRandomQuote()
            )
            
            viewModelScope.launch {
                delay(1500)
                _uiState.value = ChallengeUiState(
                    unlockAttempt = 1,
                    streak = 0,
                    motivationalQuote = MotivationalQuotes.getRandomEncouragement()
                )
                currentAttempts = 0
                startChallenge(packageName)
            }
        }
    }
    
    private fun completeChallenge() {
        viewModelScope.launch {
            val today = getStartOfDay()
            val unlockMinutes = prefs.getInt("bonus_time_minutes", 45)
            val completionTime = ((System.currentTimeMillis() - challengeStartTime) / 1000).toInt()
            val currentState = _uiState.value
            
            // Record challenge history for progressive difficulty
            challengeHistoryRepository.recordChallenge(
                packageName = packageName,
                attemptsCount = currentAttempts,
                completionTimeSeconds = completionTime,
                difficulty = currentState.currentChallenge?.difficulty ?: com.example.braintrap.data.model.ChallengeDifficulty.EASY
            )
            
            // Record the challenge completion
            usageStatsRepository.incrementChallengesCompleted(packageName, today)
            usageStatsRepository.incrementUnlockCount(packageName, today)
            
            // Check and unlock achievements
            checkAchievements(completionTime)
            
            // Temporarily whitelist the app
            com.example.braintrap.service.AppBlockingService.instance?.let { service ->
                service.temporarilyAllowApp(packageName, unlockMinutes)
                service.refreshBlockingStatus()
            }
            
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
    
    private suspend fun checkAchievements(completionTime: Int) {
        val totalChallenges = challengeHistoryRepository.getTotalChallengesCompleted()
        val fastChallenges = challengeHistoryRepository.getFastChallengesCount()
        
        // Check various achievements
        val achievementsToCheck = listOf(
            Achievement.FIRST_CHALLENGE to (totalChallenges >= 1),
            Achievement.CHALLENGES_10 to (totalChallenges >= 10),
            Achievement.CHALLENGES_50 to (totalChallenges >= 50),
            Achievement.CHALLENGES_100 to (totalChallenges >= 100),
            Achievement.SPEED_DEMON to (fastChallenges >= 5)
        )
        
        for ((achievement, condition) in achievementsToCheck) {
            if (condition && !achievementRepository.isAchievementUnlocked(achievement.id)) {
                achievementRepository.unlockAchievement(achievement)
                _uiState.value = _uiState.value.copy(newAchievement = achievement)
                delay(2000) // Show achievement notification
                _uiState.value = _uiState.value.copy(newAchievement = null)
            }
        }
    }
    
    private fun getStartOfDay(): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

