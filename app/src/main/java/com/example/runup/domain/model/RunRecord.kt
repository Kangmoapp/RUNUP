package com.example.runup.domain.model

import com.google.firebase.firestore.GeoPoint

data class RunRecord(
    val recordDate: Long = 0L,
    val time: Int = 1000000,
    val course: Course = Course()
)

data class Course(
    val id: String = "1",
    val distance: Int = 500,
    val locationPoints: List<GeoPoint> = listOf(),
    val minLat: Double = 0.0, // 최남단
    val maxLat: Double = 0.0, // 최북단
    val minLng: Double = 0.0, // 최서단
    val maxLng: Double = 0.0, // 최동단
)