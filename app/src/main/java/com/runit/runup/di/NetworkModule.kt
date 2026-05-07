package com.runit.runup.di

import com.runit.runup.BuildConfig
import com.runit.runup.service.GovLocationApiService
import com.runit.runup.service.NaverMapApiService
import com.runit.runup.service.TMapApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    // 🔹 여기서 아예 고정해서 보내면 실수를 방지할 수 있습니다.
                    .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                    .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideNaverApiService(okHttpClient: OkHttpClient): NaverMapApiService {
        return Retrofit.Builder()
            .baseUrl("https://maps.apigw.ntruss.com/") // 🔹 개인용 URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverMapApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("TmapRetrofit") // 네이버와 구분하기 위해 이름을 붙입니다.
    fun provideTmapRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://apis.openapi.sk.com/") // 🔹 Tmap 전용 URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTmapApiService(@Named("TmapRetrofit") retrofit: Retrofit): TMapApiService {
        return retrofit.create(TMapApiService::class.java)
    }

    // 🔹 [수정/추가] 공공데이터용 Retrofit (BaseURL이 다르므로 별도 생성)
    @Provides
    @Singleton
    @Named("GovRetrofit") // 이름을 붙여서 다른 Retrofit과 확실히 구분합니다. 🔹
    fun provideGovRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 🔹 [수정] GovLocationApiService 제공 로직
    @Provides
    @Singleton
    fun provideGovLocationApiService(@Named("GovRetrofit") retrofit: Retrofit): GovLocationApiService {
        // @Named("GovRetrofit")을 붙여서 위에서 만든 공공데이터용 Retrofit을 쓰라고 지정합니다. 🔹
        return retrofit.create(GovLocationApiService::class.java)
    }

}