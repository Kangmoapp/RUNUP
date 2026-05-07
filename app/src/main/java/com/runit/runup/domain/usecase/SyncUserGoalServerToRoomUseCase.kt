package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class SyncUserGoalServerToRoomUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AuthResult<Boolean> {
        return userRepository.syncUserGoalFromServer()
    }
}