package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userlogininfo: UserLoginInfo): AuthResult<Boolean> {
        return userrepository.saveUserlogininfo(userlogininfo.userEmail, userlogininfo.userPw)
    }
}