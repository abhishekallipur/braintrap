package com.example.braintrap.di

import android.content.Context
import com.example.braintrap.util.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppInfoProvider(@ApplicationContext context: Context): AppInfoProvider {
        return AppInfoProvider(context)
    }
}

