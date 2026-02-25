package com.example.runup.domain.repository
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData

interface UserRepository {
    suspend fun checkuserid (useremail : String) : Boolean
    //이메일  중복체크

    suspend fun saveUserlogininfo(useremail:String, userpw: String) : Boolean
    //이메일 ,pw 저장

    suspend fun <T> login(useremail: String, userpw: String): T
    //로그인 일치하면 UserLoginInfo, 아니면 에러메시지

    suspend fun saveRunRecord(userid: String, record: RunRecord) : Boolean
    //달리기 기록 저장

    suspend fun getGoal(userid: String, goaldistance: Int, goaltime: Int) : Boolean
    //달리기 목표 저장

    suspend fun getAllUserData(userid: String) : UserData
    //해당 아이디의 사용자 모든 데이터 반환 (마이페이지에서 사용)
}