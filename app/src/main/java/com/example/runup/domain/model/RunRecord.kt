package com.example.runup.domain.model

import com.google.firebase.firestore.GeoPoint

data class RunRecord(
    val recordDate: Long = 0L,
    val time: Int = 1000000,
    val course: Course = Course()
)
data class Course(
    val distance: Int = 500,
    val locationPoints: List<GeoPoint> = listOf(
        GeoPoint(35.8888, 128.6103), // 경북대 본관 근처
        GeoPoint(35.8900, 128.6120), // 다른 지점 1
        GeoPoint(35.8915, 128.6145)  // 다른 지점 2
    )// LocationPoint 대신 GeoPoint 사용
)