package com.example.runup.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

// 비트맵 사진 마커 씌우는 컴포저블
@Composable
fun PhotoMarkerFromBitmap(bitmap: Bitmap) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 전체를 감싸는 검은색 프레임
        Box(
            modifier = Modifier
                .size(64.dp) // 크기를 살짝 조절
                .background(Color.Black, shape = MaterialTheme.shapes.small)
                .padding(3.dp) // 검은색 테두리 두께
        ) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall), // 사진 끝을 살짝만 굴림
                contentScale = ContentScale.Crop
            )
        }
    }
}