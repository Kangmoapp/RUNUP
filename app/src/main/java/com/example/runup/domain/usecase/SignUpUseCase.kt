package com.example.runup.domain.usecase

import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository

class SignUpUseCase(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userlogininfo: UserLoginInfo){

    }
}