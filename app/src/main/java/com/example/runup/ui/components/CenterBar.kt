package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runup.ui.theme.White

@Composable
fun CenterBar(){
    Box(
        modifier = Modifier
            .background(color = White, shape = RoundedCornerShape(20 .dp))
            .width(4.dp)
            .height(95.dp)
    )
}