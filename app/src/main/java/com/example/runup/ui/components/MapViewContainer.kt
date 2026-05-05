package com.example.runup.ui.components

import android.graphics.PointF
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runup.R
import com.example.runup.ui.navigation.HomeUi
import com.example.runup.ui.theme.PointColor
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import kotlin.collections.forEach

@Composable
fun MapViewContainer(
    cameraPosition:LatLng,
    recommendCameraLocation: LatLng?,
    bearing: Float = 0.0f,
    homeUi: HomeUi = HomeUi.RUN,
    latLngList: List<LatLng> = emptyList(),   // 지금까지 나의 러닝 경로 (러닝 모드)
    guidePath: List<LatLng> = emptyList(),    // 코스 시작점까지의 안내 경로
    recommendPath: List<LatLng> = emptyList(), // 추천된 후보 코스들의 경로 (추천 모드)
    selectedCoursePath: List<LatLng> = emptyList(), // 최종 선택된 코스 경로
    destinationMarkerPos: LatLng? = null,     // 목적지 마커
    guideDistance: Int = 0, // 경로까지 걸리는 거리
    guideDuration: Long = 0L, // 경로까지 걸리는 시간
    isTrackingMode: Boolean = false,
    isManualMode: Boolean,
    onManualModeChange: (Boolean) -> Unit,
    modifier:Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var isFirstLoad by remember { mutableStateOf(true) } // 처음 지도가 켜졌을 때만 순간이동을 하기 위한 플래그

    val locationSource = remember {
        com.naver.maps.map.util.FusedLocationSource(context as android.app.Activity, 1000)
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val recommendPathOverlay = remember {// 추천 코스 전용 오버레이
        PathOverlay().apply {
            color = Color.Cyan.toArgb() // 추천 코스는 하늘색으로 구분
            outlineColor = Color.Black.toArgb()
            width = with(density) { 8.dp.toPx() }.toInt()
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
            patternInterval = with(density) { 20.dp.toPx() }.toInt()
        }
    }

    val selectedPathOverlay = remember {// 최종 선택 코스 전용 오버레이
        PathOverlay().apply {
            color = Color.Yellow.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 15.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
        }
    }

    val guidePathOverlay = remember {// 안내 경로용 오버레이
        PathOverlay().apply {
            color = PointColor.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 12.dp.toPx() }.toInt()
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
            patternImage = OverlayImage.fromResource(R.drawable.arrow_path)
            patternInterval = with(density) { 20.dp.toPx() }.toInt() // 화살표 간격
        }
    }

    val runningPathOverlay = remember {// 실제 러닝한 경로 오버레이
        PathOverlay().apply {
            color = PointColor.toArgb()
            outlineColor = Color.Black.toArgb()
            width = with(density) { 8.dp.toPx() }.toInt()
            outlineWidth = with(density) { 3.dp.toPx() }.toInt()
        }
    }

    val destMarker = remember {// 목적지 마커
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_marker_icon_blue)
            density.run { // 캡션 너비 등도 density 스코프 안에서 계산
                captionRequestedWidth = 100.dp.toPx().toInt()
            }
            captionTextSize = 14f
        }
    }

    val infoWindow = remember { // 경로 정보 말풍선용 InfoWindow
        com.naver.maps.map.overlay.InfoWindow().apply {
            adapter = object : com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter(context) {
                override fun getText(infoWindow: com.naver.maps.map.overlay.InfoWindow): CharSequence {
                    val km = String.format("%.1f", guideDistance / 1000f)
                    val min = guideDuration / 1000 / 60

                    return "${km}km (${min}분)"
                }
            }
            alpha = 0.9f
        }
    }

    val anchorMarker = remember {// 풍선을 고정할 투명 마커
        Marker().apply {
            icon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
            alpha = 0f // 마커 자체는 투명하게
            width = 1
            height = 1
        }
    }

    val destinationGlow = remember {// 목적지 지점을 강조할 글로우 효과 (은은하게 퍼지는 원)
        CircleOverlay().apply {
            radius = 5.0
            color = PointColor.copy(alpha = 0.2f).toArgb() // 우리 앱 포인트 컬러의 반투명 버전
            outlineColor = PointColor.toArgb() // 테두리는 선명하게
            outlineWidth = with(density) { 2.dp.toPx() }.toInt()
        }
    }

    val destinationCenter = remember {// 중심부의 작은 점
        CircleOverlay().apply {
            radius = 3.0 // 아주 작은 원
            color = Color.White.toArgb() // 흰색으로 강조
            outlineColor = Color.Black.toArgb()
            outlineWidth = with(density) { 1.dp.toPx() }.toInt()
        }
    }

    DisposableEffect(lifecycleOwner) {

        val observer = object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            guidePathOverlay.map = null
            destMarker.map = null
        }
    }

    var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { naverMap ->
            naverMapInstance = naverMap
            naverMap.locationSource = locationSource
            naverMap.addOnCameraChangeListener { reason, _ ->
                if (reason == CameraUpdate.REASON_GESTURE) {
                    onManualModeChange(true)
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = modifier,
        update = { _ ->
            val naverMap = naverMapInstance ?: return@AndroidView

            // --------------------------------------------- 카메라 조정 로직 ----------------------------------

            // 첫 로딩 -> 애니메이션 없이 바로 뜸
            if (isFirstLoad) {
                val initialCamera = CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                naverMap.moveCamera(initialCamera)
                isFirstLoad = false
                return@AndroidView
            }

            // 경로 안내 -> 따라가기 모드 설정 (현재 지도 모드와 위젯 상태가 다를 때만 업데이트)
            val targetMode = if (isTrackingMode) LocationTrackingMode.Face else LocationTrackingMode.None
            if (naverMap.locationTrackingMode != targetMode) {
                naverMap.locationTrackingMode = targetMode
                if (isTrackingMode) {
                    naverMap.moveCamera(
                        CameraUpdate.toCameraPosition(
                            CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                        ).animate(CameraAnimation.Easing)
                    )
                    return@AndroidView
                }
            }

            if (isTrackingMode) {// 따라가기 모드일 때는 시스템이 위치를 추적하므로 수동 이동 건너뜀
                naverMap.locationOverlay.isVisible = true
            } else {
                // 따라가기 모드 X -> 내 위치 오버레이 수동 설정
                naverMap.locationOverlay.apply {
                    isVisible = true
                    position = cameraPosition
                    setBearing(bearing)
                    subIcon = OverlayImage.fromResource(com.naver.maps.map.R.drawable.navermap_default_location_overlay_sub_icon_arrow)
                }

                // 카메라 이동 우선순위
                when {
                    homeUi == HomeUi.RUN -> { // 1순위 : 러닝 중일 때
                        naverMap.moveCamera(
                            CameraUpdate.toCameraPosition(
                                CameraPosition(cameraPosition, 18.0, 0.0, bearing.toDouble())
                            ).pivot(PointF(0.5f, 0.65f)).animate(CameraAnimation.Easing, 1200)
                        )
                    }
                    isManualMode -> { // 2순위 : 자유 모드 활성화 시: 아래의 모든 카메라 이동 명령을 무시
                        // 사용자가 지도를 마음대로 움직이게 둠
                    }

                    guidePath.size >= 2 -> { // 3순위 : 경로 안내 중일 때
                        val bounds = LatLngBounds.Builder().apply {
                            guidePath.forEach { include(it) }
                        }.build()
                        naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150).animate(CameraAnimation.Easing, 1500))
                    }

                    selectedCoursePath.size >= 2 -> { // 4순위 : homeUi가 HOME이더라도 selectedCoursePath 가 있으면 코스를 우선적으로 비춤
                        val boundsBuilder = LatLngBounds.Builder()
                        boundsBuilder.include(cameraPosition) // 현재 내 위치 포함

                        selectedCoursePath.forEach { latLng -> // selectedCoursePath 직접 순회
                            boundsBuilder.include(latLng)
                        }

                        try {
                            val bounds = boundsBuilder.build()
                            val cameraUpdate = CameraUpdate.fitBounds(bounds, 350) // 모든 지점이 포함되도록 카메라 업데이트 생성
                                .animate(CameraAnimation.Easing, 1000)
                            naverMap.moveCamera(cameraUpdate)
                        } catch (e: Exception) { // 혹시 모를 에러 발생 시 리스트의 첫 번째 좌표로 이동하는 방어 로직
                            val fallbackTarget = selectedCoursePath.first()
                            naverMap.moveCamera(
                                CameraUpdate.toCameraPosition(CameraPosition(fallbackTarget, 15.5))
                                    .animate(CameraAnimation.Easing, 1000)
                            )
                        }
                    }

                    homeUi == HomeUi.RECOMMEND -> { // 5순위 : 코스 추천 후보 보여주기
                        if (recommendCameraLocation != null) {
                            val bounds = LatLngBounds.Builder()
                                .include(cameraPosition)
                                .include(recommendCameraLocation)
                                .build()
                            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 350).animate(CameraAnimation.Easing, 1000))
                        }
                    }

                    homeUi == HomeUi.HOME -> { // 6순위: 일반 홈 화면 (찜한 코스도 없고, 구경 중도 아닐 때 -> 나를 비춤)
                        naverMap.moveCamera(
                            CameraUpdate.toCameraPosition(CameraPosition(cameraPosition, 18.0))
                                .animate(CameraAnimation.Easing, 1200)
                        )
                    }
                }
            }

            // 코스 그리는 부분 (내 러닝 코스, 경로 코스, 추천 코스)
            if (latLngList.size >= 2) { // 내 러닝 코스
                runningPathOverlay.coords = latLngList
                runningPathOverlay.map = naverMap // 지도에 부착
            } else {
                runningPathOverlay.map = null    // 좌표 부족 시 제거
            }

            if (selectedCoursePath.size >= 2) {
                selectedPathOverlay.coords = selectedCoursePath
                selectedPathOverlay.map = naverMap

                // ── 🔹 목적지 강조 로직 추가 📍 ──
                selectedCoursePath.lastOrNull()?.let { lastPoint ->
                    destinationGlow.center = lastPoint
                    destinationGlow.map = naverMap

                    destinationCenter.center = lastPoint
                    destinationCenter.map = naverMap
                }
            } else {
                selectedPathOverlay.map = null
                destinationGlow.map = null
                destinationCenter.map = null
            }

            // 경로 가이드 모드
            if (guidePath.size >= 2) {
                guidePathOverlay.coords = guidePath
                guidePathOverlay.map = naverMap

                // 말풍선 어댑터 갱신
                infoWindow.adapter = object : com.naver.maps.map.overlay.InfoWindow.DefaultTextAdapter(context) {
                    override fun getText(infoWindow: com.naver.maps.map.overlay.InfoWindow): CharSequence {
                        val km = String.format("%.1f", guideDistance / 1000f)
                        val min = guideDuration / 1000 / 60
                        return "${km}km (${min}분)"
                    }
                }

                // 말풍선 위치 업데이트 및 유지
                val middleIndex = guidePath.size / 2
                anchorMarker.position = guidePath[middleIndex]
                anchorMarker.map = naverMap
                infoWindow.open(anchorMarker)
            } else {
                // 경로 데이터가 아예 없을 때만 지웁니다.
                guidePathOverlay.map = null
                anchorMarker.map = null
                infoWindow.close()
            }

            // 목적지 마커 표시
            destinationMarkerPos?.let {
                destMarker.position = it
                destMarker.map = naverMap
                destMarker.captionText = "목적지"
            } ?: run {
                destMarker.map = null
            }

            // ── 🔹 추천 경로(Path) 그리기 로직 ── 📍
            if (homeUi == HomeUi.RECOMMEND && recommendPath.size >= 2) {
                recommendPathOverlay.coords = recommendPath
                recommendPathOverlay.map = naverMap
            } else {
                recommendPathOverlay.map = null
            }

        }
    )
}