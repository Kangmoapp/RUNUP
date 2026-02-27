package com.example.runup.domain.repository
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData

interface UserRepository {
    // 1. 이메일 중복 체크
    suspend fun checkuseremail(useremail: String): AuthResult<Boolean>

    // 2. 이메일, pw 저장 (회원가입)
    suspend fun saveUserlogininfo(useremail: String, userpw: String): AuthResult<Boolean>

    // 3. 로그인 (일치 여부 확인)
    suspend fun login(useremail: String, userpw: String): AuthResult<Boolean>

    // 4. 사용자 이름 업데이트
    suspend fun updateUserName(username: String): AuthResult<Boolean>

    // 5. 나의 달리기 목표 저장
    suspend fun updateUserGoal(goaldistance: Int, goaltime: Int): AuthResult<Boolean>

    // 6. 나의 달리기 기록 저장
    suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean>

    // 7. 현재 로그인 된 사용자의 모든 데이터 반환 (마이페이지)
    suspend fun getMyUserData(): AuthResult<UserData>
}