package com.runit.runup.data.source.remote.course

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.Course
import com.runit.runup.domain.model.CoursePathGroup
import com.runit.runup.domain.model.SortDirection
import com.runit.runup.domain.model.SortType
import com.google.firebase.firestore.GeoPoint

interface CourseDataSource {
    suspend fun saveCourse(course: Course) : AuthResult<Boolean>

    suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType,
        maxSearchDistance: Int, // 📍 추가 (단위: m)
        sortDirection: SortDirection // 📍 추가 (ASC, DESC)
    ): AuthResult<List<CoursePathGroup>>

    suspend fun getCourseFromAI(
        courseDistance: Int, // 몇 m 뛸껀지
        currentLocation: GeoPoint, //
        currentAddress : String,
        isLoop: Boolean,
        userPrompt: String,
        maxSearchDistance: Int,
    ): AuthResult<List<CoursePathGroup>>
}