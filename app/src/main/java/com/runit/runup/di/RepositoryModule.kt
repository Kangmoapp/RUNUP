package com.runit.runup.di

import com.runit.runup.data.repository.LocationRepositoryImpl
import com.runit.runup.data.repositoryimpl.CourseRepositoryImpl
import com.runit.runup.data.repositoryimpl.UserRepositoryImpl
import com.runit.runup.domain.repository.CourseRepository
import com.runit.runup.domain.repository.LocationRepository
import com.runit.runup.domain.repository.UserRepository
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
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository // <-- 인터페이스와 구현체를 연결!
}