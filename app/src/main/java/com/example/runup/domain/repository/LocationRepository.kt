package com.example.runup.domain.repository

import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {
    // 저장된 노드들
    private val _recordedNodes = MutableStateFlow<List<Node>>(emptyList())
    val recordedNodes: StateFlow<List<Node>> = _recordedNodes

    // 실시간 누적 거리를 담는 StateFlow (단위: 미터)
    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance

    // 단순 현재 위치 (수집되는 족족 업데이트)
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation

    // 서비스에서 호출: 원천 좌표만 업데이트
    fun updateCurrentLocation(geoPoint: GeoPoint) {
        _currentLocation.value = geoPoint
    }

    // ViewModel에서 호출: 현재 위치를 기반으로 노드 추가
    fun addNodeFromCurrentLocation() {
        val currentPos = _currentLocation.value ?: return
        val newNode = Node(
            locationPoint = currentPos,
            score = Scores(0.0, 0.0, 0.0)
        )

        val currentList = _recordedNodes.value
        if (currentList.isNotEmpty()) {
            val lastNode = currentList.last()
            val distance = calculateDistance(
                lastNode.locationPoint.latitude, lastNode.locationPoint.longitude,
                newNode.locationPoint.latitude, newNode.locationPoint.longitude
            )
            _totalDistance.value += distance
        }
        _recordedNodes.value = currentList + newNode
    }

    fun clearData() {
        _recordedNodes.value = emptyList()
        _totalDistance.value = 0.0
    }

    // 하버사인 공식 (두 좌표 사이의 직선 거리 계산)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // 지구 반지름 (m)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}