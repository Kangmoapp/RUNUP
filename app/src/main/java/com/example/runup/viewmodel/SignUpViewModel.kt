package com.example.runup.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.usecase.SignUpCheckEmailUseCase
import com.example.runup.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val checkEmailUseCase: SignUpCheckEmailUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    // 입력값 변경 함수들
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) = _uiState.update { it.copy(confirmPassword = v, errorMessage = null) }

    //1단계: 이메일 중복 체크
    fun checkEmail(onSuccess: () -> Unit) {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "이메일을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            Log.d("UseCaseTest", "성공!")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = checkEmailUseCase(email)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    if (result.data) {
                        // 중복되지 않은 이메일(사용 가능)일 때 콜백 실행
                        onSuccess()
                    }
                }
                is AuthResult.Fail -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    //2단계: 최종 회원가입

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "비밀번호가 일치하지 않습니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val info = UserLoginInfo(state.email, state.password)
            when (val result = signUpUseCase(info)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess() // 가입 성공 시 콜백 실행
                }
                is AuthResult.Fail -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}