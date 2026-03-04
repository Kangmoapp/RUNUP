package com.example.runup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.runup.ui.components.MenuButton
import com.example.runup.ui.components.RunupTextfield
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.TextColor
import com.example.runup.ui.theme.White
import com.example.runup.viewmodel.GoalSettingUiState
import com.example.runup.viewmodel.GoalSettingViewModel
import com.example.runup.viewmodel.LoginUiState
import com.example.runup.viewmodel.LoginViewModel
import kotlinx.coroutines.launch


@Preview
@Composable
fun PreviewGoalSettingContent(){
    GoalSettingContent(
        uiState = GoalSettingUiState(
            goalDistance = 2500,
            goalPace = 390
        ),
        {},{},{},{}
    )
}

@Composable
fun GoalSettingScreen(
    onMenuClick:()->Unit,
    viewModel: GoalSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalSettingContent(
        uiState = uiState,
        onMenuClick = onMenuClick,
        onDistanceClick = {viewModel.openDistanceDialog()},
        onDistanceClose = {viewModel.closeDistanceDialog()},
        onDistanceConfirm = {viewModel.confirmDistance(it)},
    )
}

@Composable
fun GoalSettingContent(
    uiState: GoalSettingUiState,
    onMenuClick:()->Unit,
    onDistanceClick:()->Unit,
    onDistanceClose:()->Unit,
    onDistanceConfirm:(Int)->Unit,
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
            ClickableText(text = "${uiState.goalDistance.toDouble()/1000} km", onClick = onDistanceClick)


            GoalSettingScreenText(
                text = "목표 1km 페이스",
                fontsize = 32.sp
            )
            //ClickableText(text = "6\'30\"", onClick = {showDistanceDialog = true})

            GoalSettingScreenText(
                text = "17분 25초 \n안에 들어와야 해요 ",
                fontsize = 40.sp
            )
        }
        if (uiState.showDistanceDialog) {
            NumberPickerDialog(
                range = 0..100,
                onConfirm = onDistanceConfirm,
                onDismiss = onDistanceClose
            )
        }
    }
}

@Composable
private fun ClickableText(
    text: String,
    onClick: () -> Unit
){
    Box(
        modifier = Modifier
            .size(width = 265.dp, height = 80.dp)
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
fun NumberPickerDialog(
    range: IntRange,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var selectedNumber by remember { mutableStateOf(range.first) }
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedNumber) },
                shape = RectangleShape
            ) {
                Text("확인")
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .height(150.dp)
            ) {

                LazyColumn(
                    state = listState,
                    flingBehavior = snapFlingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(range.count()) { index ->
                        val number = range.first + index

                        Text(
                            text = (number.toDouble()/10).toString(),
                            fontSize = 30.sp,
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                        )
                    }
                }
                // 현재 중앙값 계산
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    selectedNumber = range.first + listState.firstVisibleItemIndex+1
                }
            }
        }
    )
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

@Preview
@Composable
fun PreviewDistanceScrollBox(){
    NumberPickerDialog(
        range = 0..100,
        onConfirm = {},
        onDismiss = {}
    )
}