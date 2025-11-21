package com.example.braintrap.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: Int = 1, // Single row
    val currentStreak: Int,
    val longestStreak: Int,
    val lastUpdated: Long // Stored as timestamp
)

