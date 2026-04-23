package com.example.runup.ui.util

import android.graphics.Bitmap
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.UserData
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStateManager @Inject constructor() {
    // 프로필 비트맵을 담아둘 창고
    private val _profileBitmap = MutableStateFlow<Bitmap?>(null)
    val profileBitmap = _profileBitmap.asStateFlow()

    // 유저 데이터 객체 창고
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    // 업데이트 함수들
    fun updateUserData(data: UserData?) { _userData.value = data }
    fun updateProfileBitmap(bitmap: Bitmap?) { _profileBitmap.value = bitmap }

    // 로그아웃 시 클리어
    fun clear() {
        _userData.value = null
        _profileBitmap.value = null
    }
}