package com.example.runup.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.runup.domain.model.SortType
import com.example.runup.ui.theme.Gray
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White

@Composable
fun LoopSelectionDialog(
    isLoop: Boolean,
    onSelect: (Boolean) -> Unit, // (isFirst: Boolean) -> Unit
    onDismiss: () -> Unit
) {
    // 1. 텍스트 설정
    val textLoopFirst = if (isLoop) "왕복" else "편도"
    val textLoopSecond = if (isLoop) "편도" else "왕복"

    // 2. 애니메이션 상태 관리 (팝업이 뜨자마자 펼쳐진 상태로 보이고 싶다면 true)
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    // 3. 실제 팝업창
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(15.dp),
            color = Color.Transparent // 배경을 투명하게 해서 기존 디자인 유지
        ) {
            Column(
                modifier = Modifier
                    .background(shape = RoundedCornerShape(5.dp), color = Color.Transparent)
            ) {
                val isExpanded = visibleState.currentState || visibleState.targetState

                // 첫 번째 아이템 (현재 선택된 상태 표시)
                val boxModifier = if (isExpanded) {
                    Modifier.background(shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp), color = White)
                } else {
                    Modifier.background(shape = RoundedCornerShape(5.dp), color = White)
                }

                InfoText(
                    text = textLoopFirst,
                    modifier = boxModifier
                        .fillMaxWidth()
                        .clickable {
                            // 현재 상태 그대로 선택하고 닫기
                            onSelect(true)
                            onDismiss()
                        }
                )

                // 애니메이션으로 펼쳐지는 두 번째 아이템
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .background(shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp), color = White)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(color = Gray)
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 반대 상태 선택하고 닫기
                                    onSelect(false)
                                    onDismiss()
                                }
                                .height(38.dp)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = textLoopSecond,
                                fontSize = 22.sp,
                                color = TextBlack,
                            )
                        }
                    }
                }
            }
        }
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
            .fillMaxWidth()
            .padding(end = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 22.sp,
            color = TextBlack,
        )
    }
}

@Composable
fun CategoryDialog(
    currentSort: SortType,
    onConfirm: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember(currentSort) { mutableStateOf(currentSort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedSort) },
                shape = RectangleShape,
            ) {
                Text(
                    text = "취소",
                    color = TextBlack)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                SortType.entries.forEach { sortType ->
                    Column (
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { onConfirm(sortType) }
                            .padding(start = 18.dp)
                    ){
                        Text(
                            text = sortType.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                }
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(15.dp),
    )
}