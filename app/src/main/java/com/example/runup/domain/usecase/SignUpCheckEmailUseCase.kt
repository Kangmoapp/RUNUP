package com.example.runup.domain.usecase

import com.example.runup.domain.repository.UserRepository

class SignUpCheckEmailUseCase(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(useremail: String) : Boolean {
        return true
    }
}