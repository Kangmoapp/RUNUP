package com.example.runup.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferenceDataSource @Inject constructor(
    private val context: Context
) {
    companion object {
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")
        private val LAST_LAT_KEY = doublePreferencesKey("last_lat")
        private val LAST_LNG_KEY = doublePreferencesKey("last_lng")
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

    // 🔹 마지막 저장된 좌표 가져오기
    fun getLastLocation(): Flow<Pair<Double, Double>?> {
        return context.dataStore.data.map { preferences ->
            val lat = preferences[LAST_LAT_KEY]
            val lng = preferences[LAST_LNG_KEY]
            if (lat != null && lng != null) lat to lng else null
        }
    }

    // 🔹 새로운 좌표 저장하기
    suspend fun updateLastLocation(lat: Double, lng: Double) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LAT_KEY] = lat
            preferences[LAST_LNG_KEY] = lng
        }
    }
}