package com.runit.runup.domain.model

data class UserLoginInfo(
    val userEmail: String,
    val userPw: String
)

data class UserData(
    val userId: String = "",
    val userEmail: String = "",
    val userPassword: String = "",
    val userName: String = "",
    val userProfileUrl: String = "",
    val userProfileUrlMini: String = "", // 최적화를 위한 작은 이미지
    val goalDistance: Int = 0, // 목표 거리 (meter 단위)
    val goalTime: Int = 0,     // 목표 시간 (milliseconds 단위)
    val totalRunningDistance: Int = 0,
    val totalRunningCount: Int = 0,
    val runs: List<RunRecord> = emptyList(),// 사용자의 전체 러닝 기록 리스트

    // 친구 관련 필드 (UID 리스트로 관리)
    val friends: List<String> = emptyList(),
    val sentRequests: List<String> = emptyList(),     // 내가 보낸 신청
    val receivedRequests: List<String> = emptyList()  // 내가 받은 신청
)

fun UserData.toSummary(): FriendSummary {
    return FriendSummary(
        userId = this.userId,
        userEmail = this.userEmail, // 이메일로 검색하니까 이메일도 요약에 포함하는 게 좋겠죠?
        userName = this.userName,
        userProfileUrl = this.userProfileUrl.ifEmpty { this.userProfileUrlMini }
    )
}

// 친구 요약 정보 (리스트에 보여줄 용도)
data class FriendSummary(
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val userProfileUrl: String = ""
)