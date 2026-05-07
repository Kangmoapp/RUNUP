package com.runit.runup.di

import android.content.Context
import com.runit.runup.service.EmbeddingHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmbeddingModule {
    @Provides
    @Singleton
    fun provideEmbeddingHelper(
        @ApplicationContext context: Context
    ): EmbeddingHelper {
        return EmbeddingHelper(context)
    }
}