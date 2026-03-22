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


    fun startTracking() {
        _isTracking.value = true
        val intent = Intent(application, LocationService::class.java)
        application.startForegroundService(intent)
    }

    fun stopAndSave() {
        viewModelScope.launch {
            val intent = Intent(application, LocationService::class.java).apply {
                action = "STOP_TRACKING"
            }
            application.startService(intent)

            val nodes = repository.recordedNodes.value
            val distance = repository.totalDistance.value.toInt()

            if (nodes.isNotEmpty()) {
                val result = saveCourseUseCase(nodes, distance)
                if (result is AuthResult.Success) {
                    repository.clearData()
                    _isTracking.value = false
                }
            }
        }
    }
}