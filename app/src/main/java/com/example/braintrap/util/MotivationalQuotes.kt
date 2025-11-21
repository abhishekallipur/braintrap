package com.example.braintrap.util

import kotlin.random.Random

object MotivationalQuotes {
    private val quotes = listOf(
        "💪 You're stronger than your distractions!",
        "🎯 Stay focused, stay productive!",
        "🌟 Every challenge makes you better!",
        "🧠 Your brain will thank you later!",
        "⏰ Time is your most valuable resource!",
        "🚀 You're building better habits!",
        "💎 Discipline equals freedom!",
        "🏆 Winners choose focus over scrolling!",
        "✨ Your future self will thank you!",
        "🔥 Break the addiction, build your future!",
        "📚 Knowledge beats endless scrolling!",
        "🎓 Your goals are bigger than notifications!",
        "💡 Invest in yourself, not in feeds!",
        "⭐ You control the app, not vice versa!",
        "🌈 Real life is more colorful than any screen!",
        "🎨 Create instead of consume!",
        "🌱 You're growing stronger every day!",
        "🔓 Unlock your potential, lock the apps!",
        "💪 Mindful > Mindless!",
        "🎯 Focus is a superpower!",
        "🧘 Peace of mind > Pieces of content!",
        "⚡ Your energy is precious - use it wisely!",
        "🌟 Small wins lead to big changes!",
        "📈 Progress over perfection!",
        "🎉 You're doing amazing!"
    )
    
    private val celebrationMessages = listOf(
        "🎉 Challenge conquered! You're unstoppable!",
        "💪 Nailed it! Your willpower is impressive!",
        "🌟 Brilliant! You're building great habits!",
        "🔥 On fire! Keep this momentum going!",
        "⭐ Fantastic! You're in control!",
        "🏆 Victory! You earned this time!",
        "✨ Excellent! Your brain is getting stronger!",
        "🎯 Perfect! You're a focus champion!",
        "💎 Amazing! Discipline looks good on you!",
        "🚀 Crushing it! You're leveling up!"
    )
    
    private val encouragementBeforeChallenge = listOf(
        "🧮 Quick math break - you got this!",
        "💡 Time to flex that brain muscle!",
        "🎯 Show this challenge who's boss!",
        "⚡ Your brain is ready for this!",
        "🌟 A small pause for a big win!",
        "🔥 Challenge accepted! Let's go!",
        "💪 Easy work for a smart person like you!",
        "🎓 Put that brilliant mind to work!",
        "✨ This is your moment to shine!",
        "🧠 Your brain vs. a simple puzzle - you win!"
    )
    
    private val timeSavedMessages = listOf(
        "⏰ You saved {hours}h {minutes}m today! Time well invested!",
        "📚 {hours}h {minutes}m saved! That's productive thinking!",
        "🎯 {hours}h {minutes}m reclaimed! You're winning!",
        "✨ {hours}h {minutes}m of freedom! Use it wisely!",
        "💎 {hours}h {minutes}m back in your life! Amazing!"
    )
    
    fun getRandomQuote(): String = quotes.random()
    
    fun getRandomCelebration(): String = celebrationMessages.random()
    
    fun getRandomEncouragement(): String = encouragementBeforeChallenge.random()
    
    fun getTimeSavedMessage(minutesSaved: Long): String {
        val hours = minutesSaved / 60
        val minutes = minutesSaved % 60
        return timeSavedMessages.random()
            .replace("{hours}", hours.toString())
            .replace("{minutes}", minutes.toString())
    }
    
    fun getStreakMessage(streak: Int): String {
        return when {
            streak == 1 -> "🔥 Day 1! Every journey starts with a single step!"
            streak < 7 -> "🔥 $streak days! You're building momentum!"
            streak < 30 -> "⭐ $streak days! You're on a roll!"
            streak < 100 -> "👑 $streak days! You're a legend!"
            else -> "🌟 $streak days! Absolutely incredible!"
        }
    }
    
    fun getAchievementUnlockedMessage(achievementTitle: String): String {
        return "🏆 Achievement Unlocked: $achievementTitle"
    }
}
