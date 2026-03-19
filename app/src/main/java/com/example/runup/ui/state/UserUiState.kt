package com.example.runup.ui.state

import com.google.android.gms.maps.model.LatLng

data class UserUiState(
    val goalDistance: Int = 0,
    val goalPace: Int = 0,
    val showDistanceDialog: Boolean = false,
    val latLngList: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val totalDistance: Double = 0.0,
    val hasLocationPermission: Boolean = false,
    val isTracking: Boolean = false
)
