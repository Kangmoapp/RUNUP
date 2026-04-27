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
import io.objectbox.Box
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
            )
            Screen.GOALSETTING -> GoalSettingScreen(
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
                onBackClick = {viewModel.popBackStack()},
            )
            Screen.MENU -> MenuScreen (
                onBackClick = {viewModel.popBackStack()},
                onCorseClick = {viewModel.navigateTo(Screen.RECOMMEND)},
                onGoalClick= {viewModel.navigateTo(Screen.GOALSETTING)},
                onOptionClick= { },
                onHelpClick= { },
                onCommunityClick= { viewModel.navigateTo(Screen.COMMUNITY) },
                onMypageClick= {viewModel.navigateTo(Screen.MYPAGE)},
                onLocalDBClick = { viewModel.navigateTo(Screen.LOCALDB)},
                onLogoutClick = {viewModel.navigateTo(Screen.START)}
            )
            /*
            Screen.RUNNING -> RunningScreen(
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
            )

             */

            Screen.COMMUNITY -> CommunityScreen(
                onBackClick = { viewModel.popBackStack() },
                onPostClick = { postId ->
                    // 이제 "1"이 아니라 실제 클릭한 postId를 들고 갑니다.
                    viewModel.navigateToDetail(postId)
                },
                onUploadClick = {
                    viewModel.navigateTo(Screen.POST_UPLOAD)
                },
                onPopupPostClick = { uid ->
                    viewModel.navigateToUserPosts(uid)
                }
            )

            Screen.POST_UPLOAD -> PostUploadScreen(
                onBackClick = {
                    viewModel.popBackStack()
                },
                onUploadSuccess = {
                    viewModel.popBackStack()
                }
            )

            Screen.LOADING -> LoadingScreen ()

            Screen.RECOMMEND -> CourseRecommendationScreen (
                onBackClick = {viewModel.popBackStack()},
                onMenuClick = {viewModel.navigateTo(Screen.MENU)},
            )
            Screen.TEST -> TestScreen()

            Screen.LOCALDB -> CourseDebugScreen({viewModel.navigateTo(Screen.MENU)})

            Screen.MYPAGE -> MyPageScreen(
                onBackClick = {viewModel.popBackStack()},
                onPostClick = { uid ->
                    viewModel.navigateToUserPosts(uid)
                }
            )

            Screen.USER_POSTS -> UserPostScreen(
                // AppViewModel에 저장된 UID를 전달
                targetUid = viewModel.selectedTargetUid,
                onBackClick = {
                    // 🔹 이전 화면이 마이페이지였을 수도, 커뮤니티였을 수도 있으므로
                    // 상황에 맞게 popBackStack 처럼 동작하게 하거나 특정 화면을 지정합니다.
                    viewModel.popBackStack()
                },
                onPostClick = { postId ->
                    // 상세 게시물로 연결 (기존 로직 재활용)
                    viewModel.navigateToDetail(postId)
                }
            )

            else -> {}
        }
        if(isSplashLoading){
            LoadingScreen()
        }
    }
}