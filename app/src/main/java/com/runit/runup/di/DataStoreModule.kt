package com.runit.runup.di

import android.content.Context
import com.runit.runup.data.local.UserPreferenceDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferenceDataSource(
        @ApplicationContext context: Context
    ): UserPreferenceDataSource {
        return UserPreferenceDataSource(context)
    }
}