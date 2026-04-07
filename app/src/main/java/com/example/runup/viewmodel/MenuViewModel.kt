package com.example.runup.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.GoogleAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    val googleAuthManager: GoogleAuthManager,
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
): ViewModel(){
    fun signOutWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                googleAuthManager.signOut(context) // 이미 만들어진 signOut이 있다면 호출
                updateUserLoginStatusUseCase(false)
                onSuccess()
                Log.d("test", "로그아웃 성공 - 이제 다시 로그인해 보세요.")
            } catch (e: Exception) {
                Log.e("test", "로그아웃 실패: ${e.message}")
            }
        }
    }

}