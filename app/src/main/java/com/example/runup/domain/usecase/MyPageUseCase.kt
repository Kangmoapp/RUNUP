package com.example.runup.domain.usecase

import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.UserRepository

class MyPageUseCase(
    private val userrepository: UserRepository
){
    suspend operator fun invoke() : UserData {
        return UserData(
            userId = 0,
            userEmail = "test@test.com",
            userPassword = "****",
            userName = "윤석",
            goalDistance = 0,
            goalTime = 0,
            runs = emptyList() // 빈 리스트 리턴
        )
    }
}