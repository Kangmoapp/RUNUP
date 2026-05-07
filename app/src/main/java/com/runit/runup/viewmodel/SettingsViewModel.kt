// com.example.runup.viewmodel.SettingsViewModel.kt

package com.runit.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runit.runup.data.local.UserPreferenceDataSource
import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.usecase.DeleteUserAccountUseCase
import com.runit.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.runit.runup.ui.util.UserStateManager
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
    val notificationEnabled: Boolean = true, // 필요한 설정 옵션들 추가
    val isAiPostureVisible: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteUserAccountUseCase: DeleteUserAccountUseCase,
    private val userStateManager: UserStateManager, // 싱글톤 청소용
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
    private val userPreferenceDataSource: UserPreferenceDataSource,
) : ViewModel() {

    // ── 🔹 [수정] 여러 StateFlow를 하나로 통합 📍 ──
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // 알림 설정 토글 (예시)
    fun toggleNotification(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // 1. 로딩 시작
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 2. 🚨 서버(Firebase) 계정 및 데이터 삭제를 '먼저' 실행해야 권한 에러가 안 납니다!
                val result = deleteUserAccountUseCase.invoke()

                if (result is AuthResult.Success) {
                    // 3. 서버 삭제가 성공했을 때만 로컬 로그인 상태를 해제하고 찌꺼기를 지웁니다.
                    updateUserLoginStatusUseCase.invoke(false)
                    userStateManager.clear()
                }

                // 4. 로딩 종료 및 결과 업데이트
                _uiState.update { it.copy(
                    isLoading = false,
                    deleteResult = result
                )}

            } catch (e: Exception) {
                // 5. 만약 예상치 못한 에러로 앱이 터지려 해도 여기서 잡아서 로딩을 꺼줍니다.
                _uiState.update { it.copy(
                    isLoading = false,
                    deleteResult = AuthResult.Fail(e.message ?: "탈퇴 중 알 수 없는 오류 발생")
                )}
            }
        }
    }

    // 결과 상태 초기화 (토스트 띄운 후 호출)
    fun resetDeleteResult() {
        _uiState.update { it.copy(deleteResult = null) }
    }

    init {
        // ── 🔹 앱 시작 시 저장된 설정값 불러오기 📍 ──
        viewModelScope.launch {
            userPreferenceDataSource.getAiPostureVisible().collect { isVisible ->
                _uiState.update { it.copy(isAiPostureVisible = isVisible) }
            }
        }
    }

    // 스위치 토글 함수
    fun toggleAiPostureVisible(isVisible: Boolean) {
        viewModelScope.launch {
            userPreferenceDataSource.updateAiPostureVisible(isVisible)
        }
    }
}