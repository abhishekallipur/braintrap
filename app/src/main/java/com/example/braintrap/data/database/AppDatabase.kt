package com.example.braintrap.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.braintrap.data.database.dao.AchievementDao
import com.example.braintrap.data.database.dao.ChallengeHistoryDao
import com.example.braintrap.data.database.dao.StreakDao
import com.example.braintrap.data.database.dao.TimeLimitDao
import com.example.braintrap.data.database.dao.UsageStatsDao
import com.example.braintrap.data.database.entity.AchievementEntity
import com.example.braintrap.data.database.entity.ChallengeHistoryEntity
import com.example.braintrap.data.database.entity.StreakEntity
import com.example.braintrap.data.database.entity.TimeLimitEntity
import com.example.braintrap.data.database.entity.UsageStatsEntity

@Database(
    entities = [
        TimeLimitEntity::class,
        UsageStatsEntity::class,
        StreakEntity::class,
        AchievementEntity::class,
        ChallengeHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeLimitDao(): TimeLimitDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun streakDao(): StreakDao
    abstract fun achievementDao(): AchievementDao
    abstract fun challengeHistoryDao(): ChallengeHistoryDao
}

