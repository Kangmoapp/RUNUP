package com.example.runup.data.source.remote.user

import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserActivityStats
import com.example.runup.domain.model.UserData
import kotlinx.coroutines.coroutineScope

interface UserDataSource {
    // 이메일 중복체크 (중복 여부를 Boolean으로 반환)
    suspend fun isEmailAlreadyRegistered(email: String): AuthResult<Boolean>

    // email, pw 로 계정생성 후 데이터베이스에 등록
    suspend fun registerUser(email: String, pw: String): AuthResult<Boolean>

    // 구글로 로그인
    suspend fun signInWithGoogle(idToken: String): AuthResult<Boolean>

    // 로그인 성공 여부 반환
    suspend fun loginUser(email: String, pw: String): AuthResult<Boolean>

    // 사용자 이름 추가/수정
    suspend fun updateUserName(name: String): AuthResult<Boolean>

    // 유저 프로필 이미지 업데이트
    suspend fun uploadUserProfileImage(imageUri: android.net.Uri): AuthResult<String>

    // 현재 로그인된 사용자 목표(거리, 시간) 추가
    suspend fun updateUserGoal(goalDistance: Int, goalTime: Int): AuthResult<Boolean>

    // 사용자 러닝 기록 추가 (중첩된 Course 데이터 포함)
    suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean>

    // 사용자 러닝 기록 삭제
    suspend fun deleteRunRecord(courseId: String): AuthResult<Boolean>

    // 현재 로그인된 사용자의 상세 정보 가져오기 (실패 시 에러 메시지 포함 가능)
    suspend fun getMyUserData(): AuthResult<UserData>

    suspend fun getRunsPaged(
        filter: RunFilter,
        lastDate: Long?,
        pageSize: Long
    ): AuthResult<List<RunRecord>>

    // 목표 가져오기
    suspend fun getUserGoal(): AuthResult<Pair<Int,Int>>

    // 유저 커뮤니티 활동 통계 가져오기
    suspend fun getUserActivityStats(uid: String): AuthResult<UserActivityStats>


    //-------------------------------------------------------------------------------------//
    // 친구 기능
    // 1. ID로 사용자 검색
    suspend fun searchUserByEmail(searchId: String): AuthResult<UserData>

    // 2. 친구 신청 보내기
    suspend fun sendFriendRequest(targetUid: String): AuthResult<Boolean>

    // 3. 친구 신청 수락
    suspend fun acceptFriendRequest(targetUid: String): AuthResult<Boolean>

    // 4. 친구 신청 거절/취소
    suspend fun declineFriendRequest(targetUid: String): AuthResult<Boolean>

    // 5. UID 리스트로 여러 사용자 요약 정보 가져오기 (친구 목록/신청 목록용)
    suspend fun getUsersSummary(uidList: List<String>): AuthResult<List<UserData>>

    // 친구 목록 가져오기
    suspend fun getFriendUids(): AuthResult<List<String>>

    // 친구 삭제
    suspend fun deleteFriend(targetUid: String): AuthResult<Boolean>

    suspend fun deletePersonalUserData(): AuthResult<Boolean>

    suspend fun deleteAuthAccount(): AuthResult<Boolean>
}