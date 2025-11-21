package com.example.braintrap.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.braintrap.data.database.AppDatabase
import com.example.braintrap.data.database.dao.AchievementDao
import com.example.braintrap.data.database.dao.ChallengeHistoryDao
import com.example.braintrap.data.database.dao.StreakDao
import com.example.braintrap.data.database.dao.TimeLimitDao
import com.example.braintrap.data.database.dao.UsageStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create achievements table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS achievements (
                    achievementId TEXT PRIMARY KEY NOT NULL,
                    unlockedAt INTEGER NOT NULL,
                    progress INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // Create challenge_history table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS challenge_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    packageName TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    attemptsCount INTEGER NOT NULL DEFAULT 0,
                    completionTimeSeconds INTEGER NOT NULL DEFAULT 0,
                    difficulty TEXT NOT NULL
                )
            """)
        }
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mindful_usage_database"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideTimeLimitDao(database: AppDatabase): TimeLimitDao {
        return database.timeLimitDao()
    }
    
    @Provides
    fun provideUsageStatsDao(database: AppDatabase): UsageStatsDao {
        return database.usageStatsDao()
    }
    
    @Provides
    fun provideStreakDao(database: AppDatabase): StreakDao {
        return database.streakDao()
    }
    
    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }
    
    @Provides
    fun provideChallengeHistoryDao(database: AppDatabase): ChallengeHistoryDao {
        return database.challengeHistoryDao()
    }
}

