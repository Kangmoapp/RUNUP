package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class MyPageUseCase @Inject constructor(
    private val userrepository: UserRepository
){
    suspend operator fun invoke() : AuthResult<UserData> {
        return userrepository.getMyUserData()
    }
}