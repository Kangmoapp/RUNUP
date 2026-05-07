package com.runit.runup.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.usecase.LoginUseCase
import com.runit.runup.domain.usecase.SyncUserGoalServerToRoomUseCase
import com.runit.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.runit.runup.ui.util.GoogleAuthManager
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
            val result = loginUseCase.invoke(idToken)
            when (result) {
                is AuthResult.Success -> {
                    updateUserLoginStatusUseCase(true)
                    syncUserGoalServerToRoomUseCase.invoke()
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is AuthResult.Fail -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}