package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class GetUserLoginStatusUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(): AuthResult<Boolean> {
        return userrepository.getIsLogin()
    }
}