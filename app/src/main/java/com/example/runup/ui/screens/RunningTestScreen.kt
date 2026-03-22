package com.example.runup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    /*
    val pathPoints by viewModel.pathPoints.collectAsState()

    // 1. 노드(Node) 리스트를 지도가 이해할 수 있는 LatLng 리스트로 변환
    val latLngList = remember(pathPoints) {
        pathPoints.map { LatLng(it.locationPoint.latitude, it.locationPoint.longitude) }
    }

    // 2. 카메라 상태 설정 (최초 위치 혹은 현재 위치 기준)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            latLngList.lastOrNull() ?: LatLng(35.8888, 128.6103), // 좌표 없으면 경북대 근처나 기본위치
            16f
        )
    }

    // 실시간으로 좌표가 추가될 때 카메라가 내 위치를 따라가게 하고 싶다면
    LaunchedEffect(latLngList.size) {
        latLngList.lastOrNull()?.let { lastLatLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(lastLatLng))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 지도 영역 (화면의 70% 차지) ---
        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true), // 내 위치 파란 점 표시
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            // [실시간 경로 그리기]
            if (latLngList.size >= 2) {
                Polyline(
                    points = latLngList,
                    color = Color.Blue,
                    width = 15f,
                    jointType = JointType.ROUND // 선 연결 부위를 부드럽게
                )
            }
        }

        // --- 컨트롤 영역 (하단) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "수집된 좌표: ${pathPoints.size}개", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.startTracking() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("기록 시작")
                    }
                    Button(
                        onClick = { viewModel.stopAndSave() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("종료 및 저장")
                    }
                }
            }
        }
    }

     */
}