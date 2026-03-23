package com.example.runup.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.service.LocationService
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunningUiState(
    val currentLocation: LatLng? = null,
    val latLngList: List<LatLng> = emptyList(),   //지금까지 이동 경로 좌표 목록
    val totalTime:Int = 0,
    val totalDistance: Double = 0.0, //현재까지 달린 거리
    val isTracking: Boolean = false //현재 달리는 중인지 running -> true, stop -> false
)

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val saveCourseUseCase: SaveCourseUseCase,
    private val application: Application
) : ViewModel() {
    private val _isTracking = MutableStateFlow(false)
    private var recordingJob: Job? = null // 러닝 기록용 코루틴 잡
    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    init {
        startTracking()
        updateUiState()
    }

    private fun updateUiState(){
        viewModelScope.launch {
            combine(
                repository.recordedNodes,
                repository.totalDistance,
                _isTracking
            ) { nodes, totalDistance, isTracking ->
                val lastLocation = nodes.lastOrNull()?.let {
                    LatLng(it.locationPoint.latitude, it.locationPoint.longitude)
                }
                RunningUiState(
                    currentLocation = lastLocation,
                    latLngList = nodes.map {
                        LatLng(
                            it.locationPoint.latitude,
                            it.locationPoint.longitude
                        )
                    },
                    totalDistance = totalDistance,
                    isTracking = isTracking
                )
            }.collect { newState ->
                Log.d("RunningPosition", "currentLocation = ${newState.currentLocation}")
                _uiState.value = newState
            }
        }
    }


    // [1] 단순 위치 추적 시작 (서비스 실행)
    fun startCurrentLocationTracking() {

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