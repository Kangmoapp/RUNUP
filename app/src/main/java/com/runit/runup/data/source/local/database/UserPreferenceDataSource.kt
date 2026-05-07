package com.runit.runup.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferenceDataSource @Inject constructor(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)

    companion object {
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")
        private val AI_POSTURE_VISIBLE_KEY = booleanPreferencesKey("ai_posture_visible") // ── 🔹 AI 자세 교정 활성화 여부 키 추가 📍 ──
    }

    fun getIsLogin(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGIN_KEY] ?: false
        }
    }

    suspend fun updateUserLoginStatus(isLogin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGIN_KEY] = isLogin
        }
    }

    // ── 🔹 AI 검색 횟수 제한 로직 (SharedPreferences 활용) 📍 ──

    private fun getTodayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun getAiSearchCount(): Int {
        val lastDate = prefs.getString("ai_search_last_date", "")
        val today = getTodayDate()

        // 날짜가 바뀌었으면 카운트 리셋
        if (lastDate != today) {
            prefs.edit()
                .putString("ai_search_last_date", today)
                .putInt("ai_search_count", 0)
                .apply()
            return 0
        }
        return prefs.getInt("ai_search_count", 0)
    }

    fun incrementAiSearchCount() {
        val currentCount = getAiSearchCount()
        prefs.edit().putInt("ai_search_count", currentCount + 1).apply()
    }

    fun resetAiSearchCount() {
        val today = getTodayDate()
        prefs.edit()
            .putString("ai_search_last_date", today) // 날짜를 오늘로 갱신
            .putInt("ai_search_count", 0)            // 횟수를 0으로 초기화
            .apply()
    }

    // ── AI 자세 교정 설정 (SharedPreferences 활용) 📍 ──

    // 읽기: 기본값은 true로 설정하는 것이 사용자에게 친절합니다.
    fun getAiPostureVisible(): Flow<Boolean> = context.dataStore.data.map {
        it[AI_POSTURE_VISIBLE_KEY] ?: false
    }

    // 쓰기
    suspend fun updateAiPostureVisible(isVisible: Boolean) {
        context.dataStore.edit { it[AI_POSTURE_VISIBLE_KEY] = isVisible }
    }
}