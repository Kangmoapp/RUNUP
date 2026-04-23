package com.example.runup.service

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TMapApiService {
    @POST("tmap/routes/pedestrian?version=1")
    suspend fun getPedestrianRoute(
        @Header("appKey") appKey: String,
        @Query("searchOption") searchOption: String = "4",
        @Body requestBody: TMapRouteRequest
    ): TMapRouteResponse
}

// 요청 데이터 구조
data class TMapRouteRequest(
    val startX: Double, // 경도
    val startY: Double, // 위도
    val endX: Double,
    val endY: Double,
    val startName: String = "출발지",
    val endName: String = "목적지"
)

data class TMapRouteResponse(
    val features: List<Feature>
)

data class Feature(
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val type: String, // "Point" 또는 "LineString"
    val coordinates: Any // 좌표 데이터 (단일 좌표 혹은 좌표 리스트가 섞여서 옴)
)

data class Properties(
    val totalDistance: Int, // 전체 거리(m)
    val totalTime: Int      // 전체 시간(초)
)