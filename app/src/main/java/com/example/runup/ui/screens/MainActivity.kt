package com.example.runup.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val isSplashLoading by viewModel.isSplashLoading.collectAsState()
    Box(modifier = Modifier.fillMaxSize()){
        when (currentScreen) {
            Screen.START -> StartScreen (
                onHomeClick = { viewModel.DelayToHome(Screen.HOME) },
            )
            Screen.HOME -> HomeScreen(
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
                onRunClick = {viewModel.navigateTo(Screen.RUNNING)},
            )
            Screen.GOALSETTING -> GoalSettingScreen(
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
                onBackClick = {viewModel.navigateTo(Screen.HOME)},
            )
            Screen.MENU -> MenuScreen (
                onBackClick = {viewModel.navigateTo(Screen.HOME)},
                onCorseClick = {viewModel.navigateTo(Screen.RECOMMEND)},
                onGoalClick= {viewModel.navigateTo(Screen.GOALSETTING)},
                onOptionClick= { },
                onHelpClick= { },
                onCommunityClick= { viewModel.navigateTo(Screen.COMMUNITY) },
                onMypageClick= {viewModel.navigateTo(Screen.MYPAGE)},
                onLocalDBClick = { viewModel.navigateTo(Screen.LOCALDB)},
                onLogoutClick = {viewModel.navigateTo(Screen.START)}
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
                onBackClick = {
                    viewModel.navigateTo(Screen.COMMUNITY)
                },
                onUploadSuccess = {
                    viewModel.navigateTo(Screen.COMMUNITY)
                }
            )

            Screen.COMMUNITY_COMMENT -> CommunityCommentScreen(
                // viewModel에 저장된 따끈따끈한 ID를 전달합니다.
                postId = viewModel.selectedPostId,
                onBackClick = { viewModel.navigateTo(Screen.COMMUNITY) }
            )
            Screen.LOADING -> LoadingScreen ()

            Screen.RECOMMEND -> CourseRecommendationScreen (
                onBackClick = {viewModel.navigateTo(Screen.HOME)},
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
            )
            Screen.TEST -> TestScreen()

            Screen.RUNNINGTEST -> RunningTestScreen()

            Screen.LOCALDB -> CourseDebugScreen({viewModel.navigateTo(Screen.MENU)})

            Screen.MYPAGE -> MyPageScreen(
                onBackClick = {viewModel.navigateTo(Screen.MENU)},
            )

        }
        if(isSplashLoading){
            LoadingScreen()
        }
    }
}