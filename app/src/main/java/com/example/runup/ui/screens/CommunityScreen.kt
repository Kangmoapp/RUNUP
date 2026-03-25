package com.example.runup.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.runup.domain.model.Post
import com.example.runup.ui.components.MenuBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel

@Composable
fun CommunityScreen(
    onBackClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchPosts()
    }

    BackHandler { onBackClick() }

    Scaffold(
        containerColor = BackGroudColor,
        /*
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column { BackButton(onClick = onBackClick) }
                Text(
                    text = "커뮤니티",
                    color = WhiteTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onUploadClick) {
                    Icon(Icons.Default.Add, "글쓰기", tint = WhiteTextColor, modifier = Modifier.size(28.dp))
                }
            }
        }

         */
        topBar = { MenuBar(onBackClick = onBackClick, text = "게시물", isMenu = false) },
    ) { padding ->
        val posts = viewModel.posts

        if (viewModel.isLoading && posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        onClick = { onPostClick(post.postId) },
                        onLikeClick = { viewModel.onLikeClick(post.postId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Gray))
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreVert, null, tint = WhiteTextColor)
        }

        if (post.images.isNotEmpty()) {
            AsyncImage(
                model = post.images.first(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentScale = ContentScale.Crop
            )
        }

        // 인터랙션 바: 하트 색상 및 댓글 개수 반영
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onLikeClick() }) {
                Icon(
                    imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.likes > 0) Color.Red else WhiteTextColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 댓글 아이콘 및 개수
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💬", fontSize = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
                if (post.commentCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text("좋아요 ${post.likes}개", color = WhiteTextColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.content, color = WhiteTextColor)
            }
            // 댓글 모두 보기 텍스트 삭제됨
        }
    }
}