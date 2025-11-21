package com.example.braintrap.data.model

/**
 * Represents time limit configuration for an app
 */
data class TimeLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val weekdayLimitMinutes: Int? = null,
    val weekendLimitMinutes: Int? = null,
    val isEnabled: Boolean = true
)

