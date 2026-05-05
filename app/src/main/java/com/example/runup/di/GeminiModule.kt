package com.example.runup.di

import com.example.runup.data.local.UserPreferenceDataSource
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.service.EmbeddingHelper
import com.example.runup.service.GeminiHelper
import com.example.runup.ui.util.mapper.CourseMapper
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiHelper(
        embeddingHelper: EmbeddingHelper,
        courseBox: Box<CourseEntity>,
        courseMapper: CourseMapper,
        userPreferenceDataSource: UserPreferenceDataSource
    ): GeminiHelper {
        // 이제 GemmaHelper를 만들 때 모든 재료를 다 던져줍니다.
        return GeminiHelper(embeddingHelper, courseBox,  courseMapper, userPreferenceDataSource)
    }
}