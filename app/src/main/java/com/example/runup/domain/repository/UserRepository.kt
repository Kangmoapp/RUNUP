package com.example.runup.domain.repository
import android.net.Uri
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserActivityStats
import com.example.runup.domain.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // 이메일 중복 체크
    suspend fun checkuseremail(useremail: String): AuthResult<Boolean>

    // 이메일, pw 저장 (회원가입)
    suspend fun saveUserlogininfo(useremail: String, userpw: String): AuthResult<Boolean>

    // 로그인 (일치 여부 확인)
    suspend fun login(useremail: String, userpw: String): AuthResult<Boolean>

    // 구글로 로그인
    suspend fun signInWithGoogle(idToken: String): AuthResult<Boolean>

    // 사용자 이름 업데이트
    suspend fun updateUserName(username: String): AuthResult<Boolean>

    suspend fun uploadUserProfileImage(imageUri: Uri): AuthResult<String>

    // 나의 달리기 목표 저장
    suspend fun updateUserGoal(goaldistance: Int, goaltime: Int): AuthResult<Boolean>

    // 나의 달리기 기록 저장
    suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean>

    // 나의 달리기 기록 삭제
    suspend fun deleteRunRecord(courseId: String): AuthResult<Boolean>

    // 현재 로그인 된 사용자의 모든 데이터 반환 (마이페이지)
    suspend fun getMyUserData(): AuthResult<UserData>

    suspend fun getRunsPaged(
        filter: RunFilter,
        lastDate: Long?,
        pageSize: Long
    ): AuthResult<List<RunRecord>>

    // 회원 탈퇴
    suspend fun deleteUserAccount(userpw: String): AuthResult<Boolean>

    // 사용자 목표 가져오기
    suspend fun getUserGoal(): AuthResult<Pair<Int,Int>>

    //room DB 사용자 값 저장은 suspend 가져오는건 suspend x
    // 사용자 목표 저장
    suspend fun saveUserGoalToRoom(goaldistance: Int, goaltime: Int): AuthResult<Boolean>

    // 사용자 목표 가져오기
    //suspend fun getUserGoalFromRoom(): AuthResult<Pair<Int, Int>>
    fun getUserGoalFromRoom(): Flow<Pair<Int, Int>?>

    // 사용자 삭제
    suspend fun deleteUserGoalFromRoom(): AuthResult<Boolean>

    // 사용자 로그인 상태 업데이트
    suspend fun updateUserLoginStatus(loginStatus:Boolean): AuthResult<Boolean>

    // 사용자 로그인 상태 가져오기
    suspend fun getIsLogin(): AuthResult<Boolean>

    suspend fun getUserActivityStats(uid: String): AuthResult<UserActivityStats>

    // --------------------------------------------------------------------------------//
    //친구 기능

    suspend fun searchUserByEmail(searchId: String): AuthResult<UserData>

    suspend fun sendFriendRequest(targetUid: String): AuthResult<Boolean>

    suspend fun acceptFriendRequest(targetUid: String): AuthResult<Boolean>

    suspend fun declineFriendRequest(targetUid: String): AuthResult<Boolean>

    suspend fun getUsersSummary(uidList: List<String>): AuthResult<List<UserData>>

    suspend fun getFriendUids(): AuthResult<List<String>>

    suspend fun deleteFriend(targetUid: String): AuthResult<Boolean>

    // 기존 UserRepository.kt 인터페이스 파일에 추가
    suspend fun getPendingRequests(): AuthResult<List<com.example.runup.domain.model.FriendSummary>>
    suspend fun getMyFriends(): AuthResult<List<com.example.runup.domain.model.FriendSummary>>
}