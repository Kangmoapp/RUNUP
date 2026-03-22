package com.example.runup.domain.usecase

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserGoalUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke() : Flow<Pair<Int, Int>?> {
        return userRepository.getUserGoalFromRoom()
    }
}