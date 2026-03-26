package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.CoursePathGroup
import com.google.firebase.firestore.GeoPoint

interface CourseDataSource {
    suspend fun saveCourse(course: Course) : AuthResult<Boolean>

    suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        featureIndex: Int
    ): AuthResult<List<CoursePathGroup>>

    suspend fun getCourseFromAI(
        courseDistance: Int,
        currentLocation: GeoPoint,
        userPrompt: String,
    ): AuthResult<List<CoursePathGroup>>

    suspend fun startRealtimeSync()

}