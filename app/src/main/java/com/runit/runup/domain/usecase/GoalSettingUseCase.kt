package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class GoalSettingUseCase @Inject constructor(
    private val userrepository: UserRepository
){
    suspend operator fun invoke(goaldistance:Int, goaltime:Int): AuthResult<Boolean>{
        userrepository.saveUserGoalToRoom(goaldistance, goaltime)
        return userrepository.updateUserGoal(goaldistance, goaltime)
    }
}