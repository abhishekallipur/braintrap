package com.example.braintrap.data.model

import java.util.Date

/**
 * Represents a streak of consecutive days staying under limits
 */
data class Streak(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastUpdated: Date
)

