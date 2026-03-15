package com.example.runup.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.data.source.remote.community.CommunityDataSourceImpl
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Comment // 추가됨
import com.example.runup.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val dataSource: CommunityDataSourceImpl
) : ViewModel() {

    var posts by mutableStateOf<List<Post>>(emptyList())
        private set

    var selectedPost by mutableStateOf<Post?>(null)
        private set

    // --- 추가된 부분: 댓글 상태 ---
    var comments by mutableStateOf<List<Comment>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var selectedImageUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    fun fetchPosts() {
        viewModelScope.launch {
            isLoading = true
            val result = dataSource.getPosts()
            if (result is AuthResult.Success) {
                posts = result.data ?: emptyList()
            }
            isLoading = false
        }
    }

    fun fetchPostDetail(postId: String) {
        viewModelScope.launch {
            isLoading = true
            selectedPost = null
            val result = dataSource.getPostById(postId)
            if (result is AuthResult.Success) {
                selectedPost = result.data
            }
            isLoading = false
        }
    }

    // --- 추가된 부분: 실시간 댓글 감시 ---
    fun observeComments(postId: String) {
        viewModelScope.launch {
            dataSource.getCommentsFlow(postId).collect {
                comments = it
            }
        }
    }

    // --- 추가된 부분: 댓글 추가 ---
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            val result = dataSource.addComment(postId, content)
            if (result is AuthResult.Success) {
                // 댓글 작성 후 상세 정보를 갱신하여 댓글 수 카운트 업데이트
                fetchPostDetail(postId)
            }
        }
    }

    fun addSelectedImages(newUris: List<Uri>) {
        selectedImageUris = (selectedImageUris + newUris).distinct()
    }

    fun removeImage(uri: Uri) {
        selectedImageUris = selectedImageUris.filter { it != uri }
    }

    fun clearSelectedImages() {
        selectedImageUris = emptyList()
        selectedPost = null
        comments = emptyList() // 댓글 목록도 초기화
    }

    fun uploadPost(content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = dataSource.uploadPost(content, selectedImageUris)
            if (result is AuthResult.Success) {
                clearSelectedImages()
                onSuccess()
            }
            isLoading = false
        }
    }

    // CommunityViewModel.kt 의 onLikeClick 함수 수정
    fun onLikeClick(postId: String) {
        viewModelScope.launch {
            val result = dataSource.toggleLike(postId)
            if (result is AuthResult.Success) {
                val isLiked = result.data ?: false

                // 1. 메인 리스트 상태 업데이트 (화면 유지하며 숫자만 변경)
                posts = posts.map { post ->
                    if (post.postId == postId) {
                        val newLikes = if (isLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0)
                        post.copy(likes = newLikes)
                    } else post
                }

                // 2. 상세 화면 상태 업데이트 (selectedPost가 null이 되지 않게 copy 사용)
                if (selectedPost?.postId == postId) {
                    val currentPost = selectedPost ?: return@launch
                    val newLikes = if (isLiked) currentPost.likes + 1 else (currentPost.likes - 1).coerceAtLeast(0)
                    selectedPost = currentPost.copy(likes = newLikes)
                }
            }
        }
    }
}