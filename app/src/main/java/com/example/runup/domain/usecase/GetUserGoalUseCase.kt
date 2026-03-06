package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class GetUserGoalUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke() : AuthResult<Pair<Int,Int>> {
        return userRepository.getUserGoal()
    }
}