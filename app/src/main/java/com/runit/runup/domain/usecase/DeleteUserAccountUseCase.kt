package com.runit.runup.domain.usecase

import com.runit.runup.data.source.remote.community.CommunityDataSourceImpl
import com.runit.runup.data.source.remote.user.UserDataSource
import com.runit.runup.domain.model.AuthResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import javax.inject.Inject

class DeleteUserAccountUseCase @Inject constructor(
    private val userDataSource: UserDataSource,
    private val communityDataSource: CommunityDataSourceImpl
){
    suspend operator fun invoke(): AuthResult<Boolean> {
        // 1. UID 가져오기 (null 체크 포함)
        val uid = Firebase.auth.uid
            ?: return AuthResult.Fail("로그인 정보가 없습니다.")

        // 2. 활동 통계 확보 (AuthResult.Success 여부 확인) 📍
        val statsResult = userDataSource.getUserActivityStats(uid)

        if (statsResult is AuthResult.Fail) {
            return AuthResult.Fail("활동 기록 조회 실패: ${statsResult.message}")
        }

        // Success인 경우 데이터 추출
        val stats = (statsResult as AuthResult.Success).data

        // 3. 커뮤니티 데이터 정리 (내가 쓴 글 + 흔적)
        communityDataSource.deleteAllMyCommunityData(
            uid = uid,
            uploadPostIds = stats.uploadPostIds,
            likePostIds = stats.likePostIds,
            commentPostIds = stats.commentPostIds,
            followPostIds = stats.followPostIds
        )

        // 4. 개인 유저 데이터 삭제
        userDataSource.deletePersonalUserData()

        // 5. 마지막으로 인증 계정 삭제
        return userDataSource.deleteAuthAccount()
    }
}