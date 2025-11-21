package com.example.braintrap.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge_history")
data class ChallengeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: Long, // Timestamp
    val attemptsCount: Int = 0, // How many times user failed before success
    val completionTimeSeconds: Int = 0, // How fast they completed
    val difficulty: String // EASY, MEDIUM, HARD
)
