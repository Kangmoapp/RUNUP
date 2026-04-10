package com.example.runup.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MyNaverMap(
    targetLocation: LatLng,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    // 1. 내 위치를 지도에 전달할 소스 생성 (기억할 수 있도록 remember 사용)
    val locationSource = remember {
        com.naver.maps.map.util.FusedLocationSource(context as Activity, 1000)
    }

    // 카메라 상태 관리
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(targetLocation, 16.0)
    }

    // 2. 아까 작성하신 mapProperties와 uiSettings 정의
    val mapProperties = MapProperties(
        locationTrackingMode = LocationTrackingMode.Follow, // 내 위치 활성화
    )
    val uiSettings = MapUiSettings(
        isLocationButtonEnabled = true // 내 위치 버튼 활성화
    )

    // 위치 변경 시 카메라 애니메이션 이동
    LaunchedEffect(targetLocation) {
        cameraPositionState.animate(
            CameraUpdate.scrollTo(targetLocation)
        )
    }

    NaverMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        locationSource = locationSource,
    )
}