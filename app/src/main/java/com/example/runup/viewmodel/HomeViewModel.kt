package com.example.runup.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.BuildConfig
import com.example.runup.data.local.UserPreferenceDataSource
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.CourseRecommendation
import com.example.runup.domain.model.Path
import com.example.runup.domain.model.Scores
import com.example.runup.domain.model.SortDirection
import com.example.runup.domain.model.SortType
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.usecase.GetRecommendedCourseUseCase
import com.example.runup.domain.usecase.GetUserGoalUseCase
import com.example.runup.domain.usecase.GoalSettingUseCase
import com.example.runup.domain.usecase.RecordRunningUseCase
import com.example.runup.service.BleConnectionManager
import com.example.runup.service.BleSensorManager
import com.example.runup.service.LocationService
import com.example.runup.service.NaverMapApiService
import com.example.runup.service.PostureAnalyzer
import com.example.runup.service.TMapApiService
import com.example.runup.service.TMapRouteRequest
import com.example.runup.service.TtsManager
import com.example.runup.ui.navigation.CourseProgress
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.util.calculateDistance
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
    val selectedPath: Path? = null, // ── 🔹 추천/커뮤니티에서 확정된 경로 정보 📍
    val originalSelectedPath: Path? = null, // ── 🔹 왕복을 위해 보관할 원본 경로 📍 ──
    val selectedCourseName: String = "", // ── 🔹 카드나 바텀시트에 띄울 이름만 따로 보관 📍
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
    val isTracking: Boolean = false, //현재 달리는 중인지 running -> true, stop -> false
    val courseProgress: CourseProgress = CourseProgress.NONE
)

data class CourseRecommendationUiState(
    val goalDistance: Int = 0,
    val cameraLocation: LatLng? = null,
    val currentSort: SortType = SortType.DISTANCE,
    val isLoop: Boolean = true,
    val showLoop: Boolean = false,
    val showDistanceDialog: Boolean = false,
    val showSortDialog: Boolean = false,
    val isRecommendClick: Boolean = false,
    val recommendedCourses: List<CourseRecommendation> = emptyList(),
    val courseIndex: Int = 0,
    val isLoading:Boolean = false,
    val isAiMode: Boolean = false,
    val isFailSearchCourse: String = "",
    val maxSearchDistance: Int = 500, // 기본 500m (0.5km)
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val showMaxDistanceDialog: Boolean = false
)

data class AiPostureUiState(
    val isAiEnabled: Boolean = false,
    val currentPostureLabel: String = "AI 꺼짐",
    val leftBleState: String = "L: 대기 중",
    val rightBleState: String = "R: 대기 중",
)


enum class HomeTab { RUNNING, RECOMMEND }

