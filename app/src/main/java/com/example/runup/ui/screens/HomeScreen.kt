package com.example.runup.ui.screens


import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.runup.ui.components.CenterBar

import com.example.runup.ui.components.MenuButton
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.MapSize
import com.example.runup.ui.theme.MapSpaceSize
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.TextWhite

import com.example.runup.viewmodel.GoalSettingViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@Preview
@Composable
fun PreviewHomeScreen(){
    HomeContent({},{},{})
}

@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    onDistanceClick:()-> Unit,
    viewModel: GoalSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    /* 지도 화면을 이전 위치로 옮겨주는 코드
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    LaunchedEffect(Unit) {
        try {
            val location: Location? = fusedLocationClient.lastLocation.await()
            viewModel.updateCurrentLocation(
                location?.let { LatLng(it.latitude, it.longitude) }
            )
        } catch (_: SecurityException) {
        }
    }
    
     */

    HomeContent(
        onMenuClick = onMenuClick,
        onRunClick = onRunClick,
        onDistanceClick = onDistanceClick

    )
}
@Composable
private fun HomeContent(
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    onDistanceClick:()-> Unit
){
    val singapore = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            MenuButton(onClick = onMenuClick)

            Box(
                modifier = Modifier.fillMaxWidth()
            ){
                GoogleMap(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(color = White)  // ui 확인용
                        .MapSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = singapore),
                        title = "Singapore",
                        snippet = "Marker in Singapore"
                    )
                }
                BunIconButton(
                    onClick = onRunClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(180.dp)
                        .offset(y=90.dp)
                )
            }
            Spacer(modifier = Modifier.MapSpaceSize())

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ){
                HomeButton(
                    texttop = "목표 페이스",
                    textbottom = "6분 30초",
                    {},
                    modifier = Modifier.height(130.dp).weight(1f)
                )
                CenterBar()
                HomeButton(
                    texttop = "목표 거리",
                    textbottom = "3km",
                    onClick = onDistanceClick,
                    modifier = Modifier.height(130.dp).weight(1f)
                )
            }

        }
    }
}

@Composable
private fun BunIconButton(
    onClick:()->Unit,
    modifier: Modifier = Modifier
){
    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape) // 원형으로 자르기
    ){
        Icon(
            painter = painterResource(id = com.example.runup.R.drawable.yellow_shoes),
            contentDescription = "달리기 버튼",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
fun PreviewBtn(){
    Box(
        modifier = Modifier
            .height(130.dp)
            .width(180.dp)
    ){
        HomeButton(
            texttop = "목표 페이스",
            textbottom = "6분 30초",
            {},
        )
    }
}

@Composable
private fun HomeButton(
    texttop: String,
    textbottom:String,
    onClick:()->Unit,
    fontsize: TextUnit = 25.sp,
    modifier:Modifier = Modifier
        .fillMaxSize()
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
    ){
        Text(
            text = texttop,
            fontSize = fontsize,
            color = TextWhite,
            modifier = Modifier
        )
        Text(
            text = textbottom,
            fontSize = fontsize,
            color = TextWhite,
            modifier = Modifier
        )
    }
}