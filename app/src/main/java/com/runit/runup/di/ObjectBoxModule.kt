package com.runit.runup.di

import android.content.Context
import androidx.viewbinding.BuildConfig
import com.runit.runup.data.source.local.objectbox.entity.CourseEntity
import com.runit.runup.data.source.local.objectbox.entity.MyObjectBox
import com.runit.runup.ui.util.mapper.CourseMapper
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.BoxStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectBoxModule {

    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext context: Context): BoxStore {
        // Build -> Make Project를 완료해야 MyObjectBox가 인식됩니다.
        val store = MyObjectBox.builder().androidContext(context).build()

        if (BuildConfig.DEBUG) {
            // 이 코드가 실행되면 폰의 알림창에 "ObjectBox Admin"이 뜹니다.
            io.objectbox.android.Admin(store).start(context)
        }
        return store
    }

    // 필요한 엔티티의 Box를 편리하게 주입받기 위해 추가
    @Provides
    fun provideCourseBox(boxStore: BoxStore): Box<CourseEntity> {
        return boxStore.boxFor(CourseEntity::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    // 2. CourseMapper 자체를 Singleton으로 제공합니다.
    @Provides
    @Singleton
    fun provideCourseMapper(gson: Gson): CourseMapper {
        return CourseMapper(gson)
    }
}
