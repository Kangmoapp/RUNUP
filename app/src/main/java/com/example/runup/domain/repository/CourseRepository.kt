package com.example.runup.domain.repository

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course

interface CourseRepository {
    suspend fun saveCourse(course: Course): AuthResult<Boolean>
}