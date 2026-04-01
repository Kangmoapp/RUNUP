package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runup.ui.theme.Gray
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MyGoogleMap(
    cameraPosition: LatLng,
    modifier: Modifier = Modifier,
    isCourse: Boolean = false,
    course: List<GeoPoint> =emptyList(),
){
    val defaultLocation = LatLng(35.8888, 128.6103)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 17f)
    }
    val mapProperties = MapProperties(
        isMyLocationEnabled = true
    )

    LaunchedEffect(cameraPosition) {
        cameraPosition.let { it ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 17f)
            )
        }
    }
    Box{
        GoogleMap(
            modifier = modifier
                .background(color = Gray)
                .fillMaxWidth()
                .height(450.dp),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ){
            if (isCourse && course.isNotEmpty()) {
                val pathPoints = course.map { point ->
                    LatLng(point.latitude, point.longitude)
                }

                Polyline(
                    points = pathPoints
                )
            }
        }
    }
}