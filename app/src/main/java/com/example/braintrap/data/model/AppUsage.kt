package com.example.braintrap.data.model

/**
 * Represents current usage state for an app
 */
data class AppUsage(
    val packageName: String,
    val appName: String,
    val timeLimitMinutes: Int,
    val usedTimeMinutes: Long,
    val remainingTimeMinutes: Long,
    val isBlocked: Boolean,
    val isInFocusMode: Boolean = false
) {
    val progress: Float
        get() = if (timeLimitMinutes > 0) {
            (usedTimeMinutes.toFloat() / timeLimitMinutes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

