package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.RunRecord
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class GetUserRunningRecordUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AuthResult<List<RunRecord>>{
        val runRecords = userRepository.getMyUserData()
        when (runRecords) {
            is AuthResult.Success -> {
                val data = runRecords.data // 여기서 UserData를 꺼낼 수 있습니다.
                // 데이터 사용 로직
                return AuthResult.Success(data.runs)
            }
            is AuthResult.Fail -> {
                return AuthResult.Fail(runRecords.message)
            }
        }
    }
}