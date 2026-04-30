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
import com.google.firebase.firestore.GeoPoint
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PathOverlay
import com.naver.maps.map.compose.rememberCameraPositionState

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MyNaverMap(
    cameraPosition: LatLng,
    modifier: Modifier = Modifier,
    isCourse: Boolean = false,
    course: List<GeoPoint> = emptyList(),
) {
    val defaultLocation = LatLng(35.8888, 128.6103)

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        cameraPositionState.move(
            CameraUpdate.scrollAndZoomTo(defaultLocation, 17.0)
        )
    }

    LaunchedEffect(cameraPosition) {
        cameraPositionState.animate(
            CameraUpdate.scrollAndZoomTo(cameraPosition, 17.0)
        )
    }

    Box {
        NaverMap(
            modifier = modifier
                .background(color = Gray)
                .fillMaxWidth()
                .height(450.dp),
            cameraPositionState = cameraPositionState
        ) {
            if (isCourse && course.size >= 2) {
                val pathPoints = course.map { point ->
                    LatLng(point.latitude, point.longitude)
                }

                PathOverlay(
                    coords = pathPoints
                )
            }
        }
    }
}