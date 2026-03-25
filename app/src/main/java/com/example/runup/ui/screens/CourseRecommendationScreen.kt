package com.example.runup.ui.screens

import android.graphics.Point
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.components.MenuBar
import com.example.runup.ui.components.PageIndicator
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.DarkGray
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.MapSize
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextGray
import com.example.runup.ui.theme.TextWhite
import com.example.runup.ui.theme.White
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings

@Composable
fun CourseRecommendationScreen(
    onBackClick:()->Unit,
    onMenuClick:()->Unit
){
    CourseRecommendationContent(
        onBackClick = onBackClick,
        onMenuClick = onMenuClick
    )
}


@Composable
private fun CourseRecommendationContent(
    onBackClick:()->Unit,
    onMenuClick:()->Unit
){
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
        ){
            MenuBar(onBackClick = onBackClick, onMenuClick = onMenuClick)
            Box(
                modifier = Modifier
                    .height(550.dp)
                    .fillMaxWidth()
            ){
                MapHorizontalPager()

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp)
                ){
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(60.dp)
                            .width(220.dp)
                            .background(color = White,shape = RoundedCornerShape(10.dp))
                            .border(width = 3.dp, color = PointColor,shape = RoundedCornerShape(10.dp))
                    ){
                        Text(
                            text = "코스 추천 받기",
                            fontSize = 25.sp,
                        )

                    }
                }

            }

            Spacer(modifier = Modifier.height(25.dp))
            componentRecommend(text = "목표 러닝 거리", text2 = "3km")
            componentRecommend(text = "거리 계산 방법", text2 = "왕복")
            componentRecommend(text = "정렬 방법", text2 = "선택해주세요")
        }
    }
}

@Composable
private fun componentRecommend(
    text:String ="",
    text2:String = ""
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 22.sp,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(color = White, shape = RoundedCornerShape(5.dp))
                .weight(1f)
                .height(38.dp)
                .padding(end = 10.dp)
        ){
            Text(
                text = text2,
                fontSize = 22.sp,
                color = TextBlack,
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapHorizontalPager() {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount }) //

    Column(Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            CourseRecommendationGoogleMap(pageIndex)
        }
    }
}

@Composable
private fun CourseRecommendationGoogleMap(
    pageIndex:Int,
    isRecommendClick: Boolean = true
){
    Box(

    ){
        GoogleMap(
            modifier = Modifier
                .background(color = Gray)
                .fillMaxSize(),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        )
        if(isRecommendClick){
            Column(
                modifier = Modifier.padding(start = 18.dp, top = 18.dp)
            ){
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(30.dp)
                        .width(70.dp)
                        .background(color = White.copy(alpha = 0.5f), shape = RoundedCornerShape(5.dp))
                ){
                    Text(
                        text = "코스 ${pageIndex}",
                        color = TextGray,
                        fontSize = 20.sp
                    )
                }
            }

        }
    }
}

@Preview
@Composable
private fun PreviewCourseRecommendationContent(){
    CourseRecommendationContent({}, {})
}