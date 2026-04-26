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
    private val _profileBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val profileBitmaps = _profileBitmaps.asStateFlow()

    // 유저 데이터 객체 창고
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData = _userData.asStateFlow()

    // 업데이트 함수들
    fun updateUserData(data: UserData?) { _userData.value = data }
    // 🔹 특정 URL의 비트맵 추가/업데이트
    fun updateProfileBitmap(url: String, bitmap: Bitmap?) {
        if (bitmap == null) return
        _profileBitmaps.update { it + (url to bitmap) }
    }

    // 🔹 여러 비트맵 한꺼번에 업데이트 (친구 목록 로드 시 유용)
    fun updateProfileBitmaps(newBitmaps: Map<String, Bitmap>) {
        _profileBitmaps.update { it + newBitmaps }
    }

    fun clear() {
        _userData.value = null
        _profileBitmaps.value = emptyMap() // 🔹 클리어 시 Map 비우기
    }
}