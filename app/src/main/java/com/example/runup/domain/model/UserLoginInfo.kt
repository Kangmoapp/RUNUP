package com.example.runup.domain.model

data class UserLoginInfo(
    val userId: Int,
    val userEmail: String,
    val userPw: String
)

data class UserData(
    val userId: Int,
    val userEmail: String,
    val userPassword: String,
    val userName: String,
    val goalDistance: Int, // 목표 거리 (meter 단위)
    val goalTime: Int,     // 목표 시간 (milliseconds 단위)
    val runs: List<RunRecord> // 사용자의 전체 러닝 기록 리스트
)