@HiltViewModel
class HomeViewModel @Inject constructor(
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
    private val getRecommendedCourseUseCase: GetRecommendedCourseUseCase,

    @ApplicationContext private val context: Context,
): ViewModel(){
    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState

    private val _courseRecommendationUiState = MutableStateFlow(CourseRecommendationUiState())
    val courseRecommendationUiState: StateFlow<CourseRecommendationUiState> = _courseRecommendationUiState

    private val _isTracking = MutableStateFlow(false)

    private val _GuideUiState = MutableStateFlow(GuideUiState())
    val GuideUiState: StateFlow<GuideUiState> = _GuideUiState

    private val _AiPostureUiState = MutableStateFlow(AiPostureUiState())
    val AiPostureUiState: StateFlow<AiPostureUiState> = _AiPostureUiState

    private val _loadingTimer = MutableStateFlow(0)
    val loadingTimer: StateFlow<Int> = _loadingTimer

    private val inferenceBuffer = mutableListOf<FloatArray>()
    private var lastInferenceResult = ""

    private var recordingJob: Job? = null // 러닝 기록용 코루틴 잡

    private val _courseProgress = MutableStateFlow(CourseProgress.NONE)

    private var lastAnnouncedKm = 0 // 마지막으로 안내한 지점

    // 리포지토리의 주소 상태
    val addressUiState: StateFlow<AddressModel?> = locationRepository.addressState

    // lifecycle 로 값이 업데이트 될 때마다 자동 업데이트 + 하나의 생명 주기만 적용
    val runningUiState: StateFlow<RunningUiState> = combine(
        locationRepository.recordedNodes,
        locationRepository.totalDistance,
        _isTracking,
        locationRepository.totalTime,
        _courseProgress
    ) { nodes, totalDistance, isTracking, totalTime, progress ->

        checkDistanceMilestone(totalDistance, totalTime) // 일정거리마다 페이스 안내 체크 (현재 1km)

        RunningUiState(
            latLngList = nodes.map {
                LatLng(it.locationPoint.latitude, it.locationPoint.longitude)
            },
            totalDistance = totalDistance,
            isTracking = isTracking,
            totalTime = totalTime,
            courseProgress = progress
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
                        trimSelectedPath(it)
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
        _AiPostureUiState.update { currentState ->
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
                if (!_AiPostureUiState.value.isAiEnabled) return@collectLatest

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
            _AiPostureUiState.update { it.copy(currentPostureLabel = posture) }

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
                _AiPostureUiState.update { it.copy(leftBleState = "L: $state") }
            }
        }
        // 오른쪽 신발 상태 관찰
        viewModelScope.launch {
            bleConnectionManager.rightConnectionState.collectLatest { state ->
                _AiPostureUiState.update { it.copy(rightBleState = "R: $state") }
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
            locationRepository.startTimer()
            startRunningTracking()
        }
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

        locationRepository.startForegroundTracking()

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
        else {
            locationRepository.startTimer()
            _isTracking.value = true
        }
        //recordingJob?.cancel()

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

        _courseProgress.value = CourseProgress.NONE
        lastAnnouncedKm = 0 // 다음 러닝을 위해 리셋!
        _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
    }

    fun selectTab(tab: HomeTab) {
        // 탭 선택 상태 업데이트
        _homeUiState.update { it.copy(selectedTab = tab) }

        // 2. ── 🔹 [핵심] 러닝 중 탭 복구 로직 📍 ──
        if (tab == HomeTab.RUNNING) {
            // 현재 러닝 데이터가 있거나 트래킹 중이라면 UI 모드를 RUN으로 강제 전환
            if (runningUiState.value.isTracking || runningUiState.value.totalDistance > 0) {
                _homeUiState.update { it.copy(homeUi = HomeUi.RUN) }
            } else {
                // 러닝 중이 아니라면 일반 HOME 모드로
                _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
            }
        }
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

    fun onSearchClick() {
        if(_courseRecommendationUiState.value.isRecommendClick){

        }
        else{
            updateRecommendState {
                it.copy(
                    isLoading = true
                )
            }

            val location = _homeUiState.value.currentLocation ?: run {
                Log.e("RUNUP_TEST", "현재 위치가 없습니다.")
                return
            }
            viewModelScope.launch {
                val result = getRecommendedCourseUseCase.invoke(
                    _courseRecommendationUiState.value.goalDistance,
                     //GeoPoint(35.88544455378175, 128.61535052161116),
                    location,
                    _courseRecommendationUiState.value.isLoop,
                    _courseRecommendationUiState.value.currentSort,
                    3,
                    maxSearchDistance = _courseRecommendationUiState.value.maxSearchDistance, // 📍 추가
                    sortDirection = _courseRecommendationUiState.value.sortDirection // 📍 추가
                )

                when (result) {
                    is AuthResult.Success -> {
                        Log.d("RUNUP_TEST", "총 추천 개수: ${result.data.size}")

                        updateRecommendState {
                            it.copy(
                                recommendedCourses = result.data,
                                courseIndex = 0
                            )
                        }
                        // ── 🔹 [추가] 검색 성공 시 메인 화면 모드를 RECOMMEND로 전환 ── 📍
                        _homeUiState.update { it.copy(homeUi = HomeUi.RECOMMEND) }

                        Log.d("RUNUP_TEST", "추천 코스 목록: ${_courseRecommendationUiState.value.recommendedCourses}")

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
                        updateRecommendState {
                            it.copy(
                                isRecommendClick = true,
                                isLoading = false
                            )
                        }
                    }

                    is AuthResult.Fail -> {
                        updateRecommendState {
                            it.copy(
                                isLoading = false,
                                isRecommendClick = false, // ── 🔹 상황 2로 가지 않고 상황 1 유지 📍 ──
                                isFailSearchCourse = result.message
                            )
                        }

                        viewModelScope.launch {
                            delay(5000) // 5초 대기
                            clearFailMessage()
                        }
                    }
                }
            }
        }
    }

    fun onAiSearchClick(userPrompt: String) {
        if (userPrompt.isBlank()) return // 빈 값 방어

        // 로딩 시작
        updateRecommendState { it.copy(isLoading = true, isRecommendClick = false) }

        val location = _homeUiState.value.currentLocation ?: run {
            Log.e("RUNUP_TEST", "현재 위치가 없습니다.")
            return
        }

        val address = addressUiState.value
        val currentAddressString = address?.let { "${it.city} ${it.district}".trim() } ?: ""
        val maxSearchDist = _courseRecommendationUiState.value.maxSearchDistance

        viewModelScope.launch {
            // AI 전용 UseCase 호출 (두 번째 invoke 함수 사용)
            val result = getRecommendedCourseUseCase.invoke(
                courseDistance = _courseRecommendationUiState.value.goalDistance,
                currentLocation = location,
                currentAddress = currentAddressString,
                isLoop = _courseRecommendationUiState.value.isLoop,
                userPrompt = userPrompt, // 👈 채팅창에서 받은 텍스트
                count = 3,
                maxSearchDistance = maxSearchDist
            )

            when (result) {
                is AuthResult.Success -> {
                    updateRecommendState {
                        it.copy(
                            recommendedCourses = result.data,
                            courseIndex = 0,
                            isLoading = false,
                            isRecommendClick = true // 결과 모드로 전환
                        )
                    }
                    Log.d("RUNUP_GEMINI_SEARCH", "${result.data}")
                    // 지도를 RECOMMEND 모드로 바꿔서 카드와 경로가 뜨게 함
                    _homeUiState.update { it.copy(homeUi = HomeUi.RECOMMEND) }
                }
                is AuthResult.Fail -> {
                    updateRecommendState {
                        it.copy(
                            isLoading = false,
                            isRecommendClick = false, // ── 🔹 상황 2로 가지 않고 상황 1 유지 📍 ──
                            isFailSearchCourse = result.message
                        )
                    }
                    viewModelScope.launch {
                        delay(5000) // 5초 대기
                        clearFailMessage()
                    }
                }
            }
        }
    }

    // 2. 메시지 삭제 함수 추가
    fun clearFailMessage() {
        updateRecommendState { it.copy(isFailSearchCourse = "") }
    }

    fun selectRecommendCourse(course: CourseRecommendation) {
        _homeUiState.update { it.copy(
            selectedPath = course.path, // 👈 딱 path만 저장!
            originalSelectedPath = course.path,
            selectedCourseName = course.originCourse.id,
            homeUi = HomeUi.HOME
        ) }
        clearRecommendation()
    }

    // ── 🔹 [추가] 만약 선택한 코스를 취소하고 싶을 때를 대비 📍
    fun clearSelectedCourse() {
        _homeUiState.update { it.copy(
            selectedPath = null,
            originalSelectedPath = null,
            selectedCourseName = ""
        )}
        clearNavigation() // 코스를 안 볼 거면 길 안내도 당연히 종료
    }

    private fun updateRecommendState(
        transform: (CourseRecommendationUiState) -> CourseRecommendationUiState
    ) {
        _courseRecommendationUiState.update { state ->
            val newState = transform(state)
            newState.copy(
                cameraLocation = resolveRecommendCameraLocation(newState)
            )
        }
    }

    private fun resolveRecommendCameraLocation(state: CourseRecommendationUiState): LatLng? {
        return if (state.isRecommendClick) {
            // 코스를 선택(클릭)한 상태라면 해당 코스의 중심점을 반환
            state.recommendedCourses.getOrNull(state.courseIndex)?.path?.centerPoint?.let {
                LatLng(it.latitude, it.longitude)
            }
        } else {
            // 그 외에는 현재 내 위치를 반환
            _homeUiState.value.currentLocation?.let { LatLng(it.latitude, it.longitude) }
        }
    }

    // 추천 코스 인덱스 조절 (이전/다음)
    fun addRecommendCourseIndex() {
        val size = _courseRecommendationUiState.value.recommendedCourses.size
        if (size == 0) return
        updateRecommendState {
            it.copy(courseIndex = (it.courseIndex + 1) % size)
        }
    }

    fun subtractRecommendCourseIndex() {
        val size = _courseRecommendationUiState.value.recommendedCourses.size
        if (size == 0) return
        updateRecommendState {
            val newIdx = if (it.courseIndex - 1 < 0) size - 1 else it.courseIndex - 1
            it.copy(courseIndex = newIdx)
        }
    }

    fun clearRecommendation() {
        updateRecommendState {
            it.copy(
                isRecommendClick = false,
                recommendedCourses = emptyList(),
                courseIndex = 0
            )
        }
        _homeUiState.update { it.copy(homeUi = HomeUi.HOME) }
    }

    fun openRecommendLoopDialog() {
        updateRecommendState { it.copy(showLoop = true) }
    }

    fun closeRecommendLoopDialog() {
        updateRecommendState { it.copy(showLoop = false) }
    }

    fun selectRecommendLoop(isLoop: Boolean) {
        updateRecommendState {
            it.copy(
                isLoop = isLoop,
                showLoop = false // 다이얼로그 닫기
            )
        }
    }

    fun openRecommendDistanceDialog() {
        updateRecommendState { it.copy(showDistanceDialog = true) }
    }

    fun closeRecommendDistanceDialog() {
        updateRecommendState { it.copy(showDistanceDialog = false) }
    }

    fun confirmRecommendDistance(distanceKm: Int) {
        val distanceMeter = distanceKm * 100
        updateRecommendState {
            it.copy(showDistanceDialog = false, goalDistance = distanceMeter)
        }
    }

    fun confirmRecommendSort(sortType: SortType) {
        updateRecommendState {
            it.copy(showSortDialog = false, currentSort = sortType)
        }
    }

    fun setCourseFromCommunity(path: Path, authorName: String) {
        _homeUiState.update { it.copy(
            selectedPath = path, // 👈 여기서도 path만!
            selectedCourseName = "$authorName 님의 코스",
            homeUi = HomeUi.HOME,
            selectedTab = HomeTab.RECOMMEND
        ) }
    }

    fun toggleAiRecommendMode() {
        updateRecommendState {
            it.copy(
                isAiMode = !it.isAiMode // 현재 상태를 반전시킴 🔄
            )
        }
    }

    // ── 페이스 안내 로직 📍 ──
    private fun checkDistanceMilestone(totalDistance: Double, totalTime: Int) {
        if (_homeUiState.value.homeUi != HomeUi.RUN) return

        val currentKm = (totalDistance / 1000).toInt() // 현재 몇 km 지점인지 계산 1km 마다
        val goalPace = _homeUiState.value.goalPace

        // 새로운 1km 지점을 통과했을 때만 실행
        if (currentKm > lastAnnouncedKm && currentKm > 0) {
            lastAnnouncedKm = currentKm

            // 1. 현재 평균 페이스 계산 (초/km)
            val averagePace = if (totalDistance > 0) (totalTime / totalDistance) * 1000 else 0.0
            val paceMin = (averagePace / 60).toInt()
            val paceSec = (averagePace % 60).toInt()

            // 2. 기본 멘트 생성
            var message = "${currentKm} 킬로미터 통과. 현재 페이스는 ${paceMin}분 ${paceSec}초입니다."

            // 3. 목표 페이스와 비교 (목표 페이스가 설정되어 있을 때만)
            if (goalPace > 0) {
                message += if (averagePace <= goalPace) {
                    " 목표 페이스보다 빠릅니다. 페이스를 유지하세요!"
                } else {
                    " 목표 페이스보다 느립니다. 조금 더 힘내세요!"
                }
            }

            // 4. TTS 호출
            ttsManager.speakOut(message)
            Log.d("VoicePacer", "Announced: $message")
        }
    }

    private fun trimSelectedPath(currentLocation: GeoPoint) {
        val currentPath = _homeUiState.value.selectedPath ?: return
        val points = currentPath.points.toMutableList()
        if (points.isEmpty()) return

        // 1. 목적지 즉각 판단 (Fast-track)은 그대로 유지합니다.
        val lastPoint = points.last()
        val distanceToGoal = calculateDistance(
            currentLocation,
            GeoPoint(lastPoint.latitude, lastPoint.longitude)
        )

        if (distanceToGoal < 12.0) {
            handleCourseCompletion()
            return
        }

        // ── 🔹 [핵심] GPS 점프 대응 로직 📍 ──
        // 내 주변 15m 이내에 있는 점들 중 리스트에서 가장 뒤에 있는 놈의 인덱스를 찾습니다.
        // 너무 먼 점까지 찾으면 코스가 겹칠 때 버그가 생기므로 앞부분 20개 정도만 훑습니다.
        val lookAheadThreshold = 20
        val searchWindow = if (points.size > lookAheadThreshold) points.take(lookAheadThreshold) else points

        var lastIndexInRange = -1

        searchWindow.forEachIndexed { index, point ->
            val dist = calculateDistance(
                currentLocation,
                GeoPoint(point.latitude, point.longitude)
            )
            // GPS가 튀는 걸 감안해서 10~12m 정도로 넉넉하게 잡습니다.
            if (dist < 10.0) {
                lastIndexInRange = index
            }
        }

        // 2. 인덱스를 찾았다면, 해당 지점까지의 모든 점을 리스트에서 한꺼번에 제거합니다.
        if (lastIndexInRange != -1) {
            // 0번부터 lastIndexInRange까지 삭제
            repeat(lastIndexInRange + 1) {
                if (points.isNotEmpty()) points.removeAt(0)
            }

            _homeUiState.update { it.copy(selectedPath = currentPath.copy(points = points)) }

            // 모든 점이 지워졌다면 완주 처리
            if (points.isEmpty()) {
                handleCourseCompletion()
            }
        }
    }

    // 완주 로직 분리 (기존 checkCourseProgress의 when 문 로직 통합)
    private fun handleCourseCompletion() {
        val isLoop = _courseRecommendationUiState.value.isLoop
        val currentProgress = _courseProgress.value
        val backupPath = _homeUiState.value.originalSelectedPath

        when {
            // [편도 완주]
            !isLoop && currentProgress == CourseProgress.NONE -> {
                _courseProgress.value = CourseProgress.REACHED_END
                clearSelectedCourse()

                viewModelScope.launch { ttsManager.speakOut("선택한 코스를 완주했습니다!") }
            }

            // [왕복 반환점] -> 경로를 뒤집어서 새로 깔아줌 🔄
            isLoop && currentProgress == CourseProgress.NONE -> {
                backupPath?.let { path ->
                    val reversedPoints = path.points.reversed()
                    _homeUiState.update { it.copy(
                        selectedPath = path.copy(points = reversedPoints) // 다시 점들이 생겨남!
                    ) }
                    _courseProgress.value = CourseProgress.RETURNING
                    viewModelScope.launch { ttsManager.speakOut("반환점을 통과했습니다! 이제 돌아갈게요.") }
                }
            }

            // [왕복 최종 복귀]
            isLoop && currentProgress == CourseProgress.RETURNING -> {
                _courseProgress.value = CourseProgress.BACK_AT_START
                clearSelectedCourse()

                viewModelScope.launch { ttsManager.speakOut("다시 돌아왔어요! 완벽한 왕복 달리기였네요.") }
            }
        }
    }

    // 2. ViewModel 함수 추가
    fun openMaxDistanceDialog() {
        _courseRecommendationUiState.update { it.copy(showMaxDistanceDialog = true) }
    }
    fun closeMaxDistanceDialog() {
        _courseRecommendationUiState.update {
            it.copy(showMaxDistanceDialog = false)
        }
    }

    fun confirmMaxDistance(distanceKm: Int) {
        _courseRecommendationUiState.update {
            it.copy(maxSearchDistance = distanceKm * 100, showMaxDistanceDialog = false)
        }
    }

    fun toggleSortDirection() {
        _courseRecommendationUiState.update {
            val next = if (it.sortDirection == SortDirection.DESCENDING) SortDirection.ASCENDING else SortDirection.DESCENDING
            it.copy(sortDirection = next)
        }
    }

}