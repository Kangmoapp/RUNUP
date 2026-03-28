package com.example.runup.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetRecommendedCourseUseCase
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
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
    private val getRecommendedCourseUseCase: GetRecommendedCourseUseCase
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
        val location = _uiState.value.currentLocation ?: run {
            return
        }
        // 여기서 uiState의 currentLocation, goalDistance, currentSort, isLoop 를 usecase에 넘기는 함수 작성
        viewModelScope.launch {
            val result = getRecommendedCourseUseCase.invoke(
                _uiState.value.goalDistance,
                GeoPoint(location.latitude, location.longitude), // 현재 위치로 하면 그 주변에 코스 없을 수도 있어서 -> GeoPoint(35.88948381055103,128.6095353131536) 이걸로 테스트 해보셈
                _uiState.value.isLoop,
                _uiState.value.currentSort,
                3
            )

            // 코스가 반환되어 오는지 테스트 로그
            when (result) {
                is AuthResult.Success -> {
                    // 1. 전체 개수 확인
                    Log.d("RUNUP_TEST", "총 추천 개수: ${result.data.size}")

                    // 2. 각 리스트 요소에 접근해서 주요 데이터만 확인
                    result.data.forEachIndexed { i, item ->
                        Log.d("RUNUP_TEST", "[$i] 코스: ${item.originCourse.id} | 사유: ${item.reason}")
                        Log.d("RUNUP_TEST", "    -> 거리: ${item.path.distance}m | 좌표수: ${item.path.points.size} | 중심: ${item.path.centerPoint}")

                        // 첫 번째 좌표가 LatLng으로 잘 바뀌었는지 한 점만 확인
                        item.path.points.firstOrNull()?.let {
                            Log.d("RUNUP_TEST", "    -> 시작점 체크: ${it.latitude}, ${it.longitude}")
                        }
                    }
                }
                is AuthResult.Fail -> {
                    Log.e("RUNUP_TEST", "에러 발생: ${result.message}")
                }
            }
        }
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