package com.example.runup.ui.screens

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.viewmodel.StartViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.util.TableInfo
import com.example.runup.R
import com.example.runup.ui.components.PageIndicator
import com.example.runup.ui.theme.DarkGray
import com.example.runup.ui.theme.Gray
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
    )
}

@Composable
private fun StartContent(
    onLoginClick:()->Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleHorizontalPager()
            Image(
                painter = painterResource(R.drawable.google_light_sq_si),
                contentDescription = "Google Login",
                modifier = Modifier
                    .clickable { onLoginClick() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleHorizontalPager() {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount }) //

    Column(Modifier
        .fillMaxWidth()
        .wrapContentHeight()

        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 60.dp)
    ) {
        HorizontalPager(
            state = pagerState, //
            modifier = Modifier.height(600.dp).fillMaxWidth()
        ) { pageIndex ->
            when (pageIndex) {
                0 -> { page0() }
                1 -> { page1() }
                2 -> { page2() }
                3 -> { page3() }
            }
        }
        PageIndicator(pagerState, pageCount)
    }
}



@Preview
@Composable
private fun Previewpage0(){
    page0()
}

@Composable
private fun page0(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "달리기를 시작해\n 볼까요?")
    }
}

@Composable
private fun page1(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "튜토리얼 페이지 2")
    }
}

@Composable
private fun page2(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "튜토리얼 페이지 3")
    }
}

@Composable
private fun page3(){
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp)){
        StartText(text = "튜토리얼 페이지 4")
    }
}

@Composable
private fun StartText(
    text:String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = White,
        modifier = modifier
    )
}

@Preview
@Composable
private fun PreviewStartContent() {
    StartContent(
        {},
    )
}