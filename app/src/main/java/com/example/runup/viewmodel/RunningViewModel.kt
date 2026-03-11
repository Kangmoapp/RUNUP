package com.example.runup.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val saveCourseUseCase: SaveCourseUseCase,
    private val application: Application
) : ViewModel() {

    // UI(Screen)에서 지도에 그릴 때 사용할 데이터
    val pathPoints = repository.recordedNodes
    val currentDistance = repository.totalDistance

    // [시작 버튼 클릭 시]
    fun startTracking() {
        val intent = Intent(application, LocationService::class.java)
        application.startForegroundService(intent)
    }

    // [종료 버튼 클릭 시]
    fun stopAndSave() {
        viewModelScope.launch {
            // 1. 서비스에 중단 신호 보내기
            val intent = Intent(application, LocationService::class.java).apply {
                action = "STOP_TRACKING"
            }
            application.startService(intent)

            // 2. 현재까지 쌓인 데이터 스냅샷 찍기
            val nodes = pathPoints.value
            val distance = currentDistance.value.toInt()

            if (nodes.isNotEmpty()) {
                val result = saveCourseUseCase(nodes, distance)

                if (result is AuthResult.Success) {
                    // 3. 저장 성공 시 리포지토리 초기화 (이때 화면의 숫자가 0이 됨)
                    repository.clearData()
                }
            }
        }
    }
}