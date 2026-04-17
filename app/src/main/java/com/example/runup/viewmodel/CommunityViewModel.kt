package com.example.runup.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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
import com.example.runup.ui.util.UserStateManager
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
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

data class PostUploadUiState(
    val runRecords: List<RunRecord> = emptyList(),
    val selectedRunRecord: RunRecord? = null,
    val isSheetOpen: Boolean = false,
    val isLoading: Boolean = false
)

data class MapSnapshot(
    val staticMapUrl: String,
    val markerPositions: Map<String, Offset>, // 실제 좌표(점)
    val closerOffsets: Map<String, Offset>,   // 마커의 최종 위치
    val courseBounds: androidx.compose.ui.geometry.Rect // 코스 경계 픽셀
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

    var selectedLocationImageUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var selectedCommonImageUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var savedScrollIndex by mutableStateOf(0)
        private set
    var savedScrollOffset by mutableStateOf(0)
        private set

    // [A] 지도 전용 캐시
    private val _mapSnapshotCache = MutableStateFlow<Map<String, MapSnapshot>>(emptyMap())
    val mapSnapshotCache = _mapSnapshotCache.asStateFlow()

    // [B] 리스트 마커용 썸네일 캐시 (URL 기준) - 기존 _bitmapCache를 마커 전용으로 명시
    private val _thumbnailCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnailCache = _thumbnailCache.asStateFlow()

    // [C] 원본/페이지 이동용 고해상도 캐시 (URL 기준)
    private val _fullBitmapCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val fullBitmapCache = _fullBitmapCache.asStateFlow()

    // 데이터 저장 함수들
    fun saveMapSnapshot(postId: String, snapshot: MapSnapshot) {
        _mapSnapshotCache.update { it + (postId to snapshot) }
    }

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
            _communityUiState.update { it.copy(
                posts = emptyList(),
                isInitialLoading = !forceRefresh, // 새로고침일 때는 전체화면 로딩(중앙 아이콘) 안 띄움
                isRefreshing = forceRefresh       // 새로고침일 때만 상단 당기기 인디케이터 활성화
            ) } // 초기화 시 리스트 비우기
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
                            _communityUiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _communityUiState.update { it.copy(isInitialLoading = false) }
            } finally {
                // 성공/실패 여부와 상관없이 로딩 종료
                _communityUiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun preloadBitmaps(posts: List<Post>, onComplete: () -> Unit = {}) {
        if (posts.isEmpty()) {
            onComplete()
            return
        }

        val locationImages = posts.flatMap { it.locationImages }
        val commonImages = posts.flatMap { it.commonImages }

        // 1. 게시글 작성자 프로필 URL 추출
        val postAuthorUrls = posts.map { it.authorProfileUrl }

        // 2. 모든 게시글의 댓글 작성자 프로필 URL 추출 (중첩 리스트 풀기)
        val commentAuthorUrls = posts.flatMap { post ->
            post.comments.map { it.authorProfileUrlMini }
        }

        // 3. 모든 프로필 URL 합치기 + 비어있는 값 제거 + 중복 제거
        val allProfileUrls = (postAuthorUrls + commentAuthorUrls)
            .filter { it.isNotEmpty() }
            .distinct()

        // 1. 필수 로딩 (마커 썸네일) - 얘네가 다 돼야 onComplete를 부름
        var essentialLoadedCount = 0
        val totalEssential = locationImages.size

        if (totalEssential == 0) {
            onComplete()
        } else {
            locationImages.forEach { postImage ->
                viewModelScope.launch(Dispatchers.IO) {
                    // 마커용 썸네일 (150px) 캐싱
                    val targetUrl = postImage.thumbnailUrl.ifEmpty { postImage.url }
                    preloadToStateManager(targetUrl, postImage.url, 150, "THUMB")

                    // UI 스레드에서 카운트 체크
                    launch(Dispatchers.Main) {
                        essentialLoadedCount++
                        if (essentialLoadedCount >= totalEssential) {
                            onComplete() // 필수 로딩 완료! 로딩 바 제거
                        }
                    }
                }
            }
        }

        // 2. 백그라운드 로딩 (상세/원본 이미지) - onComplete와 상관없이 계속 진행
        locationImages.forEach { postImage ->
            viewModelScope.launch(Dispatchers.IO) {
                // 마커 클릭 시 뜰 원본 (별도의 키로 저장하거나 원본 URL 그대로 사용)
                // 키를 다르게 하고 싶다면 postImage.url + "_full" 형태 사용 가능
                preloadToStateManager(postImage.url, postImage.url + "_full", 800, "FULL")
            }
        }

        commonImages.forEach { commonImage ->
            viewModelScope.launch(Dispatchers.IO) {
                // 페이저에서 보일 일반 이미지들
                preloadToStateManager(commonImage.url, commonImage.url, 800, "FULL")
            }
        }

        // (2) [수정] 수집된 모든 프로필(게시글 작성자 + 댓글 작성자) 미리 로드
        allProfileUrls.forEach { url ->
            viewModelScope.launch(Dispatchers.IO) {
                // 프로필은 150~200px 정도면 충분히 선명합니다.
                preloadToStateManager(url, url, 150, "THUMB")
            }
        }
    }

    // 프리로딩 함수 수정 (용도별로 캐시 분리 적재)
    private suspend fun preloadToStateManager(url: String, cacheKey: String, size: Int, type: String) {
        if (url.isEmpty()) return
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(size)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let {
                when (type) {
                    "THUMB" -> _thumbnailCache.update { current -> current + (cacheKey to it) }
                    "FULL" -> _fullBitmapCache.update { current -> current + (cacheKey to it) }
                }
            }
        }
    }

    fun fetchPostDetail(postId: String) {
        viewModelScope.launch {
            _communityUiState.update { it.copy(
                isLoading = true,
                selectedPost = null,
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

                // 2. 목록 화면의 posts 리스트에 있는 해당 포스트 숫자도 +1
                _communityUiState.update { state ->
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) post.copy(commentCount = post.commentCount + 1)
                        else post
                    }
                    state.copy(posts = updatedPosts)
                }
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

    // 게시글 삭제 함수
    fun deletePost(post: Post) {
        viewModelScope.launch {
            val result = dataSource.deletePost(post)
            if (result is AuthResult.Success) {
                // 삭제 성공 시 현재 리스트에서 해당 포스트 제거하여 UI 갱신
                _communityUiState.update { state ->
                    state.copy(posts = state.posts.filter { it.postId != post.postId })
                }
            } else if (result is AuthResult.Fail) {
                // 에러 처리 (Toast 메시지 등)
                Log.e("CommunityViewModel", "삭제 실패: ${result.message}")
            }
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

