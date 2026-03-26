package com.example.runup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.theme.White
import com.example.runup.ui.theme.WhiteTextColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.runup.R

@Composable
fun TopBar(
    text: String = "",
    onBackClick: () -> Unit = {},
    onMenuClick: ()->Unit = {},
    isBack: Boolean = true,
    isMenu: Boolean = true,
    insteadMenuComponent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(top = 30.dp, end = 18.dp, start = 18.dp)
    ){
        if(isBack){
            Box(
                modifier = Modifier.wrapContentSize().align(Alignment.CenterStart)
            ){
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = WhiteTextColor,
                    modifier = Modifier.size(30.dp).clickable{onBackClick()}
                )
            }

        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ){
            Text(
                text = text,
                color = WhiteTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Box(
            modifier = Modifier.wrapContentSize().align(Alignment.CenterEnd)
        ){
            if(isMenu){
                Icon(
                    Icons.Default.Menu,
                    "메뉴",
                    tint = WhiteTextColor,
                    modifier = Modifier.size(40.dp).clickable{onMenuClick()}
                )
            }
            else{
                insteadMenuComponent?.invoke()
            }
        }

    }
}

@Preview
@Composable
private fun PreviewTopBar(){
    TopBar(text = "예시")
}