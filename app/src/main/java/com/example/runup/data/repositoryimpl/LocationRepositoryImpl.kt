package com.example.runup.data.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.runup.BuildConfig
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AdmVO
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.service.GovLocationApiService
import com.example.runup.service.LocationService
import com.example.runup.service.NaverMapApiService
import com.example.runup.ui.util.calculateDistance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val application: Application,
    private val naverMapApiService: NaverMapApiService,
    private val govLocationApiService: GovLocationApiService
) : LocationRepository, SensorEventListener {

    private val _recordedNodes = MutableStateFlow<List<Node>>(emptyList())
    override val recordedNodes: StateFlow<List<Node>> = _recordedNodes

    private val _totalDistance = MutableStateFlow(0.0)
    override val totalDistance: StateFlow<Double> = _totalDistance

    // ── 🔹 [NEW] 시간 데이터 관리 ── 📍
    private val _totalTime = MutableStateFlow(0)
    override val totalTime: StateFlow<Int> = _totalTime.asStateFlow()

    private var timerJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    private val _currentBearing = MutableStateFlow(0.0f)
    override val currentBearing: StateFlow<Float> = _currentBearing

    // 🔹 마지막으로 API를 호출했던 좌표 저장 (메모리 내)
    private var lastFetchedLocation: GeoPoint? = null
    private val MIN_DISTANCE_THRESHOLD = 200.0

    // 🔹 수문장 역할을 할 시간 관리 변수
    private var lastFetchedTime: Long = 0L
    private val MIN_TIME_THRESHOLD = 3 * 60 * 1000L // 3분 (밀리초)

    // 🔹 전역 주소 상태
    private val _addressState = MutableStateFlow<AddressModel?>(null)
    override val addressState: StateFlow<AddressModel?> = _addressState

    override fun updateCurrentLocation(geoPoint: GeoPoint) {
        _currentLocation.value = geoPoint
    }

    override fun addNodeFromCurrentLocation() {
        val currentPos = _currentLocation.value ?: return
        val newNode = Node(
            locationPoint = currentPos,
            score = Scores(0.0, 0.0, 0.0)
        )

        val currentList = _recordedNodes.value
        if (currentList.isNotEmpty()) {
            val lastNode = currentList.last()

            // 마지막 노드가 '정지(isStop)' 상태가 아닐 때만 거리를 계산하여 합산함
            if (!lastNode.stop) {
                val distance = calculateDistance(
                    GeoPoint(lastNode.locationPoint.latitude, lastNode.locationPoint.longitude),
                    GeoPoint(newNode.locationPoint.latitude, newNode.locationPoint.longitude)
                )
                _totalDistance.value += distance
            }
        }
        _recordedNodes.value = currentList + newNode
    }

    override fun clearData() {
        _recordedNodes.value = emptyList()
        _totalDistance.value = 0.0
        stopTimer()
        _totalTime.value = 0
    }

    override fun startTracking() {
        // ── 🔹 Intent에 "START" 액션을 반드시 추가 ── 📍
        val intent = Intent(application, LocationService::class.java).apply {
            action = "START"
        }
        application.startForegroundService(intent)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun stopTracking() {
        val intent = Intent(application, LocationService::class.java).apply {
            action = "STOP_TRACKING"
        }
        application.startService(intent)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values

        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            // orientation[0]이 Azimuth(방향)이며 라디안 단위입니다. 이를 도(degree)로 변환합니다.
            val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            _currentBearing.value = (degrees + 360) % 360
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun markLastNodeAsStopped() {
        val currentList = _recordedNodes.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastIndex = currentList.size - 1
            val lastNode = currentList[lastIndex]

            // 마지막 노드를 복사하며 isStop만 true로 변경
            currentList[lastIndex] = lastNode.copy(stop = true)

            _recordedNodes.value = currentList
        }
    }

    override suspend fun getAddressFromCoords(lat: Double, lng: Double): AddressModel? {
        return try {
            val response = naverMapApiService.reverseGeocode("$lng,$lat") //경도, 위도
            Log.d("location", "geocode api 호출")
            if (response.status.code == 0 && response.results.isNotEmpty()) {
                val region = response.results[0].region
                val city = region.area1.name
                val district = region.area2.name
                val dong = region.area3.name

                AddressModel(
                    fullAddress = "$city $district $dong",
                    displayAddress = "$district $dong",
                    city = city,
                    district = district,
                    dong = dong
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchLocations(
        parentCode: String?,
        locationName: String?
    ): List<AdmVO> {
        Log.d("LocationAPI", "fetchLocations 진입 - parentCode: [$parentCode], locationName: [$locationName]")
        return try {
            val targetParent = if (parentCode == "0" || parentCode.isNullOrEmpty())
                "0000000000"
            else parentCode

            val response = govLocationApiService.getLocations(
                key = BuildConfig.GOV_DATA_KEY,
                parentCode = targetParent,
                locationName = locationName, //"서울특별시" 처럼 이름을 직접 넘김
                numOfRows = 500,
                pageNo = 1
            )

            val rows = response.stanReginCd
                ?.firstOrNull { it.row != null }
                ?.row
                ?: emptyList()

            // 🔹 직계 자식만 필터
            val filtered = rows.filter { it.locathighCd == targetParent }

            Log.d("LocationAPI", "요청코드: $targetParent, 검색어: ${locationName}, 전체: ${rows.size}개, 직계자식: ${filtered.size}개")

            filtered.sortedBy { it.lowestAdmName }

        } catch (e: Exception) {
            Log.e("LocationAPI", "Error: ${e.message}")
            emptyList()
        }
    }

    // 🔹 핵심 리팩터링: 거리 체크 + API 호출 + 상태 업데이트를 한 번에 처리
    override suspend fun refreshAddressIfNeeded(lat: Double, lng: Double) {
        // 🔹 [수문장 로직] 3분이 지나지 않았으면 아래 로직은 쳐다보지도 않고 종료
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFetchedTime < MIN_TIME_THRESHOLD) {
            return
        }
        lastFetchedTime = currentTime

        val currentPoint = GeoPoint(lat, lng)
        val lastPoint = lastFetchedLocation

        // 처음이거나 200m 이상 이동했을 때만 실행
        if (lastPoint == null || calculateDistance(lastPoint, currentPoint) >= MIN_DISTANCE_THRESHOLD) {
            val result = getAddressFromCoords(lat, lng)
            if (result != null) {
                _addressState.value = result
                lastFetchedLocation = currentPoint // 기준점 갱신
                Log.d("LocationAPI", "주소 갱신 성공: ${result.fullAddress}")
            }
        }
    }

    // ── 🔹 [NEW] 타이머 제어 로직 ── 📍
    override fun startTimer() {
        if (timerJob?.isActive == true) return // 이미 실행 중이면 무시

        timerJob = repositoryScope.launch {
            while (isActive) {
                delay(1000L)
                _totalTime.value += 1
            }
        }
    }

    override fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

}

