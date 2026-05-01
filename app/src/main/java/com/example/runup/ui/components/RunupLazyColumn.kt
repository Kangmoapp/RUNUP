package com.example.runup.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.TextGray
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Composable
fun RunupLazyColumn(
    range: IntRange,
    startNumber: Int = 0,
    ItemHeight:Int = 56,
    VisibleItemsCount:Int = 3,
    textMapper: (Int) -> String,
    onSelectedNumberChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val adjustedStart =
        if (startNumber == 0) 0
        else startNumber - 1

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startNumber)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val isScrolling = listState.isScrollInProgress

    // ── 🔹 [핵심 추가] 초기 진입 시 위치 강제 고정 📍 ──
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .filter { it > 0 }
            .first()

        listState.scrollToItem(adjustedStart)
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = modifier.height((ItemHeight * VisibleItemsCount).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item{
            TextBox(text = "", ItemHeight = ItemHeight.dp)
        }
        items(range.count()) { index ->
            val number = range.first + index
            val textVal = textMapper(number)
            val isGray =
                (index != listState.firstVisibleItemIndex || isScrolling)
            TextBox(text = textVal, ItemHeight = ItemHeight.dp, isGray)
        }
        item{
            TextBox(text = "", ItemHeight = ItemHeight.dp)
        }
    }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val selected = (range.first + listState.firstVisibleItemIndex)
            .coerceIn(range.first, range.last)
        onSelectedNumberChange(selected)
        //selectedNumber = range.first + listState.firstVisibleItemIndex
    }
}

@Composable
private fun TextBox(
    text: String,
    ItemHeight: Dp,
    isGray: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ItemHeight),
        contentAlignment = Alignment.Center // 👈 'Center'로 변경하여 정중앙 정렬 📍
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            // ── 🔹 색상도 우리 앱의 포인트 컬러에 맞춰주면 더 예쁩니다 📍 ──
            color = if (isGray) Color.Gray else PointColor,
            fontWeight = if (isGray) FontWeight.Normal else FontWeight.Bold
        )
    }
}