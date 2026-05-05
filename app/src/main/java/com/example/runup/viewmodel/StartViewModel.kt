package com.example.runup.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.usecase.LoginUseCase
import com.example.runup.domain.usecase.SyncUserGoalServerToRoomUseCase
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.GoogleAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
@HiltViewModel
class StartViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
    private val syncUserGoalServerToRoomUseCase: SyncUserGoalServerToRoomUseCase,
    val googleAuthManager: GoogleAuthManager
): ViewModel(){

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState

    // 구글 로그인 통합 함수
    fun onGoogleLoginClick(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // GoogleAuthManager를 실행 (ViewModelScope 사용)
            googleAuthManager.signIn(
                context = context,
                scope = this, // viewModelScope 전달
                onTokenReceived = { idToken ->
                    signInWithGoogle(idToken, onSuccess)
                }
            )
        }
    }

    private fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {

            Log.d("Delaytohome", "signInWithGoogle 호출")
            val result = loginUseCase.invoke(idToken)
            Log.d("test", "${result}")
            when (result) {
                is AuthResult.Success -> {

                    Log.d("Delaytohome", "signInWithGoogle 성공")
                    updateUserLoginStatusUseCase(true)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is AuthResult.Fail -> {
                    Log.e("Delaytohome", "signInWithGoogle 실패: ${result.message}")
                    Log.d("Delaytohome", "token: $idToken")
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}