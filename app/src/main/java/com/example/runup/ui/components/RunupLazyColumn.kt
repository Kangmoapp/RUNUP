package com.example.runup.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.BlackTextColor
import com.example.runup.ui.theme.GrayTextColor

@Composable
fun RunupLazyColumn(
    range: IntRange,
    startNumber: Int = 0,
    ItemHeight:Int = 56,
    VisibleItemsCount:Int = 3,
    textMapper: (Int) -> String,
    onSelectedNumberChange: (Int) -> Unit
) {
    val startNumber =
        if(startNumber == 0) 0
        else startNumber - 1

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startNumber)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val isScrolling = listState.isScrollInProgress

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = Modifier.height((ItemHeight * VisibleItemsCount).dp),
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
            .wrapContentWidth()
            .height(ItemHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            color = if (isGray) GrayTextColor else BlackTextColor
        )
    }
}