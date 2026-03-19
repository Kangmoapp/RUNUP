package com.example.runup.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.R
import com.example.runup.ui.components.CenterBar
import com.example.runup.ui.components.MenuButton
import com.example.runup.ui.state.UserUiState
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.MapSize
import com.example.runup.ui.theme.MapSpaceSize
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextWhite
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.RunningViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@Preview
@Composable
fun PreviewRunScreen() {
    RunningContent(
        onMenuClick = {},
        onRunClick = {},
        onCompleteClick = {},
        uiState = UserUiState(
            latLngList = listOf(
                LatLng(37.5665, 126.9780),
                LatLng(37.5651, 126.9895)
            ),
            currentLocation = LatLng(37.5665, 126.9780),
            totalDistance = 1700.0,
            hasLocationPermission = true,
            isTracking = true
        )
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunningScreen(
    onMenuClick: () -> Unit,
    viewModel: RunningViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        viewModel.updateLocationPermission(locationPermissionState.allPermissionsGranted)

        if (locationPermissionState.allPermissionsGranted) {
            try {
                val location: Location? = fusedLocationClient.lastLocation.await()
                viewModel.updateCurrentLocation(
                    location?.let { LatLng(it.latitude, it.longitude) }
                )
            } catch (_: SecurityException) {
            }
        }
    }

    RunningContent(
        onMenuClick = onMenuClick,
        onRunClick = {},
        onCompleteClick = { viewModel.stopAndSave() },
        uiState = uiState
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun RunningContent(
    onMenuClick: () -> Unit,
    onRunClick: () -> Unit,
    onCompleteClick: () -> Unit,
    uiState: UserUiState
) {
    val defaultLocation = LatLng(35.8888, 128.6103)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: uiState.latLngList.lastOrNull() ?: defaultLocation,
            16f
        )
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = uiState.hasLocationPermission
    )

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 16f)
            )
        }
    }

    LaunchedEffect(uiState.latLngList.size) {
        if (uiState.latLngList.isNotEmpty()) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(uiState.latLngList.last())
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp)
        ) {
            MenuButton(onClick = onMenuClick)

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(color = White)
                        .MapSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true
                    )
                ) {
                    if (uiState.latLngList.size >= 2) {
                        Polyline(
                            points = uiState.latLngList,
                            color = Color.Blue,
                            width = 15f,
                            jointType = JointType.ROUND
                        )
                    }
                }

                BunIconButton(
                    onClick = onRunClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(180.dp)
                        .offset(y = 90.dp)
                )
            }

            Spacer(modifier = Modifier.MapSpaceSize())

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    StateBox(
                        texttop = "거리",
                        textbottom = "${uiState.totalDistance.toInt()}m"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StateBox(texttop = "페이스", textbottom = "6'49\"")
                }

                CenterBar()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    StateBox(texttop = "시간", textbottom = "11:36")
                    Spacer(modifier = Modifier.height(10.dp))
                    StateBox(texttop = "활동 칼로리", textbottom = "119kcal")
                }
            }

            OutlinedButton(
                onClick = onCompleteClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = White,
                    contentColor = TextBlack
                ),
                border = BorderStroke(2.dp, PointColor),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text("완료")
            }
        }
    }
}

@Composable
private fun BunIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.clip(CircleShape)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.yellow_shoes),
            contentDescription = "달리기 버튼",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StateBox(
    texttop: String,
    textbottom: String,
    fontsize: TextUnit = 25.sp,
    modifier: Modifier = Modifier.wrapContentSize()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            text = texttop,
            fontSize = fontsize,
            color = TextWhite
        )
        Text(
            text = textbottom,
            fontSize = fontsize,
            color = TextWhite
        )
    }
}