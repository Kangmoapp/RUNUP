package com.example.runup.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.google.type.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0,
    val currentLocation: LatLng? = null,   //현재 위치
    val showDistanceDialog: Boolean = false,
    val showPaceDialog: Boolean = false
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository
): ViewModel(){
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    init {
        loadUserGoal()
    }

    private fun loadUserGoal() {
        viewModelScope.launch {
            getUserGoalUseCase().collectLatest { goal ->
                goal?.let { (distance, pace) ->
                    _uiState.update {
                        it.copy(
                            goalDistance = distance,
                            goalPace = pace
                        )
                    }
                }
            }
        }
    }


    fun openDistanceDialog() {
        _uiState.update { it.copy(showDistanceDialog = true) }
    }
    fun closeDistanceDialog() {
        _uiState.update { it.copy(showDistanceDialog = false) }
    }

    fun openPaceDialog() {
        _uiState.update { it.copy(showPaceDialog = true) }
    }
    fun closePaceDialog() {
        _uiState.update { it.copy(showPaceDialog = false) }
    }

    fun confirmDistance(distanceKm: Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val distanceMeter:Int = distanceKm*100
        _uiState.update {
            it.copy(showDistanceDialog = false)
        }
        viewModelScope.launch {
            when (val result = goalsettingUseCase(distanceMeter, _uiState.value.goalPace)) {
                is AuthResult.Success -> {

                }
                is AuthResult.Fail -> {

                }

            }
        }
    }

    fun confirmPace(paceMinute: Int, paceSecond:Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val paceTotal:Int = paceMinute*60 + paceSecond
        _uiState.update {
            it.copy(showPaceDialog = false)
        }
        viewModelScope.launch {
            when (val result = goalsettingUseCase(_uiState.value.goalDistance, paceTotal)) {
                is AuthResult.Success -> {

                }
                is AuthResult.Fail -> {

                }

            }
        }
    }
}