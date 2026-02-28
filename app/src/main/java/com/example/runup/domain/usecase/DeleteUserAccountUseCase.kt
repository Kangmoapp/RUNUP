package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class DeleteUserAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke(userpw : String) : AuthResult<Boolean> {
        return userRepository.deleteUserAccount(userpw)
    }
}