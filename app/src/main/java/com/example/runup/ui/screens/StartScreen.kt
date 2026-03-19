package com.example.runup.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.viewmodel.StartViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartScreen(
    onHomeClick: () -> Unit,
    viewModel: StartViewModel = hiltViewModel()
) {
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    StartContent(
        onHomeClick = onHomeClick,
        hasLocationPermission = locationPermissionState.allPermissionsGranted,
        onRequestPermission = {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    )
}

@Composable
private fun StartContent(
    onHomeClick: () -> Unit,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = {}
            ) {
                Text("구글로그인")
            }

            Button(
                onClick = onHomeClick,
                enabled = hasLocationPermission
            ) {
                Text(
                    if (hasLocationPermission) "홈화면"
                    else "위치 권한이 필요합니다"
                )
            }

            if (!hasLocationPermission) {
                Button(
                    onClick = onRequestPermission
                ) {
                    Text("권한 다시 요청")
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewStartContent() {
    StartContent(
        onHomeClick = {},
        hasLocationPermission = false,
        onRequestPermission = {}
    )
}