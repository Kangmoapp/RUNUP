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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: String,
    comments: List<com.example.runup.domain.model.Comment>, // 댓글 리스트를 직접 받음
    onAddComment: (String) -> Unit,             // 댓글 달기 로직
    onDeleteComment: (String, String) -> Unit,  // 댓글 삭제 로직
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
                    CommentItem(comment, bitmapCache,onDelete = {
                        // 꾹 눌러서 삭제 확인 시 호출 🔹
                        onDeleteComment(postId, comment.commentId)
                    }) // 별도 분리된 댓글 아이템 컴포저블
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
    bitmapCache: Map<String, Bitmap>,
    onDelete: () -> Unit
) {
    // 🔹 캐시에서 프로필 비트맵 확인
    val authorProfileBitmap = bitmapCache[comment.authorProfileUrlMini]

    var showDeleteDialog by remember { mutableStateOf(false) }
    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    // 2. 내가 쓴 댓글인지 확인 🔹
    val isMyComment = remember(comment.authorId, myUid) {
        myUid != null && comment.authorId == myUid
    }

    if (showDeleteDialog) {
        Dialog(
            onDismissRequest = { showDeleteDialog = false },
            // 너비 제한을 해제해서 카드의 너비를 자유롭게 조절 🔹
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // ── 본체 카드 ──
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.82f) // 화면 너비의 82% 정도 사용 (담백하게)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)) // 깊은 다크
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // 중앙 정렬 🔹
                ) {
                    // ── [1] 아이콘 & 타이틀 ── 📍
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PointColor.copy(alpha = 0.1f), CircleShape), // 포인트 컬러 은은하게 배경
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever, // 삭제 전용 아이콘 사용 추천 🔹
                            contentDescription = null,
                            tint = PointColor, // 포인트 컬러로 강조
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "댓글 삭제",
                        color = WhiteTextColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold, // 아주 굵게
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── [2] 본문 (가독성 고려한 색상) ──
                    Text(
                        text = "정말로 이 댓글을 삭제하시겠습니까?\n이 동작은 되돌릴 수 없습니다.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 19.sp, // 줄간격 넓혀서 모던하게 🔹
                        textAlign = TextAlign.Center // 중앙 정렬
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── [3] 버튼 영역 (side-by-side) ── 📍
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp) // 버튼 사이 간격
                    ) {
                        // 취소 버튼 (담백하게 그레이)
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("취소", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }

                        // 삭제 버튼 (앱 컨셉인 PointColor 강조!) 🔹
                        Button(
                            onClick = {
                                onDelete()
                                showDeleteDialog = false
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PointColor, // 포인트 컬러 배경
                                contentColor = Color.Black // 포인트 컬러 위에는 검정 글씨 🔹
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // 살짝 입체감
                        ) {
                            Text("삭제하기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* 일반 클릭 */ },
                onLongClick = {
                    if (isMyComment) {
                        showDeleteDialog = true
                    }
                }
            )
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