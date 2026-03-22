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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val saveCourseUseCase: SaveCourseUseCase,
    private val application: Application
) : ViewModel() {

    private var recordingJob: Job? = null // 러닝 기록용 코루틴 잡

    // UI(Screen)에서 지도에 그릴 때 사용할 데이터
    val pathPoints = repository.recordedNodes
    val currentDistance = repository.totalDistance
    val userLocation = repository.currentLocation // 실시간 위치 (지도 표시용)

    // [1] 단순 위치 추적 시작 (서비스 실행)
    fun startCurrentLocationTracking() {
        val intent = Intent(application, LocationService::class.java)
        application.startForegroundService(intent)
    }

    // [2] 단순 위치 추적 종료 (서비스 종료)
    fun stopCurrentLocationTracking() {
        val intent = Intent(application, LocationService::class.java).apply {
            action = "STOP_TRACKING"
        }
        application.startService(intent)
    }

    // [3] 러닝 경로 기록 시작
    fun startRunningTracking() {
        // 이미 기록 중이면 중복 실행 방지
        if (recordingJob?.isActive == true) return

        recordingJob = viewModelScope.launch {
            while (true) {
                // 1초마다 레포지토리에 현재 좌표를 기록하라고 명령
                repository.addNodeFromCurrentLocation()
                delay(1000L)
            }
        }
    }

    // [4] 러닝 경로 기록 중단 및 저장
    fun stopRunningTracking() {
        recordingJob?.cancel() // 1초마다 기록하던 작업 중단

        viewModelScope.launch {
            val nodes = pathPoints.value
            val distance = currentDistance.value.toInt()

            if (nodes.isNotEmpty()) {
                val result = saveCourseUseCase(nodes, distance)
                if (result is AuthResult.Success) {
                    repository.clearData()
                }
            }
        }
    }
}