package com.example.runup.data.source.remote.user

import com.example.runup.domain.model.UserData

interface UserDataSource {
    //email, pw 로 계정생성 후 데이터베이스에 등록
    suspend fun registerUser(email: String, pw: String): Boolean

    //현재 로그인된 사용자의 상세 정보 가져오기
    suspend fun getMyUserData(): UserData?
    suspend fun loginUser(email: String, pw: String): Unit
    suspend fun isEmailAlreadyRegistered(email: String): Boolean
}