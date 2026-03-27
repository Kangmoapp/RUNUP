package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.CoursePathGroup
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
        sortType: SortType
    ): AuthResult<List<CoursePathGroup>>{
        return courseDataSource.getCourse(courseDistance, currentLocation, isLoop, sortType)
    }

    override suspend fun getCourseFromAI(
        courseDistance: Int, // 몇 m 뛸껀지
        currentLocation: GeoPoint, //
        isLoop: Boolean,
        userPrompt: String,
    ): AuthResult<List<CoursePathGroup>>{
        return courseDataSource.getCourseFromAI(courseDistance, currentLocation, isLoop, userPrompt)
    }
}