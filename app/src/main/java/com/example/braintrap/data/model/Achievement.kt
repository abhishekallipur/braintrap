package com.example.braintrap.data.model

enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requirement: Int
) {
    FIRST_CHALLENGE("first_challenge", "🎯 First Victory", "Complete your first challenge", "🎯", 1),
    STREAK_3("streak_3", "🔥 On Fire", "Maintain a 3-day streak", "🔥", 3),
    STREAK_7("streak_7", "⭐ Week Warrior", "Maintain a 7-day streak", "⭐", 7),
    STREAK_30("streak_30", "👑 Monthly Master", "Maintain a 30-day streak", "👑", 30),
    CHALLENGES_10("challenges_10", "🧮 Math Whiz", "Complete 10 challenges", "🧮", 10),
    CHALLENGES_50("challenges_50", "🎓 Scholar", "Complete 50 challenges", "🎓", 50),
    CHALLENGES_100("challenges_100", "🏆 Champion", "Complete 100 challenges", "🏆", 100),
    SPEED_DEMON("speed_demon", "⚡ Speed Demon", "Complete 5 challenges in under 15 seconds", "⚡", 5),
    UNDER_LIMIT_7("under_limit_7", "✅ Week Perfect", "Stay under limit for 7 days straight", "✅", 7),
    UNDER_LIMIT_30("under_limit_30", "💎 Month Perfect", "Stay under limit for 30 days straight", "💎", 30),
    TIME_SAVED_10H("time_saved_10h", "⏰ 10 Hours Saved", "Save 10 hours of screen time", "⏰", 600),
    TIME_SAVED_50H("time_saved_50h", "📚 50 Hours Saved", "Save 50 hours of screen time", "📚", 3000),
    PERFECT_DAY("perfect_day", "🌟 Perfect Day", "Don't exceed limit on any app for a day", "🌟", 1)
}
