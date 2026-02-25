package com.example.runup.domain.usecase

import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository

class LoginUseCase(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userlogininfo: UserLoginInfo) : String{
        return "test"
    }
}