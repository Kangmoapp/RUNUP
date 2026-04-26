package com.example.runup.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.runup.ui.theme.BackGroudColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    comments: List<com.example.runup.domain.model.Comment>, // 🔹 리스트를 직접 받음
    onAddComment: (String) -> Unit,                       // 🔹 댓글 달기 로직을 주입받음
    bitmapCache: Map<String, Bitmap>,
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    androidx.activity.compose.BackHandler(enabled = true) {
        if (isKeyboardOpen) {
            // 키보드가 떠 있으면 포커스를 해제해 키보드만 내림 🔹
            focusManager.clearFocus()
        } else {
            // 키보드가 없으면 부모에게 시트를 닫으라고 신호 보냄
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.85f) // 화면의 85% 정도 높이
            .fillMaxWidth()
            .background(BackGroudColor)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── [1] 헤더 영역 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("댓글", color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Divider(color = Color.DarkGray, thickness = 0.5.dp)

        // ── [2] 댓글 리스트 영역 ──
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (comments.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("아직 댓글이 없습니다. 첫 댓글을 남겨보세요!", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(comments) { comment ->
                    CommentItem(comment, bitmapCache) // 별도 분리된 댓글 아이템 컴포저블
                }
            }
        }

        // ── [3] 댓글 입력 영역 ──
        Surface(
            color = Color(0xFF1A1A1A),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // 키보드 위로 자동으로 밀려 올라감
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("댓글 달기...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                                focusManager.clearFocus() // 전송 후 키보드 내리고 싶다면 추가
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = WhiteTextColor,
                        unfocusedTextColor = WhiteTextColor,
                        cursorColor = WhiteTextColor
                    )
                )
                TextButton(
                    onClick = {
                        onAddComment(commentText) // 🔹 주입받은 함수 실행
                        commentText = ""
                        focusManager.clearFocus() // 전송 후 키보드 내리고 싶다면 추가
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Text("게시", color = if (commentText.isNotBlank()) PointColor else Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: com.example.runup.domain.model.Comment, // 윤석님의 댓글 모델
    bitmapCache: Map<String, Bitmap>
) {
    // 🔹 캐시에서 프로필 비트맵 확인
    val authorProfileBitmap = bitmapCache[comment.authorProfileUrlMini]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. 프로필 이미지
        Box(
            modifier = Modifier
                .size(34.dp) // 댓글용으로 적당히 작은 사이즈
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (authorProfileBitmap != null) {
                Image(
                    bitmap = authorProfileBitmap.asImageBitmap(),
                    contentDescription = "프로필",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (!comment.authorProfileUrlMini.isNullOrEmpty()) {
                AsyncImage(
                    model = comment.authorProfileUrlMini,
                    contentDescription = "프로필",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("👤", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. 이름, 시간, 본문 내용
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 이름
                Text(
                    text = comment.authorName,
                    color = WhiteTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 시간 (TimeMapper 활용)
                Text(
                    text = com.example.runup.ui.util.mapper.TimeMapper.formatTimeAgo(comment.timestamp),
                    color = WhiteTextColor.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 댓글 본문
            Text(
                text = comment.content,
                color = WhiteTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}