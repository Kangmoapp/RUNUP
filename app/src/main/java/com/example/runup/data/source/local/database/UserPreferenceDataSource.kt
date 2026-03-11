package com.example.runup.data.local

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
    companion object {
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")
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
}