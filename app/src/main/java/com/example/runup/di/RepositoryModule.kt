package com.example.runup.di

import com.example.runup.data.repositoryimpl.CourseRepositoryImpl
import com.example.runup.data.repositoryimpl.UserRepositoryImpl
import com.example.runup.domain.repository.CourseRepository
import com.example.runup.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository // <-- 인터페이스와 구현체를 연결!
}