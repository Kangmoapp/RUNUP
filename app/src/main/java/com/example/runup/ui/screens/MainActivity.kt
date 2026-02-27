package com.example.runup.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.runup.viewmodel.AppViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.runup.ui.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunUpApp()
        }
    }
}
@Composable
fun RunUpApp(
    viewModel: AppViewModel = viewModel()
) {

    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {

        Screen.TUTORIAL -> TutorialScreen(
            onYesClick = { viewModel.navigateTo(Screen.HOME) },
            //onYesClick = { viewModel.navigateTo(Screen.SIGNUPEMAIL) },
            onNoClick = { viewModel.navigateTo(Screen.LOGIN) },
        )
        Screen.LOGIN -> LoginScreen(
            onLoginClick = {viewModel.navigateTo(Screen.TEST)},
            onSignUpClick = {viewModel.navigateTo(Screen.SIGNUPEMAIL)}
        )
        Screen.SIGNUPEMAIL -> SignupEmailScreen(
            onContinueClick = {viewModel.navigateTo(Screen.SIGNUPPASSWORD)},
            onLoginClick = {viewModel.navigateTo(Screen.LOGIN)}
        )
        Screen.SIGNUPPASSWORD -> SignupPassWordScreen (
            onContinueClick = {viewModel.navigateTo(Screen.TEST)},
            onLoginClick = {viewModel.navigateTo(Screen.LOGIN)}
        )
        Screen.TEST -> TestScreen()
        Screen.HOME -> HomeScreen(
            onMenuClick = {},
            onRunClick = {viewModel.navigateTo(Screen.TEST)}
        )
    }
}