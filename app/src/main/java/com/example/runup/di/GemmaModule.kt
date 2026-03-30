package com.example.runup.di

import android.content.Context
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.service.EmbeddingHelper
import com.example.runup.service.GemmaHelper
import com.example.runup.ui.util.mapper.CourseMapper
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GemmaModule {
    @Provides
    @Singleton
    fun provideGemmaHelper(
        @ApplicationContext context: Context,
        embeddingHelper: EmbeddingHelper,
        courseBox: Box<CourseEntity>,
        gson: Gson,
        courseMapper: CourseMapper
    ): GemmaHelper {
        // 이제 GemmaHelper를 만들 때 모든 재료를 다 던져줍니다.
        return GemmaHelper(context, embeddingHelper, courseBox, gson, courseMapper)
    }
}