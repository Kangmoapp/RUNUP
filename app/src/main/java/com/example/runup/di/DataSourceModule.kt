package com.example.runup.di

import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.data.source.remote.course.CourseDataSourceImpl
import com.example.runup.data.source.remote.user.UserDataSource
import com.example.runup.data.source.remote.user.UserDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindUserDataSource(
        impl: UserDataSourceImpl
    ): UserDataSource

    @Binds
    @Singleton
    abstract fun bindCourseDataSource(
        courseDataSourceImpl: CourseDataSourceImpl
    ): CourseDataSource // <-- 인터페이스와 구현체 연결
}