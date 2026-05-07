package com.runit.runup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runit.runup.ui.theme.BackGroudColor
import com.runit.runup.ui.theme.WhiteTextColor

@Composable
fun MenuBtn(    // 우측 상단 메튜 버튼
    modifier:Modifier = Modifier
        .padding(top = 35.dp, end = 18.dp),
    onMenuClick:()->Unit
){
    Box(
        modifier = modifier
            .wrapContentSize()
    ){
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color = BackGroudColor, shape = RoundedCornerShape(5.dp))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ){
            Icon(
                Icons.Default.Menu,
                "메뉴",
                tint = WhiteTextColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}