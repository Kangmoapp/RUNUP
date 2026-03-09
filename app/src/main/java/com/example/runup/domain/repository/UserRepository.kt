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

    // 8. 회원 탈퇴
    suspend fun deleteUserAccount(userpw: String): AuthResult<Boolean>

    //9. 사용자 목표 가져오기
    suspend fun getUserGoal(): AuthResult<Pair<Int,Int>>

    //room DB
    //1. 사용자 목표 저장
    suspend fun saveUserGoalToRoom(goaldistance: Int, goaltime: Int): AuthResult<Boolean>

    //2. 사용자 목표 가져오기
    suspend fun getUserGoalFromRoom(): AuthResult<Pair<Int, Int>>

    //3. 사용자 삭제
    suspend fun deleteUserGoalFromRoom(): AuthResult<Boolean>

    //4. 사용자 로그인 상태 업데이트
    suspend fun updateUserLoginStatus(loginStatus:Boolean): AuthResult<Boolean>

    //5. 사용자 로그인 상태 가져오기
    suspend fun getIsLogin(): AuthResult<Boolean>
}