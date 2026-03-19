package com.example.runup.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.example.runup.ui.state.UserUiState
import com.google.type.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase
): ViewModel(){

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState
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

    fun confirmDistance(distanceKm: Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val distanceMeter:Int = distanceKm*100
        _uiState.update {
            it.copy(
                goalDistance = distanceMeter,
                showDistanceDialog = false
            )
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
}