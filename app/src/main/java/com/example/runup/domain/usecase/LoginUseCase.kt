package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userlogininfo: UserLoginInfo) : AuthResult<Boolean>{
        return userrepository.login(userlogininfo.userEmail, userlogininfo.userPw)
    }

    // 2. 구글 로그인 전용 invoke 추가
    suspend operator fun invoke(idToken: String): AuthResult<Boolean> {
        return userrepository.signInWithGoogle(idToken)
    }
}