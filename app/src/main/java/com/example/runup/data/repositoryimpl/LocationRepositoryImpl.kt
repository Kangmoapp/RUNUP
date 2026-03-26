package com.example.runup.data.repository

import android.app.Application
import android.content.Intent
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
    private val fusedLocationClient: FusedLocationProviderClient,
    private val application: Application
) : LocationRepository {

    private val _recordedNodes = MutableStateFlow<List<Node>>(emptyList())
    override val recordedNodes: StateFlow<List<Node>> = _recordedNodes

    private val _totalDistance = MutableStateFlow(0.0)
    override val totalDistance: StateFlow<Double> = _totalDistance

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation

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
            val distance = calculateDistance(
                lastNode.locationPoint.latitude, lastNode.locationPoint.longitude,
                newNode.locationPoint.latitude, newNode.locationPoint.longitude
            )
            _totalDistance.value += distance
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
    }

    override fun stopTracking() {
        val intent = Intent(application, LocationService::class.java).apply {
            action = "STOP_TRACKING"
        }
        application.startService(intent)
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
}