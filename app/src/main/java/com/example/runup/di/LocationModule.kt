package com.example.runup.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 앱 전체에서 사용할 수 있도록 설정
object LocationModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context // Hilt가 알아서 Application Context를 넣어줍니다
    ): FusedLocationProviderClient {
        // 구글 위치 서비스를 생성해서 반환하는 방법을 정의
        return LocationServices.getFusedLocationProviderClient(context)
    }
}