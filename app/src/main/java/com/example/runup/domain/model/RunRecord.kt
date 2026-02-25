package com.example.runup.domain.model

data class RunRecord(
    val recordDate: Long, // 달린 날짜 및 시간 (Timestamp)
    val time: Int,        // 달린 시간 (초 단위 권장)
    val course: Course    // 해당 러닝의 경로 정보
)
data class Course(
    val distance: Int,                 // 총 달린 거리 (meter)
    val locationPoints: List<LocationPoint> // 좌표들의 연속된 리스트
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double
)