package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.google.firebase.firestore.GeoPoint

interface CourseDataSource {
    suspend fun saveCourse(course: Course) : AuthResult<Boolean>

    suspend fun getNearCourse(
        courseDistance: Int,
        currentLocation: GeoPoint
    ): AuthResult<List<Pair<Int, List<GeoPoint>>>>
}