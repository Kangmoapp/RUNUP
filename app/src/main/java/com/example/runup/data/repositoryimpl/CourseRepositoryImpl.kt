package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.course.CourseDataSourceImpl
import com.example.runup.domain.repository.CourseRepository
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor (
    private val courseremotedatasource: CourseDataSourceImpl
): CourseRepository {

}