package com.example.runup.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.runup.viewmodel.AppViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.repository.UserRepository
import com.example.runup.domain.usecase.GetUserLoginStatusUseCase
import com.example.runup.domain.usecase.RecordRunningUseCase
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.service.EmbeddingHelper
import com.example.runup.service.GeminiHelper
import com.example.runup.service.SyncManager
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
    val isMenuVisible by viewModel.isMenuVisible.collectAsState()
    Box(modifier = Modifier.fillMaxSize()){
        when (currentScreen) {
            Screen.START -> StartScreen (
                onHomeClick = { viewModel.DelayToHome(Screen.HOME) },
            )
            Screen.HOME -> HomeScreen(
                onMenuClick = {viewModel.openMenu()},
            )

            Screen.COMMUNITY -> CommunityScreen(
                onBackClick = { viewModel.navigateTo(Screen.HOME)},
                onUploadClick = {
                    viewModel.navigateTo(Screen.POST_UPLOAD)
                },
                onFollowClick = {viewModel.navigateTo(Screen.HOME)},
                onAuthorProfileClick = { uid ->
                    viewModel.navigateToUserPosts(uid)
                }
            )

            Screen.POST_UPLOAD -> PostUploadScreen(
                onBackClick = {
                    viewModel.navigateTo(Screen.COMMUNITY)
                },
                onBackHandlerClick = {
                    viewModel.popBackStack()
                },
                onUploadSuccess = {
                    viewModel.navigateTo(Screen.COMMUNITY)
                }
            )

            Screen.LOADING -> LoadingScreen ()

            Screen.TEST -> TestScreen()

            Screen.LOCALDB -> CourseDebugScreen({viewModel.navigateTo(Screen.HOME)})

            Screen.MYPAGE -> MyPageScreen(
                onBackClick = {viewModel.navigateTo(Screen.HOME)},
                onPostClick = { uid ->
                    viewModel.navigateToUserPosts(uid)
                },
                onUploadClick = {
                    viewModel.navigateTo(Screen.POST_UPLOAD)
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
                onFollowClick = {viewModel.navigateTo(Screen.HOME)},
                onNavigateToUser = { uid ->
                    viewModel.navigateToUserPosts(uid)
                }
            )

            Screen.SETTINGS -> SettingsScreen(
                onBackClick = { viewModel.popBackStack() }, // 🔹 이전 화면 히스토리를 유지하며 돌아갑니다.

                onLogoutClick = {
                    // 🌟 단순히 이동만 하는 게 아니라, 세션과 스택을 모두 비웁니다.
                    viewModel.logout()
                },

                onDeleteAccountClick = {
                    // 🌟 회원탈퇴 로직을 실행합니다.
                    viewModel.deleteAccount(
                        onSuccess = {
                        // ✅ 탈퇴 성공 시: 시작 화면으로 이동하고 스택 비우기
                        viewModel.logout()
                    },
                        onFail = { errorMsg ->
                            // ❌ 탈퇴 실패 시: 로그를 찍거나 사용자에게 알림 (필요 시 Toast 추가)
                            Log.e("DeleteAccount", "탈퇴 실패: $errorMsg")
                        })
                }
            )

            else -> {}
        }
        if(isSplashLoading){
            LoadingScreen()
        }
        if (isMenuVisible) {
            MenuScreen(
                onBackClick = { viewModel.closeMenu() },
                onOptionClick = { viewModel.navigateFromMenu(Screen.SETTINGS)},
                onHelpClick = { },
                onCommunityClick = { viewModel.navigateFromMenu(Screen.COMMUNITY) },
                onMypageClick = { viewModel.navigateFromMenu(Screen.MYPAGE) },
                onLocalDBClick = { viewModel.navigateFromMenu(Screen.LOCALDB) },
                onLogoutClick = {
                    viewModel.closeMenu()
                    viewModel.logout()
                }
            )
        }
    }
}