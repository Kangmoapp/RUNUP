package com.example.runup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.viewmodel.RunningViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunningTestScreen(
    viewModel: RunningViewModel = hiltViewModel()
) {
    // 1. 상태 구독 (ViewModel의 StateFlow들을 관찰)
    val pathPoints by viewModel.pathPoints.collectAsState() // 기록된 경로 리스트
    val currentUserLocation by viewModel.userLocation.collectAsState() // 실시간 내 위치 (GeoPoint)
    val distance by viewModel.currentDistance.collectAsState() // 누적 거리

    // 2. 경로 데이터 변환 (Node -> LatLng)
    val latLngList = remember(pathPoints) {
        pathPoints.map { LatLng(it.locationPoint.latitude, it.locationPoint.longitude) }
    }

    // 3. 카메라 상태 설정
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.8888, 128.6103), 17f)
    }

    // [중요] 내 위치(currentUserLocation)가 바뀔 때마다 카메라가 부드럽게 따라가도록 설정
    LaunchedEffect(currentUserLocation) {
        currentUserLocation?.let { geoPoint ->
            val target = LatLng(geoPoint.latitude, geoPoint.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(target))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 지도 영역 ---
        GoogleMap(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cameraPositionState = cameraPositionState,
            // 기본 파란점(isMyLocationEnabled) 대신 우리가 직접 관리하는 좌표를 쓰고 싶다면 false로 둬도 됩니다.
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            // [실시간 경로 그리기] - 기록 중인 경우에만 선이 늘어남
            if (latLngList.size >= 2) {
                Polyline(
                    points = latLngList,
                    color = Color.Blue,
                    width = 12f,
                    jointType = JointType.ROUND
                )
            }
        }

        // --- 컨트롤 영역 (하단 테스트 패널) ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 상단 정보 표시
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("현재 위치: ${currentUserLocation?.latitude ?: 0.0}, ${currentUserLocation?.longitude ?: 0.0}")
                    Text("거리: ${String.format("%.1f", distance)}m", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
                Text("기록된 노드: ${pathPoints.size}개")

                Spacer(modifier = Modifier.height(12.dp))

                // 1단계: 위치 추적 서비스 컨트롤 (GPS 켜기/끄기)
                Text("1. 위치 서비스 (LocationService)", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startCurrentLocationTracking() }, modifier = Modifier.weight(1f)) {
                        Text("추적 시작")
                    }
                    Button(
                        onClick = { viewModel.stopCurrentLocationTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("추적 종료")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2단계: 러닝 기록 컨트롤 (경로 쌓기/저장)
                Text("2. 러닝 기록 (Running Recording)", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startRunningTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("러닝 시작")
                    }
                    Button(
                        onClick = { viewModel.stopRunningTracking() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("기록 종료/저장")
                    }
                }
            }
        }
    }
}