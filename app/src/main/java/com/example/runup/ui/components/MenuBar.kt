package com.example.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor

@Composable
fun MenuBar (
    text:String = "",
    onMenuClick:()->Unit = {},
    onBackClick:()->Unit = {},
    isBack: Boolean = true,
    isMenu: Boolean = true
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(top = 30.dp, end = 18.dp, start = 18.dp)
    ){
        if(isBack){
            Button(
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                onClick = onBackClick,
                modifier = Modifier
                    .height(35.dp)
                    .width(40.dp)
            ){
                Text(text = "Back")
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = text,
            color = WhiteTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if(isMenu){
            Button(
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                onClick = onMenuClick,
                modifier = Modifier
                    .height(35.dp)
                    .width(40.dp)
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

@Preview
@Composable
private fun PreviewMenuBar(){
    MenuBar(text = "예시")
}