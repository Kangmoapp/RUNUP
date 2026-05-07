package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(idToken: String): AuthResult<Boolean> {
        return userrepository.signInWithGoogle(idToken)
    }
}