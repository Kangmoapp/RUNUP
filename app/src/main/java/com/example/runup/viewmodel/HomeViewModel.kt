package com.example.runup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject



data class HomeUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0,
    val currentLocation: GeoPoint? = null,   //현재 위치
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository,
): ViewModel(){
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observeLocation()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            locationRepository.currentLocation.collect { geoPoint ->
                _uiState.update { currentState ->
                    currentState.copy(
                        currentLocation = geoPoint
                    )
                }
            }
        }
    }
}