package com.example.runup.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.UserLoginInfo
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalSettingUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0,
    val showDistanceDialog: Boolean = false
)

@HiltViewModel
class GoalSettingViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase
): ViewModel(){

    private val _uiState = MutableStateFlow(GoalSettingUiState())
    val uiState: StateFlow<GoalSettingUiState> = _uiState
    init {
        loadUserGoal()
    }

    private fun loadUserGoal() {
        viewModelScope.launch {
            when (val result = getUserGoalUseCase()) {

                is AuthResult.Success -> {
                    val (distance, pace) = result.data

                    _uiState.update {
                        it.copy(
                            goalDistance = distance,
                            goalPace = pace
                        )
                    }
                }

                is AuthResult.Fail -> {
                    // 필요하면 에러 처리
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