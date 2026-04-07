package com.example.runup.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetRecommendedCourseUseCase
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseRecommendationUiState(
    val goalDistance: Int = 0,
    val cameraLocation: LatLng? = null,
    val currentLocation: LatLng? = null,
    val currentSort: SortType = SortType.DISTANCE,
    val isLoop: Boolean = true,
    val showLoop: Boolean = false,
    val showDistanceDialog: Boolean = false,
    val showSortDialog: Boolean = false,
    val isRecommendClick: Boolean = false,
    val recommendedCourses: List<CourseRecommendation> = emptyList(),
    val courseIndex: Int = 0,
    val isLoading:Boolean = false
)

@HiltViewModel
class CourseRecommendationViewModel @Inject constructor(
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository,
    private val getRecommendedCourseUseCase: GetRecommendedCourseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseRecommendationUiState())
    val uiState: StateFlow<CourseRecommendationUiState> = _uiState

    init {
        loadUserGoal()
        observeCurrentLocation()
        startCurrentLocationTracking()
    }

    private fun resolveCameraLocation(state: CourseRecommendationUiState): LatLng? {
        return if (state.isRecommendClick) {
            state.recommendedCourses
                .getOrNull(state.courseIndex)
                ?.path
                ?.centerPoint
                ?.let {
                    LatLng(it.latitude, it.longitude)
                }
        } else {
            state.currentLocation
        }
    }

    private fun updateState(
        transform: (CourseRecommendationUiState) -> CourseRecommendationUiState
    ) {
        _uiState.update { state ->
            val newState = transform(state)
            newState.copy(
                cameraLocation = resolveCameraLocation(newState)
            )
        }
    }

    private fun loadUserGoal() {
        viewModelScope.launch {
            getUserGoalUseCase().collectLatest { goal ->
                goal?.let { (distance) ->
                    updateState {
                        it.copy(goalDistance = distance)
                    }
                }
            }
        }
    }

    private fun observeCurrentLocation() {
        viewModelScope.launch {
            locationRepository.currentLocation.collectLatest { location ->
                val latLng = location?.let {
                    LatLng(it.latitude, it.longitude)
                }

                updateState {
                    it.copy(
                        currentLocation = latLng
                    )
                }
            }
        }
    }

    fun startCurrentLocationTracking() {
        locationRepository.startTracking()
    }

    fun stopCurrentLocationTracking() {
        locationRepository.stopTracking()
    }

    fun resetUiState() {
        _uiState.value = CourseRecommendationUiState()
    }

    fun onSearchClick() {
        if(_uiState.value.isRecommendClick){

        }
        else{
            updateState {
                it.copy(
                    isLoading = true
                )
            }

            val location = _uiState.value.currentLocation ?: run {
                Log.e("RUNUP_TEST", "현재 위치가 없습니다.")
                return
            }
            viewModelScope.launch {
                val result = getRecommendedCourseUseCase.invoke(
                    _uiState.value.goalDistance,
                    // GeoPoint(location.latitude, location.longitude),
                    GeoPoint(35.88948381055103, 128.6095353131536),
                    _uiState.value.isLoop,
                    _uiState.value.currentSort,
                    3
                )

                when (result) {
                    is AuthResult.Success -> {
                        Log.d("RUNUP_TEST", "총 추천 개수: ${result.data.size}")

                        updateState {
                            it.copy(
                                recommendedCourses = result.data,
                                courseIndex = 0
                            )
                        }

                        Log.d("RUNUP_TEST", "추천 코스 목록: ${_uiState.value.recommendedCourses}")

                        result.data.forEachIndexed { i, item ->
                            Log.d("RUNUP_TEST", "[$i] 코스: ${item.originCourse.id} | 사유: ${item.reason}")
                            Log.d(
                                "RUNUP_TEST",
                                "    -> 거리: ${item.path.distance}m | 좌표수: ${item.path.points.size} | 중심: ${item.path.centerPoint}"
                            )

                            item.path.points.firstOrNull()?.let {
                                Log.d("RUNUP_TEST", "    -> 시작점 체크: ${it.latitude}, ${it.longitude}")
                            }
                        }

                        delay(1500)
                        updateState {
                            it.copy(
                                isRecommendClick = true,
                                isLoading = false
                            )
                        }
                    }

                    is AuthResult.Fail -> {
                        Log.e("RUNUP_TEST", "에러 발생: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 추천 모드 해제하고 다시 현재 위치 기준으로 카메라 이동
     */
    fun clearRecommendation() {
        updateState {
            it.copy(
                isRecommendClick = false,
                recommendedCourses = emptyList(),
                courseIndex = 0
            )
        }
    }

    fun openLoopDialog() {
        updateState { it.copy(showLoop = true) }
    }

    fun closeLoopDialog() {
        updateState { it.copy(showLoop = false) }
    }

    fun loopSelect(isFirst: Boolean = true) {
        if (isFirst) {
            updateState { it.copy(showLoop = false) }
        } else {
            updateState {
                it.copy(
                    isLoop = !it.isLoop,
                    showLoop = false
                )
            }
        }
    }

    fun openDistanceDialog() {
        updateState { it.copy(showDistanceDialog = true) }
    }

    fun closeDistanceDialog() {
        updateState { it.copy(showDistanceDialog = false) }
    }

    fun openSortDialog() {
        updateState { it.copy(showSortDialog = true) }
    }

    fun closeSortDialog() {
        updateState { it.copy(showSortDialog = false) }
    }

    fun confirmDistance(distanceKm: Int) {
        val distanceMeter = distanceKm * 100

        updateState {
            it.copy(
                showDistanceDialog = false,
                goalDistance = distanceMeter
            )
        }
    }

    fun confirmSort(sortType: SortType) {
        updateState {
            it.copy(
                showSortDialog = false,
                currentSort = sortType
            )
        }
    }

    fun addIndex(){
        if(_uiState.value.courseIndex < (_uiState.value.recommendedCourses.size-1))
            updateState{it.copy(courseIndex = _uiState.value.courseIndex+1)}
        else
            updateState{it.copy(courseIndex = 0)}
    }

    fun subtractIndex(){
        if(_uiState.value.courseIndex >0)
            updateState{it.copy(courseIndex = _uiState.value.courseIndex-1)}
        else
            updateState{it.copy(courseIndex = _uiState.value.recommendedCourses.size-1)}
    }
}