package com.example.runup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.runup.R

@Composable
fun AiCharacterIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.icon_character),
        contentDescription = "AI 캐릭터",
        modifier = modifier
            .size(72.dp) // 적당한 크기로 조절
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Fit
    )
}