package com.example.runup.ui.screens


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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.components.CenterBar
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.TopBar

import com.example.runup.ui.components.PaceGoalSettingDialog
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.MapSize
import com.example.runup.ui.theme.MapSpaceSize
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.TextWhite
import com.example.runup.viewmodel.HomeUiState
import com.example.runup.viewmodel.HomeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Preview
@Composable
fun PreviewHomeScreen(){

    HomeContent(
        uiState = HomeUiState(
        goalDistance = 2500,
        goalPace = 390),
        {},{},{},{},{},{},{},{ _, _ -> },
        )
}

@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onMenuClick = onMenuClick,
        onRunClick = onRunClick,
        onDistanceClick = {viewModel.openDistanceDialog()},
        onDistanceClose = {viewModel.closeDistanceDialog()},
        onDistanceConfirm = {viewModel.confirmDistance(it)},
        onPaceClick = {viewModel.openPaceDialog()},
        onPaceClose = {viewModel.closePaceDialog()},
        onPaceConfirm = { minute, second ->
            viewModel.confirmPace(minute, second)
        },
    )
}
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onMenuClick:()->Unit,
    onRunClick:()->Unit,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
    onPaceClick:()->Unit,
    onPaceClose:()->Unit,
    onPaceConfirm:(Int,Int)->Unit,
){
    val defaultLocation = LatLng(35.8888, 128.6103)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 17f)
    }
    val mapProperties = MapProperties(
        isMyLocationEnabled = true
    )

    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 17f)
            )
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            //modifier = Modifier.padding(start = 18.dp, end = 18.dp)
        ){
            TopBar(onMenuClick = onMenuClick, isBack = false)

            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp)
            ){
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
                )
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
                    .wrapContentSize()
                    .padding(top = 35.dp, start = 18.dp, end = 18.dp)
            ){
                HomeButton(
                    texttop = "목표 페이스",
                    textbottom = "${uiState.goalPace/60}분 ${uiState.goalPace%60}초",
                    onClick = onPaceClick,
                    modifier = Modifier.height(130.dp).weight(1f)
                )
                CenterBar()
                HomeButton(
                    texttop = "목표 거리",
                    textbottom = "${uiState.goalDistance.toDouble()/1000} km",
                    onClick = onDistanceClick,
                    modifier = Modifier.height(130.dp).weight(1f)
                )
            }
        }
        if (uiState.showDistanceDialog) {
            DistanceGoalSettingDialog(
                range = 0..100,
                startNumber = (uiState.goalDistance/100 + 1),
                onConfirm = onDistanceConfirm,
                onDismiss = onDistanceClose
            )
        }
        else if (uiState.showPaceDialog) {
            PaceGoalSettingDialog(
                rangeMinutes = 0..20,
                rangeSeconds = 0..60,
                startMinute = (uiState.goalPace/60 + 1),
                startSecond = (uiState.goalPace%60 + 1),
                onConfirm = onPaceConfirm,
                onDismiss = onPaceClose
            )
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