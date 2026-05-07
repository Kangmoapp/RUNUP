package com.runit.runup.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

// 클릭한 마커의 사진 원본 가져오기
@Composable
fun EnlargedImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
    displayBitmap: Bitmap? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        confirmButton = {},
        containerColor = Color.Black.copy(alpha = 0.9f),
        text = {
            Box(modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }) {
                if (displayBitmap != null) {
                    // [케이스 1] 이미 프리로드된 비트맵이 있다면 즉시 표시
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // [케이스 2] 아직 프리로드가 안 끝났다면 서버에서 로드
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    )
}