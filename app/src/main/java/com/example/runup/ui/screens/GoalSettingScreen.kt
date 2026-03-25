package com.example.runup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.components.DistanceGoalSettingDialog
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.components.PaceGoalSettingDialog
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.TextWhite
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.GoalSettingUiState
import com.example.runup.viewmodel.GoalSettingViewModel


@Preview
@Composable
fun PreviewGoalSettingContent(){
    GoalSettingContent(
        uiState = GoalSettingUiState(
            goalDistance = 2500,
            goalPace = 390
        ),
        {},{},{},{},{},{},{},{ _, _ -> },{ 10 to 10 }
    )
}

@Composable
fun GoalSettingScreen(
    onMenuClick:()->Unit,
    onBackClick:()->Unit,
    viewModel: GoalSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalSettingContent(
        uiState = uiState,
        onMenuClick = onMenuClick,
        onBackClick = onBackClick,
        onDistanceClick = {viewModel.openDistanceDialog()},
        onDistanceClose = {viewModel.closeDistanceDialog()},
        onDistanceConfirm = {viewModel.confirmDistance(it)},
        onPaceClick = {viewModel.openPaceDialog()},
        onPaceClose = {viewModel.closePaceDialog()},
        onPaceConfirm = { minute, second ->
            viewModel.confirmPace(minute, second)
        },
        onTimeCalculate = {viewModel.calculateTime()}
    )
}

@Composable
private fun GoalSettingContent(
    uiState: GoalSettingUiState,
    onMenuClick:()->Unit,
    onBackClick:()->Unit,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
    onPaceClick:()->Unit,
    onPaceClose:()->Unit,
    onPaceConfirm:(Int,Int)->Unit,
    onTimeCalculate:()->Pair<Int, Int>,
){
    val (minute, second) = onTimeCalculate()
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(onMenuClick = onMenuClick, onBackClick = onBackClick)

            GoalSettingScreenText(
               text = "이번 달리기는",
                fontsize = 48.sp,
                modifier = Modifier.padding(top=30.dp)
            )
            GoalSettingScreenText(
                text = "목표 러닝 거리",
                fontsize = 32.sp,
                modifier = Modifier.padding(top=15.dp)
            )
            ClickableText(
                text = "${uiState.goalDistance.toDouble()/1000} km",
                onClick = onDistanceClick,
                modifier = Modifier.padding(top=15.dp)
            )


            GoalSettingScreenText(
                text = "목표 1km 페이스",
                fontsize = 32.sp,
                modifier = Modifier.padding(top=15.dp)
            )
            ClickableText(
                text = "${uiState.goalPace/60}\' ${uiState.goalPace%60}\"",
                onClick = onPaceClick,
                modifier = Modifier.padding(top=15.dp)
            )

            GoalSettingScreenText(
                text = "${minute}분 ${second}초 \n안에 들어와야 해요 ",
                fontsize = 40.sp,
                modifier = Modifier.padding(top=15.dp)
            )
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
private fun ClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Box(
        modifier = modifier
            .size(width = 280.dp, height = 80.dp)
            .background(color = White, shape = RoundedCornerShape(8.dp))
            .clickable ( onClick = onClick ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 75.sp,
            color = Black
        )
    }
}


@Composable
private fun GoalSettingScreenText(
    text:String,
    fontsize:TextUnit,
    modifier: Modifier = Modifier
){
    Text(
        text= text,
        fontSize = fontsize,
        color = TextWhite,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}