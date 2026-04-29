package com.example.runup.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.BuildConfig
import com.example.runup.data.local.UserPreferenceDataSource
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.Scores
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetRecommendedCourseUseCase
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.example.runup.domain.usecase.RecordRunningUseCase
import com.example.runup.domain.usecase.SaveCourseUseCase
import com.example.runup.service.BleConnectionManager
import com.example.runup.service.BleSensorManager
import com.example.runup.service.LocationService
import com.example.runup.service.NaverMapApiService
import com.example.runup.service.PostureAnalyzer
import com.example.runup.service.TMapApiService
import com.example.runup.service.TMapRouteRequest
import com.example.runup.service.TtsManager
import com.example.runup.ui.navigation.HomeUi
import com.naver.maps.geometry.LatLng
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0,
    val currentLocation: GeoPoint? = null,   //현재 위치
    val currentBearing: Float = 0.0f, // 방향 추가
    val isScroll:Boolean = true,
    val isLoading:Boolean = false,
    val showDistanceDialog: Boolean = false,
    val showPaceDialog: Boolean = false,
    val homeUi: HomeUi = HomeUi.HOME,
    val selectedTab: HomeTab = HomeTab.RUNNING,
    val isInitialLoading: Boolean = true,

    val isAiEnabled: Boolean = false,
    val currentPostureLabel: String = "AI 꺼짐",
    val leftBleState: String = "L: 대기 중",
    val rightBleState: String = "R: 대기 중",
)

data class GuideUiState(
    val guidePath: List<LatLng> = emptyList(), // 🔹 추가: 안내 경로 좌표 리스트
    val destinationMarker: LatLng? = null,    // 🔹 추가: 목적지 마커 위치
    val isGuiding: Boolean = false,            // 🔹 추가: 현재 경로 안내 중인지 여부
    val guideDistance: Int = 0, // 🔹 추가: 경로 거리 (미터)
    val guideDuration: Long = 0L, // 🔹 추가: 예상 소요 시간 (밀리초)
    val isTrackingMode: Boolean = false //🔹  추가: 현재 트래킹 모드인지 아닌지
)

data class RunningUiState(
    val latLngList: List<LatLng> = emptyList(),   //지금까지 이동 경로 좌표 목록
    val totalTime:Int = 0,
    val totalDistance: Double = 0.0, //현재까지 달린 거리
    val isTracking: Boolean = false //현재 달리는 중인지 running -> true, stop -> false
)

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


