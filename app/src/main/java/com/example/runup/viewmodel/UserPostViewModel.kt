package com.example.runup.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.runup.data.source.remote.community.CommunityDataSourceImpl
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AdmVO
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.FilterState
import com.example.runup.domain.model.FilterType
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.UserActivityStats
import com.example.runup.domain.model.ViewScope
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.CommunityRefreshManager
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.ifEmpty

data class UserTotalStats(
    val userName: String = "",
    val totalPosts: Int = 0,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val totalFollows: Int = 0,
)

@HiltViewModel
class UserPostViewModel @Inject constructor(
    private val dataSource: CommunityDataSourceImpl,
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val refreshManager: CommunityRefreshManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    private val _communityUiState = MutableStateFlow(CommunityUiState())
    val communityUiState = _communityUiState.asStateFlow()

    private val _targetUserName = MutableStateFlow("") // 🔹 이름 전용 상태
    private val _rawStats = MutableStateFlow(UserActivityStats())

    val userStats = kotlinx.coroutines.flow.combine(_targetUserName, _rawStats) { name, stats ->
        UserTotalStats(
            userName = stats.userName,
            totalPosts = stats.uploadPostIds.size,
            totalLikes = stats.likePostIds.size,
            totalComments = stats.commentPostIds.size,
            totalFollows = stats.followPostIds.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserTotalStats())

    // 캐시 저장소 (기존과 동일)
    private val _mapSnapshotCache = MutableStateFlow<Map<String, MapSnapshot>>(emptyMap())
    val mapSnapshotCache = _mapSnapshotCache.asStateFlow()
    private val _thumbnailCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnailCache = _thumbnailCache.asStateFlow()
    private val _fullBitmapCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val fullBitmapCache = _fullBitmapCache.asStateFlow()

    private var lastVisibleSnapshot: DocumentSnapshot? = null
    private var isLastPage = false

    val addressUiState: StateFlow<AddressModel?> = locationRepository.addressState

    private val _selectedPostForPopup = MutableStateFlow<Post?>(null)
    val selectedPostForPopup = _selectedPostForPopup.asStateFlow()

    private val _currentTab = MutableStateFlow("POSTS")
    val currentTab = _currentTab.asStateFlow()

    // 지역 필터
    private val _districtLocations = MutableStateFlow<List<AdmVO>>(emptyList())
    private val _dongLocations = MutableStateFlow<List<AdmVO>>(emptyList())

    val districtLocations: StateFlow<List<AdmVO>> = _districtLocations
    val dongLocations: StateFlow<List<AdmVO>> = _dongLocations

    // 지역 필터 로딩
    private val _isLoadingDistrict = MutableStateFlow(false)
    private val _isLoadingDong = MutableStateFlow(false)
    val isLoadingDistrict: StateFlow<Boolean> = _isLoadingDistrict
    val isLoadingDong: StateFlow<Boolean> = _isLoadingDong

    private var currentTargetUid: String = ""

    private var currentLoadedUid: String? = null // 현재 로드된 데이터의 주인 🔹

    // 🔹 이 화면의 핵심: 누구의 글을 보여줄 것인가?
    fun fetchUserPosts(targetUid: String, isInitial: Boolean = false, forceRefresh: Boolean = false) {
        // 최초 진입 시 현재 타겟 아이디 저장
        if (targetUid.isNotEmpty()) { currentTargetUid = targetUid }

        if (currentLoadedUid != currentTargetUid) {
            resetState()
            currentLoadedUid = currentTargetUid
        }

        if (_communityUiState.value.isLoading || (isLastPage && !isInitial)) return

        val determinedScope = if (currentTargetUid == myUid) ViewScope.MINE else ViewScope.FRIENDS

        if (isInitial) {
            lastVisibleSnapshot = null
            isLastPage = false
            _communityUiState.update { it.copy(
                posts = emptyList(),
                isInitialLoading = !forceRefresh,
                isRefreshing = forceRefresh,
                filterState = it.filterState.copy(scope = determinedScope),
                isLastPage = false // 초기화
            ) }
        }

        viewModelScope.launch {
            _communityUiState.update { it.copy(isLoading = true) }

            // 🔹 새로 만든 전용 함수 호출
            val result = dataSource.getTargetUserPosts(
                targetUid = currentTargetUid,
                tabType = _currentTab.value, // 현재 선택된 탭 정보
                filter = _communityUiState.value.filterState,
                lastVisibleSnapshot = lastVisibleSnapshot,
                limit = 4L
            )

            if (result is AuthResult.Success) {
                val (newPosts, lastSnapshot) = result.data
                if (newPosts.size < 4) isLastPage = true
                lastVisibleSnapshot = lastSnapshot

                _communityUiState.update { state ->
                    state.copy(
                        posts = if (isInitial) newPosts else state.posts + newPosts,
                        isLastPage = isLastPage
                    )
                }

                preloadBitmaps(newPosts) {
                    _communityUiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                }
            }
            _communityUiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    // 데이터 저장 함수들
    fun saveMapSnapshot(postId: String, snapshot: MapSnapshot) {
        _mapSnapshotCache.update { it + (postId to snapshot) }
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

    fun deletePost(post: Post) {
        viewModelScope.launch {
            val result = dataSource.deletePost(post)
            if (result is AuthResult.Success) {
                // 1. 메인 리스트 UI 즉시 제거
                _communityUiState.update { state ->
                    state.copy(posts = state.posts.filter { it.postId != post.postId })
                }

                // 2. 상단 통계 실시간 업데이트 (내가 내 페이지를 관리 중일 때) 🔹
                if (myUid == currentTargetUid) {
                    _rawStats.update { current ->
                        // ── 모든 활동 리스트에서 해당 postId를 제거하여 '클린 삭제' 수행 ── 📍
                        current.copy(
                            uploadPostIds = current.uploadPostIds - post.postId,    // 내 게시물 리스트에서 제거
                            likePostIds = current.likePostIds - post.postId,      // 내 좋아요 리스트에서 제거
                            commentPostIds = current.commentPostIds - post.postId, // 내 댓글 리스트에서 제거
                            followPostIds = current.followPostIds - post.postId // 🔹 추가: 팔로우 목록에서도 제거
                        )
                    }
                }

                Log.d("UserPostViewModel", "게시물 삭제 및 통계 정화 완료: ${post.postId}")
                refreshManager.notifyDataChanged()
            } else if (result is AuthResult.Fail) {
                Log.e("UserPostViewModel", "삭제 실패: ${result.message}")
            }
        }
    }

    fun onLikeClick(postId: String) {
        viewModelScope.launch {
            val result = dataSource.toggleLike(postId)
            if (result is AuthResult.Success) {
                val isLiked = result.data ?: false
                _communityUiState.update { state ->
                    val updatedPosts = if (currentTab.value == "HEARTS" && !isLiked && myUid == currentTargetUid) {
                        state.posts.filter { it.postId != postId }
                    } else {
                        state.posts.map { post ->
                            if (post.postId == postId) {
                                val newLikes = if (isLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0)
                                post.copy(likes = newLikes, isLiked = isLiked)
                            } else post
                        }
                    }
                    val updatedSelectedPost = if (state.selectedPost?.postId == postId) {
                        val newLikes = if (isLiked) state.selectedPost.likes + 1 else (state.selectedPost.likes - 1).coerceAtLeast(0)
                        state.selectedPost.copy(likes = newLikes, isLiked = isLiked)
                    } else state.selectedPost
                    state.copy(posts = updatedPosts, selectedPost = updatedSelectedPost)
                }

                if (myUid == currentTargetUid) { // 👈 이 조건이 핵심입니다 📍
                    _rawStats.update { current ->
                        if (isLiked) {
                            if (!current.likePostIds.contains(postId)) {
                                current.copy(likePostIds = current.likePostIds + postId)
                            } else current
                        } else {
                            current.copy(likePostIds = current.likePostIds - postId)
                        }
                    }
                }
                refreshManager.notifyDataChanged()
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

                if (myUid == currentTargetUid) { // 👈 이 조건문을 추가합니다 📍
                    _rawStats.update { current ->
                        // 이미 댓글을 달았던 포스트라면 리스트에 추가하지 않음 (중복 방지)
                        if (!current.commentPostIds.contains(postId)) {
                            current.copy(commentPostIds = current.commentPostIds + postId)
                        } else {
                            current
                        }
                    }
                }

                refreshManager.notifyDataChanged()
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            val result = dataSource.deleteComment(postId, commentId)

            if (result is AuthResult.Success) {
                val isLastCommentDeleted = result.data // true면 마지막 댓글이었다는 뜻 🔹

                // 1. 상세 정보 및 UI 즉시 반영
                fetchPostDetail(postId)
                _communityUiState.update { state ->
                    val updatedPosts = if (currentTab.value == "COMMENTS" && isLastCommentDeleted) {
                        state.posts.filter { it.postId != postId }
                    } else {
                        state.posts.map { post ->
                            if (post.postId == postId) {
                                post.copy(commentCount = (post.commentCount - 1).coerceAtLeast(0))
                            } else post
                        }
                    }
                    val updatedComments = state.comments.filterNot { it.commentId == commentId }
                    state.copy(posts = updatedPosts, comments = updatedComments)
                }

                // 2. 통계 실시간 동기화 (내 페이지일 때만) 📍
                if (myUid == currentTargetUid && isLastCommentDeleted) {
                    _rawStats.update { current ->
                        // 마지막 댓글이었으므로 통계 목록에서 해당 postId 제거
                        current.copy(commentPostIds = current.commentPostIds - postId)
                    }
                }

                refreshManager.notifyDataChanged()
            }
        }
    }

    fun onFollowClick(postId: String, onCourseReady: (Post) -> Unit) {
        viewModelScope.launch {
            val result = dataSource.followRunning(postId)
            if (result is AuthResult.Success) {
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

                _communityUiState.update { state ->
                    // 1. 게시물 리스트 업데이트
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            val alreadyFollowed = post.followedBy.contains(myUid)
                            val newCount = if (alreadyFollowed) post.followCount else post.followCount + 1
                            val newFollowedBy = if (alreadyFollowed) post.followedBy else post.followedBy + myUid
                            post.copy(followCount = newCount, followedBy = newFollowedBy)
                        } else post
                    }

                    // 2. 상세 팝업(selectedPostForPopup) 실시간 동기화 🔹
                    val currentPopup = _selectedPostForPopup.value
                    if (currentPopup?.postId == postId) {
                        val alreadyFollowed = currentPopup.followedBy.contains(myUid)
                        val newCount = if (alreadyFollowed) currentPopup.followCount else currentPopup.followCount + 1
                        val newFollowedBy = if (alreadyFollowed) currentPopup.followedBy else currentPopup.followedBy + myUid
                        _selectedPostForPopup.value = currentPopup.copy(followCount = newCount, followedBy = newFollowedBy)
                    }

                    state.copy(posts = updatedPosts)
                }

                // ── 🔹 추가: 내 페이지일 경우 통계(followedPostIds) 업데이트 ── 📍
                if (myUid == currentTargetUid) {
                    _rawStats.update { current ->
                        if (!current.followPostIds.contains(postId)) {
                            current.copy(followPostIds = current.followPostIds + postId)
                        } else {
                            current
                        }
                    }
                }

                // 3. 전역 리프레시 알림 (메인 피드 등 다른 화면 동기화)
                refreshManager.notifyDataChanged()

                // 4. 콜백 실행 (코스 데이터를 들고 메인으로 이동)
                _communityUiState.value.posts.find { it.postId == postId }?.let {
                    onCourseReady(it)
                }
            }
        }
    }





    fun setFilter(type: FilterType, city: String = "", district: String = "", dong: String = "") {
        viewModelScope.launch {
            _communityUiState.update { currentState ->
                // 🔹 현재의 scope(전체/친구/나)를 그대로 유지하면서 지역 정보만 업데이트합니다.
                val updatedFilter = when (type) {
                    FilterType.MY_LOCATION -> {
                        val address = addressUiState.value
                        currentState.filterState.copy(
                            type = type,
                            city = address?.city ?: "",
                            district = address?.district ?: "",
                            dong = address?.dong ?: ""
                        )
                    }
                    else -> {
                        currentState.filterState.copy(
                            type = type,
                            city = city,
                            district = district,
                            dong = dong
                        )
                    }
                }
                currentState.copy(filterState = updatedFilter)
            }

            fetchUserPosts(currentTargetUid, isInitial = true, forceRefresh = true)
        }
    }


    // ViewModel
    fun loadDistricts(cityCode: String, cityName: String) {  // 🔹 이름 추가
        viewModelScope.launch {
            _isLoadingDistrict.value = true
            _districtLocations.value = emptyList()
            _dongLocations.value = emptyList()
            try {
                val result = locationRepository.fetchLocations(
                    parentCode = cityCode,
                    locationName = cityName  // 🔹 "서울특별시" 넘김
                )
                _districtLocations.value = result
            } catch (e: Exception) {
                Log.e("LocationAPI", "구/군 로드 실패: ${e.localizedMessage}")
            } finally {
                _isLoadingDistrict.value = false
            }
        }
    }

    fun loadDongs(districtCode: String, cityName: String, districtName: String) {  // 🔹 이름 추가
        viewModelScope.launch {
            _isLoadingDong.value = true
            _dongLocations.value = emptyList()
            try {
                val combinedName = "$cityName $districtName"
                val result = locationRepository.fetchLocations(
                    parentCode = districtCode,
                    locationName = combinedName  // 🔹 "수성구" 이렇게 넘김
                )
                _dongLocations.value = result
            } catch (e: Exception) {
                Log.e("LocationAPI", "동/읍/면 로드 실패: ${e.localizedMessage}")
            } finally {
                _isLoadingDong.value = false
            }
        }
    }

    // 처음 커뮤니티 화면 들어왔을때 위치 한번 갱신
    private fun requestInitialAddress() {
        viewModelScope.launch {
            locationRepository.currentLocation.value?.let {
                locationRepository.refreshAddressIfNeeded(it.latitude, it.longitude)
            }
        }
    }

    fun selectPost(post: Post?) {
        _selectedPostForPopup.value = post
    }

    // 탭 클릭 시 호출
    fun onTabSelected(tab: String) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        fetchUserPosts(currentTargetUid, isInitial = true)
    }

    // 초기 로드 시 통계(전체 개수) 가져오기
    fun loadUserStats(uid: String) {
        viewModelScope.launch {
            val result = userRepository.getUserActivityStats(uid)
            if (result is AuthResult.Success) {
                _rawStats.value = result.data
            }
        }
    }

    fun resetState() {
        // 1. 게시물 및 로딩 상태 초기화 🔹
        _communityUiState.update {
            CommunityUiState(
                posts = emptyList(),
                isInitialLoading = true
            )
        }
        // 2. 상단 통계 초기화 🔹
        _rawStats.value = UserActivityStats()

    }
}