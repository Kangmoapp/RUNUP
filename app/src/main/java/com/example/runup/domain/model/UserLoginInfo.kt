package com.example.runup.domain.model

data class UserLoginInfo(
    val userEmail: String,
    val userPw: String
)

data class UserData(
    val userId: String = "",
    val userEmail: String = "",
    val userPassword: String = "",
    val userName: String = "",
    val goalDistance: Int = 0, // 목표 거리 (meter 단위)
    val goalTime: Int = 0,     // 목표 시간 (milliseconds 단위)
    val runs: List<RunRecord> = emptyList()// 사용자의 전체 러닝 기록 리스트
)