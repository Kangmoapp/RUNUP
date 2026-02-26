package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import com.example.runup.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.TUTORIAL)
    val currentScreen: StateFlow<Screen> = _currentScreen

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

}