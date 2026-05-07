package com.runit.runup.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.runit.runup.ui.theme.PointColor
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Composable
fun RunupLazyColumn(
    range: IntRange,
    startNumber: Int = 0,
    ItemHeight: Int = 56,
    VisibleItemsCount: Int = 3,
    textMapper: (Int) -> String,
    onSelectedNumberChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val adjustedStart = (startNumber-1 - range.first).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = adjustedStart)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(Unit) {
        android.util.Log.d("PICKER", "LaunchedEffect 실행됨")
        snapshotFlow { listState.layoutInfo.viewportSize.height }
            .filter { it > 0 }
            .first()
        listState.scrollToItem(adjustedStart)
        android.util.Log.d("PICKER", "scrollToItem 완료: ${listState.firstVisibleItemIndex}")
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = modifier.height((ItemHeight * VisibleItemsCount).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Box(modifier = Modifier.height(ItemHeight.dp)) } // 빈 칸

        items(range.count()) { index ->
            // 👇 isScrolling을 여기 안에서 직접 읽기 (리컴포즈 범위를 items 내부로 제한)
            val isScrolling = listState.isScrollInProgress
            val number = range.first + index
            val isGray = (index != listState.firstVisibleItemIndex || isScrolling)
            TextBox(text = textMapper(number), ItemHeight = ItemHeight.dp, isGray = isGray)
        }

        item { Box(modifier = Modifier.height(ItemHeight.dp)) } // 빈 칸
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val selected = (range.first + listState.firstVisibleItemIndex)
            .coerceIn(range.first, range.last)
        onSelectedNumberChange(selected)
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