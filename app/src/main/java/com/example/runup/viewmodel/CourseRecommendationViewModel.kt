package com.example.runup.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseRecommendationUiState(
    val goalDistance: Int = 0,
    val currentLocation: LatLng? = null,   //현재 위치
    val currentSort: SortType = SortType.DISTANCE,
    val isLoop: Boolean = true,
    val showLoop: Boolean = false,
    val showDistanceDialog: Boolean = false,
    val showSortDialog: Boolean = false
)
@HiltViewModel
class CourseRecommendationViewModel @Inject constructor(
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository,
): ViewModel(){
    private val _uiState = MutableStateFlow(CourseRecommendationUiState())
    val uiState: StateFlow<CourseRecommendationUiState> = _uiState

    init {
        loadUserGoal()
        observeCurrentLocation()
        startCurrentLocationTracking()
    }

    private fun loadUserGoal() {
        viewModelScope.launch {
            getUserGoalUseCase().collectLatest { goal ->
                goal?.let { (distance) ->
                    _uiState.update {
                        it.copy(
                            goalDistance = distance
                        )
                    }
                }
            }
        }
    }
    private fun observeCurrentLocation() {
        viewModelScope.launch {
            locationRepository.currentLocation.collectLatest { location ->
                _uiState.update {
                    it.copy(
                        currentLocation = location?.let {
                            LatLng(it.latitude, it.longitude)
                        }
                    )
                }
            }
        }
    }
    fun resetUiState() {
        _uiState.value = CourseRecommendationUiState()
    }

    fun onSearchClick(){
        // 여기서 uiState의 goalDistance, currentSort, isLoop 를 usecase에 넘기는 함수 작성
    }

    fun openLoopDialog(){
        _uiState.update { it.copy(showLoop = true) }
    }
    fun closeLoopDialog() {
        _uiState.update { it.copy(showLoop = false) }
    }


    fun loopSelect(isFirst: Boolean = true){
        if(isFirst) _uiState.update { it.copy(showLoop = false) }
        else _uiState.update { it.copy(isLoop = !_uiState.value.isLoop, showLoop = false) }
    }

    // [1] 단순 위치 추적 시작 (GPS 서비스 ON)
    fun startCurrentLocationTracking() {
        locationRepository.startTracking()
    }

    fun stopCurrentLocationTracking() {
        locationRepository.stopTracking()
    }

    // 목표 설정 함수들
    fun openDistanceDialog() {
        _uiState.update { it.copy(showDistanceDialog = true) }
    }
    fun closeDistanceDialog() {
        _uiState.update { it.copy(showDistanceDialog = false) }
    }

    fun openSortDialog() {
        _uiState.update { it.copy(showSortDialog = true) }
    }
    fun closeSortDialog() {
        _uiState.update { it.copy(showSortDialog = false) }
    }


    fun confirmDistance(distanceKm: Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val distanceMeter:Int = distanceKm*100
        _uiState.update {
            it.copy(showDistanceDialog = false, goalDistance = distanceMeter)
        }
    }

    fun confirmSort(sortType: SortType) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        _uiState.update {
            it.copy(showSortDialog = false, currentSort = sortType)
        }
    }
}