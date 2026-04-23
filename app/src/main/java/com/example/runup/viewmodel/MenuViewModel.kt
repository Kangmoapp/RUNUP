package com.example.runup.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.UserRepository
import com.example.runup.domain.usecase.UpdateUserLoginStatusUseCase
import com.example.runup.ui.util.GoogleAuthManager
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

                // 2. 데이터에 프로필 URL이 있다면 즉시 비트맵으로 변환해서 저장
                if (data.userProfileUrl.isNotEmpty() && userStateManager.profileBitmap.value == null) {
                    preloadProfileBitmap(context, data.userProfileUrl)
                }
            }
        }
    }

    private suspend fun preloadProfileBitmap(context: Context, url: String) {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Bitmap 변환을 위해 필수
            .build()

        val result = loader.execute(request)
        if (result is coil.request.SuccessResult) {
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            userStateManager.updateProfileBitmap(bitmap)
        }
    }

    fun signOutWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                googleAuthManager.signOut(context) // 이미 만들어진 signOut이 있다면 호출
                updateUserLoginStatusUseCase(false)
                onSuccess()
                Log.d("test", "로그아웃 성공 - 이제 다시 로그인해 보세요.")
            } catch (e: Exception) {
                Log.e("test", "로그아웃 실패: ${e.message}")
            }
        }
    }

}