package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.CoursePathGroup
import com.example.runup.domain.model.SortDirection
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.CourseRepository
import com.google.firebase.firestore.GeoPoint
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor (
    private val courseDataSource: CourseDataSource
): CourseRepository {
    override suspend fun saveCourse(course: Course): AuthResult<Boolean>{
        return courseDataSource.saveCourse(course)
    }

    override suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType,
        maxSearchDistance: Int, // 📍 추가
        sortDirection: SortDirection // 📍 추가
    ): AuthResult<List<CoursePathGroup>>{
        return courseDataSource.getCourse(courseDistance, currentLocation, isLoop, sortType, maxSearchDistance, sortDirection )
    }

    override suspend fun getCourseFromAI(
        courseDistance: Int, // 몇 m 뛸껀지
        currentLocation: GeoPoint, //
        currentAddress: String,
        isLoop: Boolean,
        userPrompt: String,
        maxSearchDistance: Int
    ): AuthResult<List<CoursePathGroup>>{
        return courseDataSource.getCourseFromAI(courseDistance, currentLocation, currentAddress, isLoop, userPrompt, maxSearchDistance)
    }
}