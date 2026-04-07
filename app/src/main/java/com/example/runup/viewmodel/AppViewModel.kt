package com.example.runup.viewmodel

import android.util.Log
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

    private val _isSplashLoading = MutableStateFlow(true)
    val isSplashLoading: StateFlow<Boolean> = _isSplashLoading

    // --- 추가된 부분: 상세페이지로 전달할 ID 저장 변수 ---
    var selectedPostId: String = ""
        private set

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

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- 추가된 부분: ID를 저장하며 상세페이지로 이동하는 함수 ---
    fun navigateToDetail(postId: String) {
        selectedPostId = postId
        _currentScreen.value = Screen.COMMUNITY_DETAIL
    }
}