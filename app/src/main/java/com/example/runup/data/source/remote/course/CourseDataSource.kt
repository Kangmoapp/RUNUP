package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.google.firebase.firestore.GeoPoint

interface CourseDataSource {
    suspend fun saveCourse(course: Course) : AuthResult<Boolean>

    suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        featureIndex: Int
    ): AuthResult<List<List<Pair<Int, List<GeoPoint>>>>>

    suspend fun startRealtimeSync()

    suspend fun getSearchResult(queryText: String): List<Course>
}