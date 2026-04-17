package com.example.runup.domain.repository

import com.example.runup.domain.model.Node
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val recordedNodes: StateFlow<List<Node>>
    val totalDistance: StateFlow<Double>
    val currentLocation: StateFlow<GeoPoint?>
    val currentBearing: StateFlow<Float>

    fun updateCurrentLocation(geoPoint: GeoPoint)
    fun addNodeFromCurrentLocation()
    fun clearData()

    fun startTracking()
    fun stopTracking()
    fun markLastNodeAsStopped()
}