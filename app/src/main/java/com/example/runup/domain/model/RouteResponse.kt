package com.example.runup.domain.model

// 응답 데이터 구조 (좌표 추출용)
data class RouteResponse(val route: RouteResult)
data class RouteResult(val trafast: List<TrafastPath>)
data class TrafastPath(val summary: RouteSummary, val path: List<List<Double>>)

data class RouteSummary(
    val distance: Int,   // 전체 거리 (미터 단위)
    val duration: Long   // 예상 소요 시간 (밀리초 단위)
)