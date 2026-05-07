package com.runit.runup.domain.usecase

import com.runit.runup.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserGoalUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke() : Flow<Pair<Int, Int>?> {
        return userRepository.getUserGoalFromRoom()
    }
}