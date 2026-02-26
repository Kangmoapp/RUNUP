package com.example.runup.domain.model

import com.google.firebase.firestore.GeoPoint

data class RunRecord(
    val recordDate: Long = 0L,
    val time: Int = 0,
    val course: Course = Course()
)
data class Course(
    val distance: Int = 0,
    val locationPoints: List<GeoPoint> = emptyList() // LocationPoint 대신 GeoPoint 사용
)