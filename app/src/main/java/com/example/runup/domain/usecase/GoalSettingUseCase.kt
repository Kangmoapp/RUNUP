package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository

class GoalSettingUseCase(
    private val userrepository: UserRepository
){
    suspend operator fun invoke(goaldistance:Int, goaltime:Int): AuthResult<Boolean>{
        return userrepository.updateUserGoal(goaldistance, goaltime)
    }
}