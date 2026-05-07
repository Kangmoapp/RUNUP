package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class SignUpCheckEmailUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(useremail: String) : AuthResult<Boolean> {
        return userrepository.checkuseremail(useremail)
    }
}