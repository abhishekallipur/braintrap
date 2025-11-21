package com.example.braintrap.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val achievementId: String,
    val unlockedAt: Long, // Timestamp when unlocked
    val progress: Int = 0 // For multi-level achievements
)
