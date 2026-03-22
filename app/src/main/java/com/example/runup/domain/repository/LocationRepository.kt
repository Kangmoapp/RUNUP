package com.example.runup.domain.repository

import android.annotation.SuppressLint
import com.example.runup.domain.model.AuthResult
import android.util.Log
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.pow

@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private val _recordedNodes = MutableStateFlow<List<Node>>(emptyList())
    val recordedNodes: StateFlow<List<Node>> = _recordedNodes

    // 실시간 누적 거리를 담는 StateFlow (단위: 미터)
    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance

    fun addNode(newNode: Node) {
        val currentList = _recordedNodes.value
        if (currentList.isNotEmpty()) {
            val lastNode = currentList.last()
            // 이전 노드와 새 노드 사이의 거리 계산 (미터 단위)
            val distance = calculateDistance(
                lastNode.locationPoint.latitude, lastNode.locationPoint.longitude,
                newNode.locationPoint.latitude, newNode.locationPoint.longitude
            )
            _totalDistance.value += distance
        }
        _recordedNodes.value = currentList + newNode
        Log.d("Check", "RunningPositionLS = ${_recordedNodes.value}")
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

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): AuthResult<GeoPoint> {
        return try {
            // 코루틴을 중단시키고 비동기 결과를 기다림
            suspendCancellableCoroutine { continuation ->
                val currentLocationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(AuthResult.Success(GeoPoint(location.latitude, location.longitude)))
                        } else {
                            // 위치 정보가 null인 경우 (GPS가 잡히지 않을 때 등)
                            continuation.resume(AuthResult.Fail("현재 위치를 찾을 수 없습니다. GPS 설정을 확인해주세요."))
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Google API 자체에서 에러가 난 경우
                        continuation.resume(AuthResult.Fail(exception.message ?: "위치 서비스 오류가 발생했습니다."))
                    }
            }
        } catch (e: Exception) {
            // 위의 로직 중 어디서든 예상치 못한 Crash가 발생하면 여기서 잡아줌
            AuthResult.Fail("시스템 오류: ${e.localizedMessage}")
        }
    }
}