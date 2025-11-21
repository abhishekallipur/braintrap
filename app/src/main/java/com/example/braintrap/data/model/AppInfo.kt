package com.example.braintrap.data.model

import android.graphics.drawable.Drawable

/**
 * Represents information about an installed app
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory {
    SOCIAL_MEDIA,
    ENTERTAINMENT,
    GAMES,
    PRODUCTIVITY,
    OTHER
}

