package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userlogininfo: UserLoginInfo): AuthResult<Unit> {
        val e = userlogininfo.userEmail.trim()
        val p = userlogininfo.userPw

        if (e.isBlank() || p.isBlank()) {
            return AuthResult.Fail("이메일/비밀번호를 입력해 주세요.")
        }
        return userrepository.login(e, p)
    }
}