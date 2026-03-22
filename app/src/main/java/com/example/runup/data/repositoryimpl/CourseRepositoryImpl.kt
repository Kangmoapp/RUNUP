package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.repository.CourseRepository
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor (
    private val courseDataSource: CourseDataSource
): CourseRepository {
    override suspend fun saveCourse(course: Course): AuthResult<Boolean>{
        return courseDataSource.saveCourse(course)
    }
}