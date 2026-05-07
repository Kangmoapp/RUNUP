package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.UserData
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class MyPageUseCase @Inject constructor(
    private val userrepository: UserRepository
){
    suspend operator fun invoke() : AuthResult<UserData> {
        return userrepository.getMyUserData()
    }
}