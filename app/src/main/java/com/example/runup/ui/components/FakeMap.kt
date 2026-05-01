package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.runup.ui.theme.TextBlack
import com.example.runup.ui.theme.White

@Composable
fun FakeMap(
    modifier:Modifier = Modifier
){

    Box(
        modifier = modifier.background(White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Map Preview Placeholder",
            color = TextBlack
        )
    }
}