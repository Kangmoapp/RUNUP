package com.example.runup.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetUserLoginStatusUseCase
import com.example.runup.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val getUserLoginStatusUseCase: GetUserLoginStatusUseCase,
    private val locationRepository: LocationRepository,
) : ViewModel() {
    private val _currentScreen = MutableStateFlow(Screen.LOADING)
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _isMenuVisible = MutableStateFlow(false)
    val isMenuVisible: StateFlow<Boolean> = _isMenuVisible

    fun openMenu() {
        _isMenuVisible.value = true
    }

    fun closeMenu() {
        _isMenuVisible.value = false
    }

    fun navigateFromMenu(screen: Screen) {
        _isMenuVisible.value = false
        navigateTo(screen)
    }

    private val _isSplashLoading = MutableStateFlow(true)
    val isSplashLoading: StateFlow<Boolean> = _isSplashLoading

    // 유저 게시물 화면으로 갈 때 필요한 UID 저장
    var selectedTargetUid by mutableStateOf("")
        private set

    // 🔹 지나온 화면들을 저장할 히스토리 스택
    private val screenStack = mutableListOf<Screen>()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            val result = getUserLoginStatusUseCase()

            if (result is AuthResult.Success && result.data) {
                locationRepository.startTracking()
            }
            when (result) {
                is AuthResult.Success -> {
                    _currentScreen.value =
                        if (result.data) Screen.HOME
                        else Screen.START
                }
                is AuthResult.Fail -> {
                    _currentScreen.value = Screen.TEST
                }
            }

            val elapsed = System.currentTimeMillis() - startTime

            if(_currentScreen.value == Screen.HOME){
                if (elapsed < 2000) {
                    delay(2000 - elapsed)
                }
            }
            else {
                if (elapsed < 1000) {
                    delay(1000 - elapsed)
                }
            }

            _isSplashLoading.value = false
        }
    }

    fun DelayToHome(screen: Screen){
        viewModelScope.launch{
            Log.d("Delaytohome", "DelayToHome 호출")
            _currentScreen.value = screen
            _isSplashLoading.value = true
            delay(2000)
            _isSplashLoading.value = false
        }
    }

    // ── 화면 이동 로직 (스택 저장 포함) ──
    fun navigateTo(screen: Screen) {
        // 현재 화면이 이동할 화면과 다를 때만 스택에 저장 (중복 방지)
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen
    }

    // ── [핵심] 이전 화면으로 돌아가기 ──
    fun popBackStack() {
        if (screenStack.isNotEmpty()) {
            // 마지막에 저장된 화면을 꺼내서 현재 화면으로 설정
            val previousScreen = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = previousScreen
        } else {
            // 스택이 비어있다면 (예외 상황) 홈으로 이동
            _currentScreen.value = Screen.HOME
        }
    }

    // 유저 게시물 이동 시에도 스택 저장
    fun navigateToUserPosts(uid: String) {
        screenStack.add(_currentScreen.value) // 현재 화면 저장
        selectedTargetUid = uid
        _currentScreen.value = Screen.USER_POSTS
    }

    // 로그아웃 시 스택 초기화 필수!
    fun logout() {
        screenStack.clear()
        _currentScreen.value = Screen.START
    }
}