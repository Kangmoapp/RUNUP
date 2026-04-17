package com.example.runup.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.mapper.TimeMapper
import com.example.runup.viewmodel.CommunityViewModel

@Composable
fun CommunityCommentScreen(
    postId: String,
    onBackClick: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    var commentText by remember { mutableStateOf("") }
    val uiState by viewModel.communityUiState.collectAsState()

    val post = uiState.selectedPost
    val comments = uiState.comments
    val isLoading = uiState.isLoading

    val bitmapCache by viewModel.thumbnailCache.collectAsState()

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
        topBar = { TopBar(onBackClick = onBackClick, text = "댓글", isMenu = false) },
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
                                    .background(Color.Gray), // 로딩 전 기본 배경
                                contentAlignment = Alignment.Center
                            ) {
                                val authorBitmap = bitmapCache[post.authorProfileUrl]
                                if (authorBitmap != null) {
                                    // [케이스 1] 목록 화면에서 이미 프리로드된 비트맵이 있다면 즉시 표시 (0초 로딩)
                                    Image(
                                        bitmap = authorBitmap.asImageBitmap(),
                                        contentDescription = "작성자 프로필",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (post.authorProfileUrl.isNotEmpty()) {
                                    // [케이스 2] 캐시에 없지만 URL은 있다면 AsyncImage로 로드
                                    AsyncImage(
                                        model = post.authorProfileUrl,
                                        contentDescription = "작성자 프로필",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // [케이스 3] URL 자체가 없는 경우 기본 아이콘
                                    Text("👤", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = post.authorName,
                                color = WhiteTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // 상호작용 및 본문 영역
                    item {
                        Column(modifier = Modifier.padding(8.dp)) {
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
                                    text = "${post.likes}",
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
                        val authorProfileUrlMiniBitmap = bitmapCache[comment.authorProfileUrlMini]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp), // 간격 살짝 조정
                            verticalAlignment = Alignment.Top // 프로필 사진은 위쪽에 맞춤
                        ) {
                            // 1. 프로필 사진 영역
                            Box(
                                modifier = Modifier
                                    .size(32.dp) // 댓글용은 조금 더 작게 (32dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (authorProfileUrlMiniBitmap != null) {
                                    Image(
                                        bitmap = authorProfileUrlMiniBitmap.asImageBitmap(),
                                        contentDescription = "댓글 작성자 프로필",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (!comment.authorProfileUrlMini.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = comment.authorProfileUrlMini,
                                        contentDescription = "댓글 작성자 프로필",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("👤", fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 2. 이름 및 내용 영역
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 이름
                                    Text(
                                        text = comment.authorName,
                                        color = WhiteTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.width(8.dp)) // 이름과 시간 사이 간격

                                    // 시간 표시 (방금 전, n분 전 등)
                                    Text(
                                        text = TimeMapper.formatTimeAgo(comment.timestamp),
                                        color = WhiteTextColor.copy(alpha = 0.5f), // 시간을 약간 흐릿하게 처리
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.content,
                                    color = WhiteTextColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            } else {
                // 데이터를 찾을 수 없는 경우
                Text(
                    text = "댓글이 없습니다.",
                    color = WhiteTextColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}