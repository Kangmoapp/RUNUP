package com.example.runup.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.usecase.LoginUseCase
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.usecase.GoalSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalSettingUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0
)

@HiltViewModel
class GoalSettingViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase
): ViewModel(){

    private val _uiState = MutableStateFlow(GoalSettingUiState())
    val uiState: StateFlow<GoalSettingUiState> = _uiState

    /*

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, errorMessage = null) }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email
        val password = _uiState.value.password

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userlogininfo= UserLoginInfo(email, password)
            when (val result = loginUseCase(userlogininfo)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is AuthResult.Fail -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

     */
}