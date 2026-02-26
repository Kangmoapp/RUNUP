package com.example.runup.data.repositoryimpl

import com.example.runup.data.source.remote.user.UserDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor (
    private val userremotedatasource: UserDataSource
) : UserRepository {

    // 1. 이메일 중복 체크 구현
    override suspend fun checkuserid(useremail: String): Boolean {
        // TODO: Firebase Auth나 DB를 조회하여 이메일 중복 여부 확인
        return false
    }

    // 2. 이메일, PW 저장 구현 (회원가입)
    override suspend fun saveUserlogininfo(useremail: String, userpw: String): Boolean {
        // TODO: Firebase Auth에 계정 생성 로직 구현
        return false
    }

    // 3. 로그인 구현
    // T는 UseCase에서 기대하는 타입(예: UserLoginInfo 또는 String)에 따라 결정됩니다.
    override suspend fun login(useremail: String, userpw: String): AuthResult<Unit> {
        return try {
            userremotedatasource.loginUser(useremail, userpw)
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Fail(message = e.message ?: "로그인 실패", throwable = e)
        }
    }

    // 4. 달리기 기록 저장 구현
    override suspend fun saveRunRecord(userid: String, record: RunRecord): Boolean {
        // TODO: Firestore의 해당 userid 문서 아래에 record 저장
        return false
    }

    // 5. 달리기 목표 저장 구현
    override suspend fun getGoal(userid: String, goaldistance: Int, goaltime: Int): Boolean {
        // TODO: 사용자 문서의 goalDistance, goalTime 필드 업데이트
        return false
    }

    // 6. 모든 사용자 데이터 반환 구현
    override suspend fun getAllUserData(userid: String): UserData {
        // TODO: Firestore에서 해당 userid의 전체 문서를 가져와 UserData 객체로 변환
        throw Exception("Not yet implemented")
    }
}