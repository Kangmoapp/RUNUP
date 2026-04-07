package com.example.runup.viewmodel


import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.example.runup.service.BleConnectionManager
import com.example.runup.service.BleSensorManager
import com.example.runup.service.LocationService
import com.example.runup.service.PostureAnalyzer
import com.example.runup.service.TtsManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    val showPaceDialog: Boolean = false,
    val isAiEnabled: Boolean = false,
    val currentPostureLabel: String = "AI 꺼짐",
    val leftBleState: String = "L: 대기 중",
    val rightBleState: String = "R: 대기 중"
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository,
    private val application: Application,

    private val postureAnalyzer: PostureAnalyzer,
    private val ttsManager: TtsManager,
    private val bleSensorManager: BleSensorManager,
    private val bleConnectionManager: BleConnectionManager
): ViewModel(){
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    // AI 추론용 버퍼
    private val inferenceBuffer = mutableListOf<FloatArray>()
    private var lastInferenceResult = ""

    init {
        loadUserGoal()
        observeCurrentLocation()
        startCurrentLocationTracking()

        // 센서 데이터 수집기 시작
        bleSensorManager.startDataProcessing()
        observeSensorData()
        observeBleConnection()
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

    // =====================================
    // AI 추론 및 음성 제어 로직
    // =====================================
    fun toggleAi() {
        _uiState.update { currentState ->
            val newState = !currentState.isAiEnabled
            if (!newState) {
                inferenceBuffer.clear()
                lastInferenceResult = ""
            }
            currentState.copy(
                isAiEnabled = newState,
                currentPostureLabel = if (newState) "분석 대기 중..." else "AI 꺼짐"
            )
        }
    }
    private fun observeSensorData() {
        viewModelScope.launch(Dispatchers.Default) {
            bleSensorManager.sensorDataFlow.collectLatest { snapshot ->
                // AI가 활성화 상태일 때만 데이터 수집
                if (!_uiState.value.isAiEnabled) return@collectLatest

                inferenceBuffer.add(snapshot)

                if (inferenceBuffer.size >= 50) {
                    val windowToProcess = inferenceBuffer.toList()
                    runInference(windowToProcess)
                    inferenceBuffer.clear()
                }
            }
        }
    }

    private fun runInference(window: List<FloatArray>) {
        val result = postureAnalyzer.analyze(window)
        if (result != null) {
            val (posture, probability) = result

            // UI 업데이트
            _uiState.update { it.copy(currentPostureLabel = posture) }

            // 자세가 바뀌었고, 확률이 70% 이상일 때 음성 알림
            if (posture != lastInferenceResult && probability > 0.7f) {
                ttsManager.speakOut("$posture 입니다")
                lastInferenceResult = posture
            }
        }
    }
    fun startBluetoothScan() {
        bleConnectionManager.startScan()
    }
    private fun observeBleConnection() {
        // 왼쪽 신발 상태 관찰
        viewModelScope.launch {
            bleConnectionManager.leftConnectionState.collectLatest { state ->
                _uiState.update { it.copy(leftBleState = "L: $state") }
            }
        }
        // 오른쪽 신발 상태 관찰
        viewModelScope.launch {
            bleConnectionManager.rightConnectionState.collectLatest { state ->
                _uiState.update { it.copy(rightBleState = "R: $state") }
            }
        }
    }
}