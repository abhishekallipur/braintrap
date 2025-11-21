package com.example.braintrap.data.model

import java.util.Date

/**
 * Represents app usage statistics for a specific day
 */
data class UsageStats(
    val packageName: String,
    val date: Date,
    val usageTimeMinutes: Long,
    val unlockCount: Int = 0,
    val challengesCompleted: Int = 0
)

