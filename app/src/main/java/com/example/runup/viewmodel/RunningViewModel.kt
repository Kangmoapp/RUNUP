package com.example.runup.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.RecordRunningUseCase
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.service.LocationService
import com.example.runup.ui.navigation.Screen
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.timeout
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
    private val recordRunningUseCase: RecordRunningUseCase,
    private val application: Application
) : ViewModel() {


    private val _isTracking = MutableStateFlow(false)
    private var recordingJob: Job? = null // 러닝 기록용 코루틴 잡

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    private val _isStart = MutableStateFlow(true)
    val isStart: StateFlow<Boolean> = _isStart

    private val _loadTimer = MutableStateFlow(0)
    val loadTimer: StateFlow<Int> = _loadTimer

    // [추가] 실시간 시간 기록을 위한 StateFlow (초 단위)
    private val _totalTime = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            startScreen()
        }
        startCurrentLocationTracking()
        updateUiState()
    }
    private suspend fun startScreen(){
        _isStart.value = true
        for (i in 3 downTo 1) {
            _loadTimer.value = i
            delay(1000)
        }
        _isStart.value = false
    }

    private fun updateUiState(){
        viewModelScope.launch {
            combine(
                repository.recordedNodes,
                repository.totalDistance,
                repository.currentLocation, // 실시간 위치
                _isTracking,
                _totalTime,
            ) { nodes, totalDistance, currentGeo, isTracking, totalTime ->
                RunningUiState(
                    // 실시간 내 위치 (기록 중이 아니어도 표시됨)
                    currentLocation = currentGeo?.let { LatLng(it.latitude, it.longitude) },
                    // 지금까지 이동한 경로
                    latLngList = nodes.map { LatLng(it.locationPoint.latitude, it.locationPoint.longitude) },
                    totalDistance = totalDistance,
                    isTracking = isTracking,
                    totalTime = totalTime
                )
            }.collect { newState ->
                Log.d("RunningPosition", "UI State Updated: ${newState.currentLocation}")
                _uiState.value = newState
            }
        }
    }


    // [1] 단순 위치 추적 시작 (GPS 서비스 ON)
    fun startCurrentLocationTracking() {
        repository.startTracking()
    }

    // [2] 단순 위치 추적 종료 (GPS 서비스 OFF)
    fun stopCurrentLocationTracking() {
        // 러닝 기록 중이었다면 그것부터 멈춤
        if (_isTracking.value) stopRunningTracking()

        repository.stopTracking()
    }

    // [3] 러닝 경로 기록 시작
    fun startRunningTracking() {
        if (_isTracking.value) return // 이미 기록 중이면 무시

        _isTracking.value = true
        _totalTime.value = 0

        recordingJob = viewModelScope.launch {
            while (true) {
                // 1초마다 레포지토리의 현재 위치를 노드로 변환하여 저장
                repository.addNodeFromCurrentLocation()
                // 2. 시간 1초 증가 (초 단위)
                _totalTime.value += 1
                delay(1000L)
            }
        }
    }

    // [4] 러닝 경로 기록 중단 및 저장
    fun stopRunningTracking() {
        _isTracking.value = false
        recordingJob?.cancel()

        viewModelScope.launch {
            val nodes = repository.recordedNodes.value
            val distance = repository.totalDistance.value.toInt()
            val timeInMillis = _totalTime.value * 1000

            if (nodes.isNotEmpty()) {
                val result = recordRunningUseCase(nodes, distance, timeInMillis)
                if (result is AuthResult.Success) {
                    // 저장 성공 후 경로 데이터만 초기화
                    repository.clearData()
                    _totalTime.value = 0 // 저장 성공 후 시간 초기화
                }
            }
        }
    }
}