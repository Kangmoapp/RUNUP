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
import com.example.runup.domain.model.PostImage
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.UserActivityStats
import com.example.runup.domain.model.ViewScope
import com.example.runup.domain.repository.CourseRepository
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.CommunityRefreshManager
import com.example.runup.ui.util.ImagePreloader
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
    private val courseRepository: CourseRepository,
    private val refreshManager: CommunityRefreshManager,
    private val imagePreloader: ImagePreloader,
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

    private val _mapSnapshotCache = MutableStateFlow<Map<String, MapSnapshot>>(emptyMap())
    val mapSnapshotCache = _mapSnapshotCache.asStateFlow()

    private val _locationMarkerCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val locationMarkerCache = _locationMarkerCache.asStateFlow()

    private val _profileCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val profileCache = _profileCache.asStateFlow()

    private val _fullImageCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val fullImageCache = _fullImageCache.asStateFlow()

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

    fun initUserPage(targetUid: String) {
        // 1. 페이징 관련 내부 변수 초기화 (매우 중요!)
        lastVisibleSnapshot = null
        isLastPage = false

        // 2. UI 상태 초기화 (리스트 비우고 로딩 띄우기)
        _communityUiState.update { it.copy(
            posts = emptyList(),
            isInitialLoading = true, // 새로운 데이터를 가져올 때까지 로딩 화면 강제 🔹
            isRefreshing = false,
            isLastPage = false
        ) }

        // 3. 탭 상태도 기본값으로 (필요시)
        _currentTab.value = "POSTS"

        // 4. 새로운 데이터 로드 시작
        fetchUserPosts(targetUid, isInitial = true)
        loadUserStats(targetUid)
    }

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

            val result = dataSource.getTargetUserPosts(
                targetUid = currentTargetUid,
                tabType = _currentTab.value,
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

    // ── 🔹 [개편] 3단계 프리로딩 아키텍처 적용 ── 📍
    private fun preloadBitmaps(posts: List<Post>, onComplete: () -> Unit = {}) {
        if (posts.isEmpty()) { onComplete(); return }

        // 1. URL 추출 유틸리티 활용
        val urlMap = imagePreloader.extractUrlsFromPosts(posts)

        val markerImages = urlMap["MARKER"] as List<PostImage>
        val authorUrls = urlMap["AUTHOR"] as List<String>
        val commentUrls = urlMap["COMMENT"] as List<String>
        val fullUrls = urlMap["FULL"] as List<String>

        // ── [1단계: ESSENTIAL] 마커 + 작성자 프로필 (로딩 인디케이터 해제 기준) ── 📍
        val totalEssential = markerImages.size + authorUrls.size
        var essentialLoadedCount = 0

        if (totalEssential == 0) {
            onComplete()
        } else {
            // [A] 마커 이미지 로드 (locationMarkerCache)
            markerImages.forEach { img ->
                viewModelScope.launch(Dispatchers.IO) {
                    val targetUrl = img.thumbnailUrl.ifEmpty { img.url }
                    val bitmap = imagePreloader.loadBitmap(targetUrl, 150)
                    bitmap?.let { b ->
                        _locationMarkerCache.update { it + (img.url to b) }
                    }
                    launch(Dispatchers.Main) {
                        essentialLoadedCount++
                        if (essentialLoadedCount >= totalEssential) onComplete()
                    }
                }
            }

            // [B] 게시글 작성자 프로필 로드 (profileCache)
            authorUrls.forEach { url ->
                viewModelScope.launch(Dispatchers.IO) {
                    val bitmap = imagePreloader.loadBitmap(url, 150)
                    bitmap?.let { b -> _profileCache.update { it + (url to b) } }
                    launch(Dispatchers.Main) {
                        essentialLoadedCount++
                        if (essentialLoadedCount >= totalEssential) onComplete()
                    }
                }
            }
        }

        // ── [2단계: BG_THUMB] 댓글 작성자 프로필 (profileCache) ──
        commentUrls.forEach { url ->
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = imagePreloader.loadBitmap(url, 150)
                bitmap?.let { b -> _profileCache.update { it + (url to b) } }
            }
        }

        // ── [3단계: FULL] 고해상도 상세 이미지 (fullImageCache) ──
        fullUrls.forEach { url ->
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = imagePreloader.loadBitmap(url, 800)
                bitmap?.let { b -> _fullImageCache.update { it + (url to b) } }
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
                var updatedPost: Post? = null

                _communityUiState.update { state ->
                    // 1. 게시물 리스트 업데이트
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            val alreadyFollowed = post.followedBy.contains(myUid)
                            val newCount = if (alreadyFollowed) post.followCount else post.followCount + 1
                            val newFollowedBy = if (alreadyFollowed) post.followedBy else post.followedBy + myUid

                            val newPost = post.copy(followCount = newCount, followedBy = newFollowedBy)
                            updatedPost = newPost // 상태 저장 📍
                            newPost
                        } else post
                    }

                    // 2. 상세 팝업(selectedPostForPopup) 실시간 동기화
                    val currentPopup = _selectedPostForPopup.value
                    if (currentPopup?.postId == postId) {
                        val alreadyFollowed = currentPopup.followedBy.contains(myUid)
                        val newCount = if (alreadyFollowed) currentPopup.followCount else currentPopup.followCount + 1
                        val newFollowedBy = if (alreadyFollowed) currentPopup.followedBy else currentPopup.followedBy + myUid

                        val newPopupPost = currentPopup.copy(followCount = newCount, followedBy = newFollowedBy)
                        _selectedPostForPopup.value = newPopupPost
                        updatedPost = newPopupPost // 팝업이 최신이면 팝업 데이터 기준 📍
                    }

                    state.copy(posts = updatedPosts)
                }

                // ── 🔹 [핵심] 따라뛰기 5회 달성 시 공식 코스로 승격 ── 📍
                updatedPost?.let { post ->
                    if (post.followCount == 5 && post.runRecord != null) {
                        val saveResult = courseRepository.saveCourse(post.runRecord.course)
                        if (saveResult is AuthResult.Success) {
                            Log.d("CoursePromotion", "내 활동 페이지에서 공식 코스 등록 성공: ${post.postId}")
                        }
                    }
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