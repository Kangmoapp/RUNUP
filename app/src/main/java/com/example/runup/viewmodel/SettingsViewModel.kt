// com.example.runup.viewmodel.SettingsViewModel.kt

package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.usecase.DeleteUserAccountUseCase
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── 🔹 [추가] 설정을 위한 통합 UI 상태 📍 ──
data class SettingsUiState(
    val isLoading: Boolean = false,
    val deleteResult: AuthResult<Boolean>? = null,
    val notificationEnabled: Boolean = true // 필요한 설정 옵션들 추가
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteUserAccountUseCase: DeleteUserAccountUseCase,
    private val userStateManager: UserStateManager, // 싱글톤 청소용
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
) : ViewModel() {

    // ── 🔹 [수정] 여러 StateFlow를 하나로 통합 📍 ──
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // 알림 설정 토글 (예시)
    fun toggleNotification(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    // 구글 전용 회원 탈퇴 실행
    fun deleteAccount() {
        viewModelScope.launch {
            // 로딩 시작
            _uiState.update { it.copy(isLoading = true) }
            updateUserLoginStatusUseCase.invoke(false)
            val result = deleteUserAccountUseCase.invoke()
            if (result is AuthResult.Success) {
                userStateManager.clear()

            }

            // 결과 업데이트 및 로딩 종료
            _uiState.update { it.copy(
                isLoading = false,
                deleteResult = result
            ) }
        }
    }

    // 결과 상태 초기화 (토스트 띄운 후 호출)
    fun resetDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }
}