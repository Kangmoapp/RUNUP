package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserLoginStatusUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userLoginStatus: Boolean): AuthResult<Boolean> {
        return userrepository.updateUserLoginStatus(userLoginStatus)
    }
}

