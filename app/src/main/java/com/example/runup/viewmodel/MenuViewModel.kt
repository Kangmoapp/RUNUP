package com.example.runup.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.GoogleAuthManager
import com.example.runup.ui.util.ImagePreloader
import com.example.runup.ui.util.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val updateUserLoginStatusUseCase: UpdateUserLoginStatusUseCase,
    private val userRepository: UserRepository,
    private val userStateManager: UserStateManager,
    private val imagePreloader: ImagePreloader, // 🔹 주입 추가
    ): ViewModel(){
    fun initPreload(context: Context) {
        viewModelScope.launch {
            // 🔹 [핵심] 이미 데이터가 로드되어 있다면 함수를 종료합니다.
            // 이렇게 하면 메뉴로 다시 돌아와도 네트워크 통신을 하지 않습니다.
            if (userStateManager.userData.value != null) {
                Log.d("Preload", "이미 데이터가 존재하여 로딩을 스킵합니다.")
                return@launch
            }
            // 1. 유저 데이터 먼저 가져와서 창고에 저장
            val result = userRepository.getMyUserData()
            if (result is AuthResult.Success) {
                val data = result.data
                userStateManager.updateUserData(data)

                // 🔹 내 프로필 프리로드 (전역 창고에 저장)
                if (data.userProfileUrl.isNotEmpty()) {
                    val bitmap = imagePreloader.loadBitmap(data.userProfileUrl, 200)
                    bitmap?.let {
                        userStateManager.updateProfileBitmap(data.userProfileUrl, it)
                    }
                }
            }
        }
    }

    fun signOutWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Google/Firebase 로그아웃
                googleAuthManager.signOut(context)

                // 2. [가장 중요] 메모리 창고(싱글톤) 초기화 🚨
                userStateManager.clear()

                // 3. 로컬 DB(Room)에 저장된 목표 데이터 삭제 🚨
                userRepository.deleteUserGoalFromRoom()

                // 4. 로그인 상태값 업데이트 (DataStore 등)
                updateUserLoginStatusUseCase(false)

                restartApp(context)
                Log.d("test", "로그아웃 성공 - 모든 메모리와 로컬 데이터가 청소되었습니다.")
            } catch (e: Exception) {
                Log.e("test", "로그아웃 실패: ${e.message}")
            }
        }
    }

    private fun restartApp(context: Context) {
        // 1. 앱의 런처 인텐트를 가져옵니다 (보통 MainActivity)
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component

        // 2. 모든 액티비티 스택을 날리고 새로 시작하는 인텐트 생성
        val mainIntent = Intent.makeRestartActivityTask(componentName)

        // 3. 앱 재시작 실행
        context.startActivity(mainIntent)

        // 4. 현재 실행 중인 프로세스를 완전히 종료 (이게 핵심! 🚨)
        // 0은 정상 종료를 의미합니다.
        Runtime.getRuntime().exit(0)
    }

}