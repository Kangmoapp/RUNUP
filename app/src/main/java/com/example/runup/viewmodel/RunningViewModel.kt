package com.example.runup.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.service.LocationService
import com.example.runup.ui.state.UserUiState
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunningViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val saveCourseUseCase: SaveCourseUseCase,
    private val application: Application
) : ViewModel() {

    private val _hasLocationPermission = MutableStateFlow(false)
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    private val _isTracking = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.recordedNodes,
                repository.totalDistance,
                _hasLocationPermission,
                _currentLocation,
                _isTracking
            ) { nodes, totalDistance, hasLocationPermission, currentLocation, isTracking ->
                UserUiState(
                    latLngList = nodes.map {
                        LatLng(
                            it.locationPoint.latitude,
                            it.locationPoint.longitude
                        )
                    },
                    currentLocation = currentLocation,
                    totalDistance = totalDistance,
                    hasLocationPermission = hasLocationPermission,
                    isTracking = isTracking
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateLocationPermission(granted: Boolean) {
        _hasLocationPermission.value = granted
    }

    fun updateCurrentLocation(latLng: LatLng?) {
        _currentLocation.value = latLng
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
                    _currentLocation.value = null
                    _isTracking.value = false
                }
            }
        }
    }
}