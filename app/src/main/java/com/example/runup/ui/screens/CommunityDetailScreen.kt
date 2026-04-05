package com.example.runup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel

@Composable
fun CommunityDetailScreen(
    postId: String,
    onBackClick: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    var commentText by remember { mutableStateOf("") }
    val uiState by viewModel.communityUiState.collectAsState()
    val post = uiState.selectedPost
    val comments = uiState.comments
    val isLoading = uiState.isLoading

    // 스크롤 상태 기억 (데이터가 변경되어도 위치 유지)
    val listState = rememberLazyListState()

    LaunchedEffect(postId) {
        if (postId.isNotEmpty()) {
            viewModel.fetchPostDetail(postId)
            viewModel.observeComments(postId)
        }
    }

    Scaffold(
        containerColor = BackGroudColor,
        /*
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column { BackButton(onClick = onBackClick) }
                Text(
                    text = "게시물",
                    color = WhiteTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },

         */
        topBar = { TopBar(onBackClick = onBackClick, text = "게시물", isMenu = false) },
        bottomBar = {
            Surface(
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
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
                            viewModel.addComment(postId, commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Text(
                            text = "게시",
                            color = if (commentText.isNotBlank()) Color(0xFF4A90E2) else Color.Gray
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // post가 아예 없는 초기 로딩 상황에서만 로딩 바 표시
            if (isLoading && post == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else if (post != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState // 스크롤 상태 유지
                ) {
                    // 작성자 정보
                    item {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = post.authorName,
                                color = WhiteTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 이미지 영역
                    items(post.locationImages) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    // 상호작용 및 본문 영역
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 좋아요 버튼 (토글 반영)
                                IconButton(
                                    onClick = { viewModel.onLikeClick(post.postId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (post.likes > 0) Color.Red else WhiteTextColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "좋아요 ${post.likes}개",
                                    color = WhiteTextColor,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                // 댓글 아이콘 및 개수 표시
                                Text(text = "💬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${comments.size}",
                                    color = WhiteTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post.content,
                                color = WhiteTextColor,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.DarkGray, thickness = 0.5.dp)
                        }
                    }

                    // 댓글 리스트 제목
                    item {
                        Text(
                            text = "댓글 ${comments.size}개",
                            color = WhiteTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 실시간 댓글 목록
                    items(comments) { comment ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = comment.authorName,
                                color = WhiteTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = comment.content,
                                color = WhiteTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            } else {
                // 데이터를 찾을 수 없는 경우
                Text(
                    text = "게시물을 찾을 수 없습니다.",
                    color = WhiteTextColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}