package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.usecase.GetUserLoginStatusUseCase
import com.example.runup.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val getUserLoginStatusUseCase: GetUserLoginStatusUseCase
) : ViewModel() {
    private val _currentScreen = MutableStateFlow(Screen.TUTORIAL)
    val currentScreen: StateFlow<Screen> = _currentScreen

    // --- 추가된 부분: 상세페이지로 전달할 ID 저장 변수 ---
    var selectedPostId: String = ""
        private set

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            when (val result = getUserLoginStatusUseCase()) {
                is AuthResult.Success -> {
                    _currentScreen.value =
                        if (result.data) Screen.HOME
                        else Screen.TUTORIAL
                }
                is AuthResult.Fail -> {
                    _currentScreen.value = Screen.TEST
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- 추가된 부분: ID를 저장하며 상세페이지로 이동하는 함수 ---
    fun navigateToDetail(postId: String) {
        selectedPostId = postId
        _currentScreen.value = Screen.COMMUNITY_DETAIL
    }
}