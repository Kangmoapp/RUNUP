package com.example.runup.ui.screens

import android.app.Dialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.domain.model.SortType
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextGray
import com.example.runup.ui.theme.TextWhite
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.CourseRecommendationUiState
import com.example.runup.viewmodel.CourseRecommendationViewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings

@Composable
fun CourseRecommendationScreen(
    onBackClick:()->Unit,
    onMenuClick:()->Unit,
    viewModel: CourseRecommendationViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()

    CourseRecommendationContent(
        uiState = uiState,
        onDistanceClick = {viewModel.openDistanceDialog()},
        onDistanceClose = {viewModel.closeDistanceDialog()},
        onDistanceConfirm = {viewModel.confirmDistance(it)},
        onSortClick = {viewModel.openSortDialog()},
        onSortClose = {viewModel.closeSortDialog()},
        onSortConfirm = { sort ->
            viewModel.confirmSort(sort)
        },
        openLoopDialog = {viewModel.openLoopDialog()},
        loopSelect = {viewModel.loopSelect()},
        onBackClick = onBackClick,
        onMenuClick = onMenuClick,
        onSearchClick = {viewModel.onSearchClick()},
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUiState()
        }
    }
}


@Composable
private fun CourseRecommendationContent(
    uiState: CourseRecommendationUiState,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
    onSortClick:()->Unit,
    onSortClose:()->Unit,
    onSortConfirm:(SortType)->Unit,
    openLoopDialog:()->Unit,
    loopSelect:(Boolean)->Unit,
    onBackClick:()->Unit,
    onMenuClick:()->Unit,
    onSearchClick:()->Unit,
){
    val textLoopFirst :String = if(uiState.isLoop) "왕복" else "편도"
    val textLoopSecond :String = if(uiState.isLoop) "편도" else "왕복"

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
        ){
            Box(){

            }
            TopBar(onBackClick = onBackClick, onMenuClick = onMenuClick)
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
                        .clickable{onSearchClick()}
                ){
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(60.dp)
                            .width(220.dp)
                            .background(color = White, shape = RoundedCornerShape(10.dp))
                            .border(
                                width = 3.dp,
                                color = PointColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ){
                        Text(
                            text = "코스 추천 받기",
                            fontSize = 25.sp,
                        )

                    }
                }

            }

            Spacer(modifier = Modifier.height(25.dp))
            componentRecommend(text = "목표 러닝 거리", textInfo = "${uiState.goalDistance}km", onDistanceClick)
            if(uiState.showLoop){
                LoopDialog(
                    textLoopFirst = textLoopFirst,
                    textLoopSecond = textLoopSecond,
                    loopSelect = loopSelect
                )
            }
            else{
                componentRecommend(text = "거리 계산 방법", textInfo = textLoopFirst, openLoopDialog)
                componentRecommend(text = "정렬 방법", textInfo = "${uiState.currentSort.label}", onSortClick)
            }
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
}



@Composable
private fun LoopDialog(
    textLoopFirst:String,
    textLoopSecond:String,
    loopSelect:(Boolean)->Unit
){
    Row(
        modifier = Modifier
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ){
        Text(
            text = "거리 계산 방법",
            fontSize = 22.sp,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .background(color = White, shape = RoundedCornerShape(5.dp))
        ){
            InfoText(
                text = textLoopFirst,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable{loopSelect(true)}
            )
            Spacer(
                modifier = Modifier.fillMaxWidth().height(1 .dp).background(color = Gray)
            )
            InfoText(
                text = textLoopSecond,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable{loopSelect(false)}
            )
        }
    }
}

@Composable
private fun componentRecommend(
    text:String ="",
    textInfo:String = "",
    onClick:()->Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
            .clickable { onClick()}
    ){
        Text(
            text = text,
            fontSize = 22.sp,
            color = TextWhite,
            modifier = Modifier
                .weight(1f)
        )
        InfoText(text = textInfo,
            modifier = Modifier
                .weight(1f)
                .background(color = White, shape = RoundedCornerShape(5.dp))
        )
    }
}
@Composable
private fun InfoText(
    text:String,
    modifier:Modifier = Modifier
){
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .padding(end = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 22.sp,
            color = TextBlack,
        )
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
                        .background(
                            color = White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(5.dp)
                        )
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
    CourseRecommendationContent(
        uiState = CourseRecommendationUiState(),
        {},{},{},{}, {},{},{},{},{},{},{},
    )
}