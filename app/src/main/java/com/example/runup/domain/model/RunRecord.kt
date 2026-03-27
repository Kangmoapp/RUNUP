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
    val locationPoints: List<Node> = listOf(),
    val minLat: Double = 0.0, // 최남단
    val maxLat: Double = 0.0, // 최북단
    val minLng: Double = 0.0, // 최서단
    val maxLng: Double = 0.0, // 최동단
    val scores: Scores = Scores(0.0,0.0,0.0)
)

data class Node(
    val locationPoint: GeoPoint = GeoPoint(35.0,128.0),
    val score : Scores = Scores(0.0, 0.0, 0.0)
)

data class Scores(
    val brightScore: Double = 0.0,
    val crowdedScore: Double = 0.0,
    val hardScore: Double = 0.0,
)

data class CoursePathGroup(
    val originCourse: Course, // 기존 코스 정보 (ID 포함)
    val reason: String = "",
    val generatedPaths: List<Path> // 파생된 경로들 (거리와 좌표 리스트)
)

data class Path(
    val distance: Int,           // 경로 거리 (m)
    val points: List<GeoPoint>,  // 실제 좌표 리스트
    val centerPoint: GeoPoint    // 경로의 중심 좌표
)

data class CourseRecommendation(
    val originCourse: Course,
    val reason: String,
    val path: Path // 아까 만든 RecommendedPath 객체
)