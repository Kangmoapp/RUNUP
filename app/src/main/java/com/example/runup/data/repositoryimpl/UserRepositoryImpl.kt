package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.user.UserDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userdatasource: UserDataSource
) : UserRepository {
    // 1. 이메일 중복 체크
    override suspend fun checkuseremail(useremail: String): AuthResult<Boolean> {
        return userdatasource.isEmailAlreadyRegistered(useremail)
    }
    // 2. 이메일, PW 저장
    override suspend fun saveUserlogininfo(useremail: String, userpw: String): AuthResult<Boolean> {
        return userdatasource.registerUser(useremail, userpw)
    }
    // 3. 로그인
    override suspend fun login(useremail: String, userpw: String): AuthResult<Boolean> {
        return userdatasource.loginUser(useremail, userpw)
    }
    // 4. 사용자 이름 업데이트
    override suspend fun updateUserName(useremail: String, username: String): AuthResult<Boolean> {
        return userdatasource.updateUserName(useremail, username)
    }
    // 5. 달리기 목표 저장
    override suspend fun updateUserGoal(goaldistance: Int, goaltime: Int): AuthResult<Boolean> {
        return userdatasource.updateUserGoal(goaldistance, goaltime)
    }
    // 6. 달리기 기록 저장
    override suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean> {
        return userdatasource.saveRunRecord(record)
    }
    // 7. 내 데이터 가져오기
    override suspend fun getMyUserData(): AuthResult<UserData> {
        return userdatasource.getMyUserData()
    }
}