package com.runit.runup.di

import com.runit.runup.data.local.UserPreferenceDataSource
import com.runit.runup.data.source.local.objectbox.entity.CourseEntity
import com.runit.runup.service.EmbeddingHelper
import com.runit.runup.service.GeminiHelper
import com.runit.runup.ui.util.mapper.CourseMapper
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