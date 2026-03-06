package com.example.runup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun NumberPickerDialog(
    range: IntRange,
    startNumber: Int = 0,
    textMapper: (Int) -> String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val startNumber =
        if(startNumber == 0) 0
        else startNumber - 1

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startNumber)
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
                    item{
                        textBox(text = "")
                    }
                    items(range.count()) { index ->
                        val number = range.first + index
                        val textVal = textMapper(number)
                        textBox(text = textVal)
                    }
                    item{
                        textBox(text = "")
                    }
                }
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    selectedNumber = range.first + listState.firstVisibleItemIndex
                }
            }
        }
    )
}

@Composable
private fun textBox(text:String){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 10.dp, bottom = 10.dp)
    ){
        Text(
            text = text,
            fontSize = 30.sp,
        )
    }

}