enum class HomeTab { RUNNING, RECOMMEND, COURSE }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val saveCourseUseCase: SaveCourseUseCase,
    private val recordRunningUseCase: RecordRunningUseCase,

    private val goalsettingUseCase: GoalSettingUseCase,
    private val getUserGoalUseCase: GetUserGoalUseCase,
    private val locationRepository: LocationRepository,

    private val postureAnalyzer: PostureAnalyzer,
    private val ttsManager: TtsManager,
    private val bleSensorManager: BleSensorManager,
    private val bleConnectionManager: BleConnectionManager,
    private val naverMapApiService: NaverMapApiService,
    private val tMapApiService: TMapApiService,
    private val userPreferenceDataSource: UserPreferenceDataSource,
    private val getRecommendedCourseUseCase: GetRecommendedCourseUseCase,

    @ApplicationContext private val context: Context,
): ViewModel(){
    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState

    private val _recommendUiState = MutableStateFlow(CourseRecommendationUiState())
    val recommendUiState: StateFlow<CourseRecommendationUiState> = _recommendUiState

    private val _isTracking = MutableStateFlow(false)

    private val _GuideUiState = MutableStateFlow(GuideUiState())
    val GuideUiState: StateFlow<GuideUiState> = _GuideUiState

    private val _loadingTimer = MutableStateFlow(0)
    val loadingTimer: StateFlow<Int> = _loadingTimer

    private val inferenceBuffer = mutableListOf<FloatArray>()
    private var lastInferenceResult = ""

    private var recordingJob: Job? = null // 러닝 기록용 코루틴 잡

    // 리포지토리의 주소 상태
    val addressUiState: StateFlow<AddressModel?> = locationRepository.addressState

    // lifecycle 로 값이 업데이트 될 때마다 자동 업데이트 + 하나의 생명 주기만 적용
    val runningUiState: StateFlow<RunningUiState> = combine(
        locationRepository.recordedNodes,
        locationRepository.totalDistance,
        _isTracking,
        locationRepository.totalTime
    ) { nodes, totalDistance, isTracking, totalTime ->
        RunningUiState(
            latLngList = nodes.map {
                LatLng(it.locationPoint.latitude, it.locationPoint.longitude)
            },
            totalDistance = totalDistance,
            isTracking = isTracking,
            totalTime = totalTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RunningUiState()
    )

    init {
        observeLocation()
        loadUserGoal()

        startCurrentLocationTracking()

        bleSensorManager.startDataProcessing()
        observeSensorData()
        observeBleConnection()
    }

    private fun observeLocation() {
        viewModelScope.launch {
            launch {
                locationRepository.currentLocation.collect { geoPoint ->
                    _homeUiState.update { it.copy(currentLocation = geoPoint) }

                    // 🔹 초기 로딩 중이고, 첫 좌표(geoPoint)가 null이 아니면 로딩 해제!
                    if (_homeUiState.value.isInitialLoading && geoPoint != null) {
                        _homeUiState.update { it.copy(isInitialLoading = false) }
                    }

                    // 위치가 업데이트될 때마다 100m 이동했는지 체크하여 주소 갱신
                    geoPoint?.let {
                        locationRepository.refreshAddressIfNeeded(it.latitude, it.longitude)
                    }
                }
            }

            launch {
                locationRepository.currentBearing.collect { bearing ->
                    _homeUiState.update { it.copy(currentBearing = bearing) }
                }
            }
        }
    }
    private fun loadUserGoal() {
        viewModelScope.launch {
            getUserGoalUseCase().collectLatest { goal ->
                goal?.let { (distance, pace) ->
                    _homeUiState.update {
                        it.copy(
                            goalDistance = distance,
                            goalPace = pace
                        )
                    }
                }
            }
        }
    }


    // 목표 설정 함수들
    fun openDistanceDialog() {
        _homeUiState.update { it.copy(showDistanceDialog = true) }
    }
    fun closeDistanceDialog() {
        _homeUiState.update { it.copy(showDistanceDialog = false) }
    }

    fun openPaceDialog() {
        _homeUiState.update { it.copy(showPaceDialog = true) }
    }
    fun closePaceDialog() {
        _homeUiState.update { it.copy(showPaceDialog = false) }
    }

    fun confirmDistance(distanceKm: Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val distanceMeter:Int = distanceKm*100
        _homeUiState.update {
            it.copy(showDistanceDialog = false)
        }
        viewModelScope.launch {
            when (val result = goalsettingUseCase(distanceMeter, _homeUiState.value.goalPace)) {
                is AuthResult.Success -> {

                }
                is AuthResult.Fail -> {

                }

            }
        }
    }

    fun confirmPace(paceMinute: Int, paceSecond:Int) {  //이 함수에서 db에 목표거리 저장 (distanceMeter)
        val paceTotal:Int = paceMinute*60 + paceSecond
        _homeUiState.update {
            it.copy(showPaceDialog = false)
        }
        viewModelScope.launch {
            when (val result = goalsettingUseCase(_homeUiState.value.goalDistance, paceTotal)) {
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
        _homeUiState.update { currentState ->
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
                if (!_homeUiState.value.isAiEnabled) return@collectLatest

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
            _homeUiState.update { it.copy(currentPostureLabel = posture) }

            // 자세가 바뀌었고, 확률이 70% 이상일 때 음성 알림
            if (posture != lastInferenceResult && probability > 0.7f) {
                val message = when(posture) {
                    // --- [러닝 중 경고 멘트] ---
                    "과도한 뒤꿈치 착지" -> "뒤꿈치 충격이 큽니다. 발바닥 전체로 착지해 보세요."
                    "오버스트라이드" -> "보폭이 너무 깁니다. 몸의 중심 아래로 발을 딛어보세요."
                    "케이던스 부족" -> "발구름이 느립니다. 보폭을 조금 줄이고, 리듬을 더 빠르게 가져가 보세요."
                    "전족부 착지" -> "발 앞꿈치로만 착지하고 있습니다. 종아리에 무리가 갈 수 있으니 발바닥 전체를 사용해 보세요."
                    "지친 상태의 러닝" -> "자세가 흐트러지고 있습니다. 어깨에 힘을 빼고 시선을 멀리 보세요."

                    // --- [보행 및 일상 경고 멘트] ---
                    "왼쪽 짝다리" -> "왼쪽 발에 체중이 쏠려 있습니다. 양발에 체중을 고르게 분산시켜 보세요."
                    "오른쪽 짝다리" -> "오른쪽 발에 체중이 쏠려 있습니다. 골반의 균형을 맞춰보세요."
                    "팔자 걸음" -> "팔자걸음이 감지되었습니다. 발끝이 정면을 향하도록 11자로 걸어보세요."
                    "내회전" -> "발목이 안쪽으로 무너지고 있습니다. 발의 아치를 세운다는 느낌으로 걸어보세요."
                    "외회전" -> "발목이 바깥쪽으로 꺾여 있습니다. 발바닥 안쪽에도 힘을 실어보세요."

                    // --- [정상 상태 (필요시 사용, 보통은 쿨타임 로직에서 미리 걸러짐)] ---
                    "정지" -> "올바른 정지 자세입니다."
                    "걷기" -> "올바른 보행 자세입니다."
                    "뛰기" -> "좋은 자세를 유지하고 있습니다. 페이스를 유지하세요."

                    // 매칭되는 라벨이 없을 경우 기본값
                    else -> posture
                }
                ttsManager.speakOut(message)
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
                _homeUiState.update { it.copy(leftBleState = "L: $state") }
            }
        }
        // 오른쪽 신발 상태 관찰
        viewModelScope.launch {
            bleConnectionManager.rightConnectionState.collectLatest { state ->
                _homeUiState.update { it.copy(rightBleState = "R: $state") }
            }
        }
        _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
    }


    // =====================================
    // 러닝 시 로직
    // =====================================

    fun onRunClick() {
        viewModelScope.launch {
            _homeUiState.update { it.copy(isLoading = true, homeUi = HomeUi.RUN) }
            for (i in 3 downTo 1) {
                _loadingTimer.value = i
                delay(1000)
            }
            _homeUiState.update { it.copy(isLoading = false) }

            // 2. 추적 및 타이머 시작 (중복 호출 제거) 🔹
            locationRepository.startTracking()
            locationRepository.startTimer()
            startRunningTracking()
        }

    // 러닝 최종 저장 혹은 취소 시 서비스 종료 📍
    private fun stopForegroundService() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = "STOP_TRACKING" // 📍 LocationService에 정의한 액션과 맞춰야 함
        }
        context.startService(intent) // 또는 stopService(intent)
    }

    fun startCurrentLocationTracking() {
        locationRepository.startTracking()
    }

    // [2] 단순 위치 추적 종료 (GPS 서비스 OFF)
    fun stopCurrentLocationTracking() {
        // 러닝 기록 중이었다면 그것부터 멈춤
        if (_isTracking.value) stopRunningTracking()
        locationRepository.stopTracking()
    }

    // [3] 러닝 경로 기록 시작
    fun startRunningTracking() {
        if (_isTracking.value) return // 이미 기록 중이면 무시

        _isTracking.value = true

        recordingJob = viewModelScope.launch {
            while (true) {
                // 1초마다 레포지토리의 현재 위치를 노드로 변환하여 저장
                if(_isTracking.value){
                    locationRepository.addNodeFromCurrentLocation()
                    delay(1000L)
                } else {
                    delay(500L) // false일 때도 잠깐 쉬기
                }
                Log.d("RunningTracking", "TrackingState: ${_isTracking.value}")
            }
        }
    }

    // [4] 러닝 경로 기록 중단
    fun stopRunningTracking() {
        if(_isTracking.value){
            locationRepository.markLastNodeAsStopped()
            locationRepository.stopTimer()
            _isTracking.value = false
        }
        else _isTracking.value = true
        //recordingJob?.cancel()
        /*
        viewModelScope.launch {
            val nodes = locationRepository.recordedNodes.value
            val distance = locationRepository.totalDistance.value.toInt()
            val timeInMillis = _totalTime.value * 1000

            if (nodes.isNotEmpty()) {
                val result = recordRunningUseCase(nodes, distance, timeInMillis)
                if (result is AuthResult.Success) {
                    // 저장 성공 후 경로 데이터만 초기화
                    locationRepository.clearData()
                    _totalTime.value = 0 // 저장 성공 후 시간 초기화
                }
            }
        }
         */
    }

    fun recordRunningCourse(scores: Scores) {
        //_isTracking.value = false
        recordingJob?.cancel()
        viewModelScope.launch {
            val nodes = locationRepository.recordedNodes.value
            val distance = locationRepository.totalDistance.value.toInt()
            val timeInMillis = locationRepository.totalTime.value * 1000L

            if (nodes.isNotEmpty()) {
                val result = recordRunningUseCase(nodes, distance, timeInMillis.toInt(), scores)
                if (result is AuthResult.Success) {
                    // 저장 성공 후 경로 데이터만 초기화
                    locationRepository.clearData()
                }
            }
        }
        stopForegroundService()
        _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
    }

    fun cancelRunningCourse() {
        // 1. 위치 기록 Job 중단
        recordingJob?.cancel()

        // 2. 저장 없이 데이터만 초기화
        locationRepository.clearData()

        // 3. UI 상태를 러닝 종료로 변경
        stopForegroundService()
        _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
    }

    fun selectTab(tab: HomeTab) {
        _homeUiState.update { it.copy(selectedTab = tab) }
    }

    // HomeViewModel.kt 내부
    fun startNavigation(destination: LatLng) {
        val startLoc = _homeUiState.value.currentLocation ?: return

        viewModelScope.launch {
            try {
                val request = TMapRouteRequest(
                    startX = startLoc.longitude,
                    startY = startLoc.latitude,
                    endX = destination.longitude,
                    endY = destination.latitude
                )
                Log.d("Tmap", "호출 시작...")
                val response = tMapApiService.getPedestrianRoute(
                    appKey = BuildConfig.TMAP_API_KEY,
                    requestBody = request
                )
                Log.d("Tmap", "응답 성공: ${response.features.size}")
                val pathList = mutableListOf<LatLng>()
                var totalDistance = 0
                var totalTime = 0L

                response.features.forEach { feature ->
                    // 1. 거리/시간 정보는 첫 번째 feature의 properties에 들어있음
                    if (feature.properties.totalDistance > 0) {
                        totalDistance = feature.properties.totalDistance
                        totalTime = feature.properties.totalTime.toLong() * 1000 // ms 변환
                    }

                    // 2. 경로 좌표 추출 (LineString 타입만)
                    if (feature.geometry.type == "LineString") {
                        val coords = feature.geometry.coordinates as List<List<Double>>
                        coords.forEach {
                            pathList.add(LatLng(it[1], it[0])) // [lng, lat] -> LatLng(lat, lng)
                        }
                    }
                }

                _GuideUiState.update {
                    it.copy(
                        guidePath = pathList,
                        guideDistance = totalDistance,
                        guideDuration = totalTime,
                        isGuiding = true,
                        destinationMarker = destination
                    )
                }
            } catch (e: Exception) {
                Log.e("Tmap", "도보 경로 에러: ${e.message}")
            }
        }
    }

    fun toggleTrackingMode() {
        _GuideUiState.update { it.copy(isTrackingMode = !it.isTrackingMode) }
    }

    fun clearNavigation() {
        _GuideUiState.update {
            it.copy(
                guidePath = emptyList(),
                destinationMarker = null,
                isGuiding = false,
                isTrackingMode = false
            )
        }
    }

    // =====================================
    // 코스추천 로직
    // =====================================


}