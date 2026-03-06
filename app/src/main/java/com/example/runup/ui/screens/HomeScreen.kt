package com.example.runup.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.R
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runup.ui.components.MenuButton
import com.example.runup.ui.theme.BackGroudColor

@Preview
@Composable
fun PreviewHomeScreen(){
    HomeScreen({},{})
}

@Composable
fun HomeScreen(
    onMenuClick:()->Unit,
    onRunClick:()->Unit
){
    /*
    val singapore = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
     */

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(start = 18.dp, end = 18.dp)

        ){
            MenuButton(onClick = onMenuClick)

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .width(360.dp)
                    .height(690.dp)
                    .padding(bottom = 90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .graphicsLayer { clip = false }
            ) {
                Image(
                    painter = painterResource(id = com.example.runup.R.drawable.ic_launcher_background), // 너 지도 이미지 리소스로 교체
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                BunIconButton(
                    onClick = onRunClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(180.dp)
                        .offset(y = 90.dp)
                )
            }

        }
        /*
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = singapore),
                title = "Singapore",
                snippet = "Marker in Singapore"
            )
        }

         */
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