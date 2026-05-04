package com.example.runup.data.source.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("RunUpPrefs", Context.MODE_PRIVATE)

    // 1. UID 저장 (로그인 성공 시 호출)
    fun saveUid(uid: String) {
        prefs.edit().putString("MY_UID", uid).apply()
    }

    // 2. UID 불러오기 (ViewModel 등에서 내 정보가 필요할 때 호출)
    fun getUid(): String {
        return prefs.getString("MY_UID", "") ?: ""
    }

    // 3. 로그아웃 (UID 삭제)
    fun clearSession() {
        prefs.edit().remove("MY_UID").apply()
    }
}