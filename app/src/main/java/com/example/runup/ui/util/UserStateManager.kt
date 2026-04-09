package com.example.runup.ui.util

import android.graphics.Bitmap
import com.example.runup.domain.model.Post
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

    // 비트맵 업데이트 함수
    fun updateProfileBitmap(bitmap: Bitmap?) {
        _profileBitmap.value = bitmap
    }
}