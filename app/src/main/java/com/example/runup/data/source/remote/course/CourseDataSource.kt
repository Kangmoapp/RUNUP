package com.example.runup.data.source.remote.course

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course

interface CourseDataSource {
    suspend fun saveCourse(course: Course) : AuthResult<Boolean>
}