package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.runup.ui.theme.White

@Composable
fun ColumnScope.MenuButton (
    onClick:()->Unit,
    modifier: Modifier = Modifier.padding(top = 18.dp, bottom = 15.dp)
){
    Button(
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        onClick = onClick,
        modifier = modifier
            .height(35.dp)
            .width(40.dp)
            .align(Alignment.End)
    ){
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ){
            WhiteBar()
            WhiteBar()
            WhiteBar()
        }
    }
}

@Composable
private fun WhiteBar(){
    Box(
        Modifier
            .background(
                color = White,
                shape = RoundedCornerShape(5.dp)
            )
            .fillMaxWidth()
            .height(3.dp)
    ){}
}