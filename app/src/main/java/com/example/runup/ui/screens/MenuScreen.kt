package com.example.runup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.TextWhite

@Preview
@Composable
fun PreviewMenuScreen(){
    MenuScreen({},{},{},{},{},{},{})
}

@Composable
fun MenuScreen(
    onBackClick:()->Unit,
    onCorseClick:()-> Unit,
    onGoalClick:()->Unit,
    onOptionClick:()->Unit,
    onHelpClick:()->Unit,
    onCommunityClick:()->Unit,
    onMypageClick:()->Unit,
){
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = BackGroudColor
    ) {
        Column(
        ){
            TopBar(onBackClick = onBackClick, isMenu = false)
            MenuText(text = "코스 추천",onClick = onCorseClick)
            MenuText(text = "목표 설정 화면",onClick = onGoalClick)
            MenuText(text = "설정",onClick = onOptionClick)
            MenuText(text = "도움말",onClick = onHelpClick)
            MenuText(text = "커뮤니티",onClick = onCommunityClick)
            MenuText(text = "마이페이지",onClick = onMypageClick)
        }
    }
}

@Composable
private fun MenuText(
    text:String,
    onClick:()-> Unit
){
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable { onClick() }
            .padding(bottom = 18.dp)
    ){
        Text(
            text = text,
            fontSize = 25.sp,
            color = TextWhite,
            modifier = Modifier
                .padding(start = 20.dp)
                .wrapContentSize()
        )
    }
}