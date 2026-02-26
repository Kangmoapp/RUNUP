package com.example.runup.data.source.remote.user

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData

interface UserDataSource {
    // 1. 이메일 중복체크 (중복 여부를 Boolean으로 반환)
    suspend fun isEmailAlreadyRegistered(email: String): AuthResult<Boolean>

    // 2. email, pw 로 계정생성 후 데이터베이스에 등록
    suspend fun registerUser(email: String, pw: String): AuthResult<Boolean>

    // 3. 로그인 성공 여부 반환
    suspend fun loginUser(email: String, pw: String): AuthResult<Boolean>

    // 4. 사용자 이름 추가/수정
    suspend fun updateUserName(userid: String, name: String): AuthResult<Boolean>

    // 5. 현재 로그인된 사용자 목표(거리, 시간) 추가
    suspend fun updateUserGoal(goalDistance: Int, goalTime: Int): AuthResult<Boolean>

    // 6. 사용자 러닝 기록 추가 (중첩된 Course 데이터 포함)
    suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean>

    // 7. 현재 로그인된 사용자의 상세 정보 가져오기 (실패 시 에러 메시지 포함 가능)
    suspend fun getMyUserData(): AuthResult<UserData>
}