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
        Screen.START -> StartScreen (
            onHomeClick = { viewModel.navigateTo(Screen.HOME) },
        )

        Screen.TUTORIAL -> TutorialScreen(
            onYesClick = { viewModel.navigateTo(Screen.HOME) },
            onNoClick = { viewModel.navigateTo(Screen.HOME) },
        )
        Screen.LOGIN -> LoginScreen(
            onLoginClick = {viewModel.navigateTo(Screen.HOME)},
            onSignUpClick = {viewModel.navigateTo(Screen.SIGNUPEMAIL)}
        )
        Screen.SIGNUPEMAIL -> SignupEmailScreen(
            onContinueClick = {viewModel.navigateTo(Screen.SIGNUPPASSWORD)},
            onLoginClick = {viewModel.navigateTo(Screen.LOGIN)}
        )
        Screen.SIGNUPPASSWORD -> SignupPassWordScreen (
            onContinueClick = {viewModel.navigateTo(Screen.LOGIN)},
            onLoginClick = {viewModel.navigateTo(Screen.LOGIN)}
        )
        Screen.HOME -> HomeScreen(
            onMenuClick = {viewModel.navigateTo(Screen.MENU)},
            onRunClick = {viewModel.navigateTo(Screen.RUNNING)},
        )
        Screen.GOALSETTING -> GoalSettingScreen(
            onMenuClick = {viewModel.navigateTo(Screen.MENU)},
        )
        Screen.MENU -> MenuScreen (
            onBackClick = {viewModel.navigateTo(Screen.HOME)},
            onCorseClick = { },
            onGoalClick= {viewModel.navigateTo(Screen.GOALSETTING)},
            onOptionClick= { },
            onHelpClick= { },
            onCommunityClick= { viewModel.navigateTo(Screen.COMMUNITY) },
            onMypageClick= { },
        )
        Screen.RUNNING -> RunningScreen(
            onMenuClick = {viewModel.navigateTo(Screen.MENU)},
        )

        Screen.COMMUNITY -> CommunityScreen(
            onBackClick = { viewModel.navigateTo(Screen.MENU) },
            onPostClick = { postId ->
                // 이제 "1"이 아니라 실제 클릭한 postId를 들고 갑니다.
                viewModel.navigateToDetail(postId)
            },
            onUploadClick = {
                viewModel.navigateTo(Screen.POST_UPLOAD)
            }
        )

        Screen.POST_UPLOAD -> PostUploadScreen(
            onBackClick = { viewModel.navigateTo(Screen.COMMUNITY) },
            onUploadSuccess = {
                viewModel.navigateTo(Screen.COMMUNITY)
            }
        )

        Screen.COMMUNITY_DETAIL -> CommunityDetailScreen(
            // viewModel에 저장된 따끈따끈한 ID를 전달합니다.
            postId = viewModel.selectedPostId,
            onBackClick = { viewModel.navigateTo(Screen.COMMUNITY) }
        )

        Screen.TEST -> TestScreen()

    }
}