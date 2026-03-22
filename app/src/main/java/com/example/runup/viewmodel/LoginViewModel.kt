package com.example.runup.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.usecase.LoginUseCase
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.GoogleAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
    val googleAuthManager: GoogleAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(v: String) = _uiState.update {
        it.copy(email = v, errorMessage = null)
    }

    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(password = v, errorMessage = null)
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email
        val password = _uiState.value.password

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userlogininfo = UserLoginInfo(email, password)

            when (val result = loginUseCase(userlogininfo)) {
                is AuthResult.Success -> {
                    updateUserLoginStatusUseCase(true)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }

                is AuthResult.Fail -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

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