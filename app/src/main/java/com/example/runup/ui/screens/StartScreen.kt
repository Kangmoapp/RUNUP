package com.example.runup.ui.screens

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.viewmodel.StartViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.example.runup.R
import com.example.runup.ui.components.SignupText
import com.example.runup.ui.theme.White

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartScreen(
    onHomeClick: () -> Unit,
    viewModel: StartViewModel = hiltViewModel()
) {

    //위치 권한 받기
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

    val context = LocalContext.current
    StartContent(
        onLoginClick = {
            viewModel.onGoogleLoginClick(
                context = context,
                onSuccess = onHomeClick   // 로그인 성공 시 홈 이동
            )
        },
        onHomeClick = onHomeClick,
        hasLocationPermission = locationPermissionState.allPermissionsGranted,
        onRequestPermission = {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    )
}

@Composable
private fun StartContent(
    onLoginClick:()->Unit,
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
            modifier = Modifier.fillMaxSize()
        ) {
            SignupText(text = "달리기를 시작해\n 볼까요?",
                modifier = Modifier.padding(top = 100.dp, bottom = 30.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(start = 18.dp, end = 18.dp, bottom = 30.dp)
                    .background(color = White, shape = RoundedCornerShape(5.dp))
            ){
                Text(text = "좌우 슬라이드 뷰의 앱 튜토리얼 화면 예정",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize()
                    )
            }
            Image(
                painter = painterResource(R.drawable.google_light_sq_si),
                contentDescription = "Google Login",
                modifier = Modifier
                    .clickable { onLoginClick() }
            )

            /*
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

             */
        }
    }
}

@Preview
@Composable
private fun PreviewStartContent() {
    StartContent(
        {},
        onHomeClick = {},
        hasLocationPermission = false,
        onRequestPermission = {}
    )
}