package com.example.runup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.runup.ui.components.MenuButton
import com.example.runup.ui.components.RunupTextfield
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.TextColor
import com.example.runup.viewmodel.GoalSettingUiState
import com.example.runup.viewmodel.GoalSettingViewModel
import com.example.runup.viewmodel.LoginUiState
import com.example.runup.viewmodel.LoginViewModel

@Composable
fun GoalSettingScreen(
    onMenuClick:()->Unit,
    viewModel: GoalSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalSettingContent(
        uiState = uiState,
        onMenuClick = onMenuClick
    )
}

@Preview
@Composable
fun PreviewGoalSettingContent(){
    GoalSettingContent(
        uiState = GoalSettingUiState(
            goalDistance = 3000,
            goalPace = 390
        ),
        {}
    )
}

@Composable
fun GoalSettingContent(
    uiState: GoalSettingUiState,
    onMenuClick:()->Unit
){

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuButton(onClick = onMenuClick)

            GoalSettingScreenText(
               text = "이번 달리기는",
                fontsize = 48.sp
            )
            GoalSettingScreenText(
                text = "목표 러닝 거리",
                fontsize = 32.sp
            )

            RunupTextfield(
                value = uiState.goalDistance.toString(),
                onValueChange = { },
                placeholderText = "거리 입력",
                modifier = Modifier
                    .height(52.dp)
                    .width(365.dp)
            )
            GoalSettingScreenText(
                text = "목표 1km 페이스",
                fontsize = 32.sp
            )
            RunupTextfield(
                value = uiState.goalPace.toString(),
                onValueChange = { },
                placeholderText = "거리 입력",
                modifier = Modifier
                    .height(52.dp)
                    .width(365.dp)
            )
            GoalSettingScreenText(
                text = "17분 25초 \n안에 들어와야 해요 ",
                fontsize = 40.sp
            )
        }
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
        color = TextColor,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}