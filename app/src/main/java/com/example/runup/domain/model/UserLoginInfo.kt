package com.example.runup.domain.model

import com.google.gson.annotations.SerializedName

data class UserLoginInfo(
    val userEmail: String,
    val userPw: String
)
// UserLoginInfo.kt 파일 내부 UserData 부분만 수정

data class UserData(
    @SerializedName(value = "userId", alternate = ["uid"])
    val userId: String = "",

    @SerializedName(value = "userEmail", alternate = ["email"])
    val userEmail: String = "",
    val userPassword: String = "",
    @SerializedName(value = "userName", alternate = ["nickname", "name"])
    val userName: String = "",

    @SerializedName(value = "userProfileUrl", alternate = ["profileImageUrl", "imageUrl"])
    val userProfileUrl: String = "",
    val userProfileUrlMini: String = "", // 최적화를 위한 작은 이미지
    val goalDistance: Int = 0, // 목표 거리 (meter 단위)
    val goalTime: Int = 0,     // 목표 시간 (milliseconds 단위)

    // 🌟 [수정/추가] 서버에서 보낸 데이터를 정확히 받을 수 있도록 세팅합니다.
    val totalRunningDistance: Long = 0L, // Int를 Long으로 변경
    val totalRunningCount: Int = 0,
    val totalRunningTime: Int = 0,       // 👈 새로 추가된 '총 시간' 받을 바구니!

    val runs: List<RunRecord> = emptyList(),
    val friends: List<String> = emptyList(),
    val sentRequests: List<String> = emptyList(),
    val receivedRequests: List<String> = emptyList()
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