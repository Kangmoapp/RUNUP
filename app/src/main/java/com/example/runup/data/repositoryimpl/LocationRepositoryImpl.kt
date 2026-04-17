package com.example.runup.data.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.service.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val application: Application
) : LocationRepository, SensorEventListener {

    private val _recordedNodes = MutableStateFlow<List<Node>>(emptyList())
    override val recordedNodes: StateFlow<List<Node>> = _recordedNodes

    private val _totalDistance = MutableStateFlow(0.0)
    override val totalDistance: StateFlow<Double> = _totalDistance

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    private val _currentBearing = MutableStateFlow(0.0f)
    override val currentBearing: StateFlow<Float> = _currentBearing

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
                    lastNode.locationPoint.latitude, lastNode.locationPoint.longitude,
                    newNode.locationPoint.latitude, newNode.locationPoint.longitude
                )
                _totalDistance.value += distance
            }
        }
        _recordedNodes.value = currentList + newNode
    }

    override fun clearData() {
        _recordedNodes.value = emptyList()
        _totalDistance.value = 0.0
    }

    override fun startTracking() {
        val intent = Intent(application, LocationService::class.java)
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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
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
}