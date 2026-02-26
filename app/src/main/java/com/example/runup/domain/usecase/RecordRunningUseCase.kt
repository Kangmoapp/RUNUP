package com.example.runup.domain.usecase

import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.repository.CourseRepository
import com.example.runup.domain.repository.UserRepository

class RecordRunningUseCase(
    private val userRepository: UserRepository,
    private val courserepository: CourseRepository
    //user DB와 course DB 두곳에 전부 코스를 저장해야함
) {
    suspend operator fun invoke(runrecord : RunRecord){

    }
}