package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserLoginStatusUseCase @Inject constructor(
    private val userrepository: UserRepository
) {
    suspend operator fun invoke(userLoginStatus:Boolean): AuthResult<Boolean> {
        return userrepository.updateUserLoginStatus(userLoginStatus)
    }
}
//userLoginStatus 에 true -> 로그인 상태
//false -> 로그아웃 상태

