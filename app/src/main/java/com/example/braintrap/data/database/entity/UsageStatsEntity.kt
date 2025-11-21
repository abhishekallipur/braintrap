package com.example.braintrap.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "usage_stats")
data class UsageStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: Long, // Stored as timestamp
    val usageTimeMinutes: Long,
    val unlockCount: Int = 0,
    val challengesCompleted: Int = 0
)

