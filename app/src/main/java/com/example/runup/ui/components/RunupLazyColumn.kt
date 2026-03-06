package com.example.runup.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.BlackTextColor
import com.example.runup.ui.theme.GrayTextColor

@Composable
fun RunupLazyColumn(
    range: IntRange,
    startNumber: Int = 0,
    textMapper: (Int) -> String,
    onSelectedNumberChange: (Int) -> Unit,
    modifier:Modifier = Modifier.fillMaxSize()
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item{
            textBox(text = "", modifier = Modifier.padding(top = 20.dp))
        }
        items(range.count()) { index ->
            val number = range.first + index
            val textVal = textMapper(number)
            val isGray =
                (index != listState.firstVisibleItemIndex || isScrolling)
            textBox(text = textVal, isGray)
        }
        item{
            textBox(text = "", modifier = Modifier.padding(bottom = 20.dp))
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
private fun textBox(text:String, isGray: Boolean = false, modifier:Modifier = Modifier.wrapContentSize()){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 10.dp, bottom = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 30.sp,
            color = if (isGray) GrayTextColor else BlackTextColor
        )
    }

}