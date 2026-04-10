package com.example.runup.ui.screens

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CourseRecommendationViewModel
import com.example.runup.viewmodel.HomeUiState
import com.example.runup.viewmodel.HomeViewModel
import com.naver.maps.map.MapView
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.geometry.LatLng
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker

@Preview
@Composable
private fun Preview_HomeContent(){
    HomeContent(uiState = HomeUiState(),{})
}


@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    viewModel: HomeViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        uiState = uiState,
        onMenuClick = onMenuClick
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onMenuClick:()->Unit,
){
    val isPreview = LocalInspectionMode.current

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            if(isPreview){
                FakeMap(modifier = Modifier.fillMaxSize())
            }
            else{
                uiState.currentLocation?.let { geoPoint ->
                    MapViewContainer(
                        cameraPosition = LatLng(geoPoint.latitude, geoPoint.longitude),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize()
                    .padding(top = 35.dp, end = 18.dp)
            ){
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(color = BackGroudColor,shape = RoundedCornerShape(5.dp))
                        .clickable{onMenuClick()},
                    contentAlignment = Alignment.Center
                ){
                    Icon(
                        Icons.Default.Menu,
                        "메뉴",
                        tint = WhiteTextColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

    }
}


@Composable
private fun MapViewContainer(
    cameraPosition:LatLng,
    modifier:Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val marker = remember { Marker() }

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
        }
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { naverMap: NaverMap ->
                naverMap.moveCamera(
                    CameraUpdate.toCameraPosition(
                        CameraPosition(cameraPosition, 16.0)
                    ).animate(CameraAnimation.Easing, 1200)
                )

                marker.position = cameraPosition
                marker.captionText = "현재 위치"
                marker.map = naverMap
            }
        }
    )
    /*
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { naverMap ->
                    naverMap.moveCamera(
                        CameraUpdate.toCameraPosition(
                            com.naver.maps.map.CameraPosition(cameraPosition, 16.0)
                        ).animate(CameraAnimation.Easing, 1200)
                    )
                    marker.position = cameraPosition
                    marker.captionText = "현재 위치"
                    marker.map = naverMap
                }
            }
        },
        modifier = modifier
    )

     */
}
@Composable
private fun FakeMap(
    modifier:Modifier = Modifier
){

    Box(
        modifier = modifier.background(White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Map Preview Placeholder",
            color = TextBlack
        )
    }
}