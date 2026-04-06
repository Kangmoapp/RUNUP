package com.example.runup.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.runup.data.source.remote.community.CommunityDataSourceImpl
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Comment // 추가됨
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.usecase.GetUserRunningRecordUseCase
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 피드 목록 및 상세 보기 상태만 관리
data class CommunityUiState(
    val posts: List<Post> = emptyList(),
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true
)

data class PostUploadUiState(
    val runRecords: List<RunRecord> = emptyList(),
    val selectedRunRecord: RunRecord? = null,
    val isSheetOpen: Boolean = false,
    val isLoading: Boolean = false
)

private var lastVisibleSnapshot: DocumentSnapshot? = null
private var isLastPage = false

@HiltViewModel
class CommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: CommunityDataSourceImpl,
    private val getUserRunningRecordUseCase: GetUserRunningRecordUseCase
) : ViewModel() {

    // 커뮤니티 스크린 상태 관련
    private val _communityUiState = MutableStateFlow(CommunityUiState())
    val communityUiState: StateFlow<CommunityUiState> = _communityUiState.asStateFlow()

    // 포스트 업로드 스크린 상태 관련
    private val _postUploadUiState = MutableStateFlow(PostUploadUiState())
    val postUploadUiState: StateFlow<PostUploadUiState> = _postUploadUiState.asStateFlow()

    // 비트맵 캐시를 ViewModel에서 관리
    private val _bitmapCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val bitmapCache = _bitmapCache.asStateFlow()

    var selectedLocationImageUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var selectedCommonImageUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var savedScrollIndex by mutableStateOf(0)
        private set
    var savedScrollOffset by mutableStateOf(0)
        private set

    fun saveScrollState(index: Int, offset: Int) {
        savedScrollIndex = index
        savedScrollOffset = offset
    }


    fun fetchPosts(isInitial: Boolean = false, forceRefresh: Boolean = false) {
        // 처음이면서 post가 존재하는 상태이면 굳이 fetchPost를 실행하지 않음
        if (!forceRefresh && isInitial && _communityUiState.value.posts.isNotEmpty()) {
            return
        }

        // isLoading 상태거나, 처음이 아니면서 마지막페이지이면
        if (_communityUiState.value.isLoading || (isLastPage && !isInitial)) return



        if (isInitial) {
            lastVisibleSnapshot = null // 마지막 보던 곳
            isLastPage = false // 마지막 페이지 여부
            _communityUiState.update { it.copy(posts = emptyList(), isInitialLoading = true) } // 초기화 시 리스트 비우기
        }

        viewModelScope.launch {
            _communityUiState.update { it.copy(isLoading = true) }
            try {
                // 마지막 보던 곳에서 최대 3개 가져오기
                val result = dataSource.getPosts(lastVisibleSnapshot, 3L)

                if (result is AuthResult.Success) {
                    val (newPosts, lastSnapshot) = result.data
                    if (newPosts.isEmpty() || newPosts.size < 3) { // 새로 가져온 포스터가 3개 미만이면 마지막 페이지 여부 체크
                        isLastPage = true
                    }
                    lastVisibleSnapshot = lastSnapshot // 다음 호출을 위해 커서 업데이트

                    // 먼저 포스트 데이터만 업데이트 (아직 isInitialLoading은 true 유지)
                    _communityUiState.update { state ->
                        state.copy(posts = if (isInitial) newPosts else state.posts + newPosts)
                    }

                    // 비트맵 프리로딩 시작
                    preloadBitmaps(newPosts) {
                        // 프리로딩이 완료되면(혹은 상단 3개가 준비되면) 로딩 종료
                        if (isInitial) {
                            _communityUiState.update { it.copy(isInitialLoading = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _communityUiState.update { it.copy(isInitialLoading = false) }
            } finally {
                // 성공/실패 여부와 상관없이 로딩 종료
                _communityUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun preloadBitmaps(posts: List<Post>, onComplete: () -> Unit = {}) {
        if (posts.isEmpty()) {
            onComplete()
            return
        }

        var totalImages = posts.flatMap { it.locationImages }.size // 사진 개수
        if (totalImages == 0) { // 0개면 바로 로드
            onComplete()
            return
        }

        var loadedCount = 0

        posts.forEach { post ->
            post.locationImages.forEach { postImage ->
                viewModelScope.launch(Dispatchers.IO) {
                    val targetUrl = postImage.thumbnailUrl.ifEmpty { postImage.url } // 썸네일 url 가져오되, 없으면 원본 url
                    val request = ImageRequest.Builder(context)
                        .data(targetUrl).size(150, 150).allowHardware(false).build()

                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            _bitmapCache.update { it + (postImage.url to bitmap) } // url 주소와 비트맵을 함께 저장,
                        }
                    }

                    // 성공/실패 상관없이 카운트
                    loadedCount++
                    if (loadedCount >= totalImages) {
                        launch(Dispatchers.Main) { onComplete() }
                    }
                }
            }
        }
    }

    fun fetchPostDetail(postId: String) {
        viewModelScope.launch {
            _communityUiState.update { it.copy(
                isLoading = true,
                selectedPost = null,
                comments = emptyList()
            ) }
            val result = dataSource.getPostById(postId)
            if (result is AuthResult.Success) {
                _communityUiState.update { it.copy(selectedPost = result.data) }
            }
            _communityUiState.update { it.copy(isLoading = false) }
        }
    }

    // --- 추가된 부분: 실시간 댓글 감시 ---
    fun observeComments(postId: String) {
        viewModelScope.launch {
            dataSource.getCommentsFlow(postId).collect { commentList ->
                _communityUiState.update { it.copy(comments = commentList) }
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

    fun addSelectedLocationImages(newUris: List<Uri>) {
        val currentSize = selectedLocationImageUris.size
        val availableSpace = 4 - currentSize
        if (availableSpace > 0) {
            // 남은 공간만큼만 잘라서 추가 (예: 이미 2장 있는데 5장 선택하면 2장만 더 추가)
            val imagesToAdd = newUris.take(availableSpace)
            selectedLocationImageUris = (selectedLocationImageUris + imagesToAdd).distinct()
        }
    }

    fun removeLocationImage(uri: Uri) {
        selectedLocationImageUris = selectedLocationImageUris.filter { it != uri }
    }

    fun addSelectedCommonImages(newUris: List<Uri>) {
        selectedCommonImageUris = (selectedCommonImageUris + newUris).distinct()
    }

    fun removeCommonImage(uri: Uri) {
        selectedCommonImageUris = selectedCommonImageUris.filter { it != uri }
    }

    fun clearSelectedImages() {
        // 1. 이미지 리스트 초기화 (이건 별도 변수이므로 유지)
        selectedLocationImageUris = emptyList()
        selectedCommonImageUris = emptyList()

        // 2. 피드 상세 및 댓글 상태 통합 초기화
        _communityUiState.update { it.copy(
            selectedPost = null,
            comments = emptyList()
        ) }
    }

    fun uploadPost(content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 피드 상태의 로딩 시작
            _postUploadUiState.update { it.copy(isLoading = true) }

            val selectedRecord = _postUploadUiState.value.selectedRunRecord
            val result = dataSource.uploadPost(content, selectedLocationImageUris, selectedCommonImageUris,selectedRecord)

            if (result is AuthResult.Success) {
                clearSelectedImages() // 위에서 수정한 함수 호출
                // 2. 게시글 작성 상태(업로드용 uiState) 초기화
                _postUploadUiState.update { it.copy(selectedRunRecord = null) }
                onSuccess()
                fetchPosts() // 업로드 성공 후 목록 새로고침
            }
            // 3. 로딩 종료
            _postUploadUiState.update { it.copy(isLoading = false) }
        }
    }

    // CommunityViewModel.kt 의 onLikeClick 함수 수정
    fun onLikeClick(postId: String) {
        viewModelScope.launch {
            val result = dataSource.toggleLike(postId)
            if (result is AuthResult.Success) {
                val isLiked = result.data ?: false
                _communityUiState.update { state ->
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            val newLikes = if (isLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0)
                            post.copy(likes = newLikes)
                        } else post
                    }
                    val updatedSelectedPost = if (state.selectedPost?.postId == postId) {
                        val newLikes = if (isLiked) state.selectedPost.likes + 1 else (state.selectedPost.likes - 1).coerceAtLeast(0)
                        state.selectedPost.copy(likes = newLikes)
                    } else state.selectedPost
                    state.copy(posts = updatedPosts, selectedPost = updatedSelectedPost)
                }
            }
        }
    }

    // 3. 상태 업데이트 함수 (copy 활용)
    fun fetchMyRunRecords() {
        viewModelScope.launch {
            val result = getUserRunningRecordUseCase.invoke()
            if (result is AuthResult.Success) {
                _postUploadUiState.update { it.copy(runRecords = result.data) }
                Log.d("runrecord", "${_postUploadUiState.value.runRecords}")
            }
        }
    }

    fun setSheetOpen(isOpen: Boolean) {
        _postUploadUiState.update { it.copy(isSheetOpen = isOpen) }
    }

    fun selectRunRecord(record: RunRecord?) {
        _postUploadUiState.update { it.copy(selectedRunRecord = record, isSheetOpen = false) }
    }
}