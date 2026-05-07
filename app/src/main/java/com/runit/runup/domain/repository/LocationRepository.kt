package com.runit.runup.domain.repository

import com.runit.runup.domain.model.AddressModel
import com.runit.runup.domain.model.AdmVO
import com.runit.runup.domain.model.Node
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val recordedNodes: StateFlow<List<Node>>
    val totalDistance: StateFlow<Double>
    val currentLocation: StateFlow<GeoPoint?>
    val currentBearing: StateFlow<Float>
    val totalTime: StateFlow<Int>

    fun updateCurrentLocation(geoPoint: GeoPoint)
    fun addNodeFromCurrentLocation()
    fun clearData()

    fun startTracking()
    fun startForegroundTracking()
    fun stopTracking()
    fun markLastNodeAsStopped()

    suspend fun getAddressFromCoords(lat: Double, lng: Double): AddressModel?

    suspend fun fetchLocations(parentCode: String?, locationName: String? = null): List<AdmVO>

    val addressState: StateFlow<AddressModel?> // 🔹 추가: 주소 상태 관찰
    suspend fun refreshAddressIfNeeded(lat: Double, lng: Double) // 🔹 추가: 필요 시 갱신 로직

    fun startTimer()
    fun stopTimer()
}