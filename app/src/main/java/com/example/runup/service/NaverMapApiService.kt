package com.example.runup.service


import com.example.runup.domain.model.GeocodingResponse
import com.example.runup.domain.model.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


interface NaverMapApiService {
    @GET("map-direction/v1/driving")
    suspend fun getRoute(
        @Query("start") start: String,
        @Query("goal") goal: String,
        @Query("option") option: String = "trafast"
    ): RouteResponse


    @GET("map-reversegeocode/v2/gc")
    suspend fun reverseGeocode(
        @Query("coords") coords: String, // "경도,위도" 순서
        @Query("orders") orders: String = "addr,roadaddr",
        @Query("output") output: String = "json",
    ): GeocodingResponse
}

