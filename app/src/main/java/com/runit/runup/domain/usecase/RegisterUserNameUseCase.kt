package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class RegisterUserNameUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke(username : String) : AuthResult<Boolean> {
        return userRepository.updateUserName(username)
    }
}