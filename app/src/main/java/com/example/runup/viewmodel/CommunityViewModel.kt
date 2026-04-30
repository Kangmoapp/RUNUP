package com.example.runup.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runup.data.source.remote.community.CommunityDataSourceImpl
import com.example.runup.domain.model.AddressModel
import com.example.runup.domain.model.AdmVO
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Comment
import com.example.runup.domain.model.FilterState
import com.example.runup.domain.model.FilterType
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.PostImage
import com.example.runup.domain.model.RunFilter
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.ViewScope
import com.example.runup.domain.repository.CourseRepository
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.CommunityRefreshManager
import com.example.runup.ui.util.ImagePreloader
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val isRefreshing: Boolean = false,
    val filterState: FilterState = FilterState(),
    val isFilterDialogOpen: Boolean = false,
    val isLastPage: Boolean = false // 🔹 추가
)

data class PostUploadUiState(
    val runRecords: List<RunRecord> = emptyList(),
    val selectedRunRecord: RunRecord? = null,
    val isSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    // 🔹 페이지네이션 상태
    val hasMore: Boolean = true,
    val lastDate: Long? = null,
    val isPaging: Boolean = false
)

data class MapSnapshot(
    val staticMapUrl: String, // 네이버 API 요청한 지도 StaticImage Url
    val pathPoints: List<Pair<Offset, Boolean>>,
    val startPoint: Offset?,             // 🔹 시작점 픽셀
    val endPoint: Offset?,               // 🔹 종료점 픽셀
    val markerPositions: Map<String, Offset>, // 마커 처음 위치
    val closerOffsets: Map<String, Offset>,   // 마커의 최종 위치
    val courseBounds: androidx.compose.ui.geometry.Rect // 코스 경계 픽셀
)
private var lastVisibleSnapshot: DocumentSnapshot? = null
private var isLastPage = false



@HiltViewModel
class CommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: CommunityDataSourceImpl,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val refreshManager: CommunityRefreshManager,
    private val imagePreloader: ImagePreloader,
    private val courseRepository: CourseRepository, // 🔹 추가
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

    // [A] 지도 마커 및 피드 리스트용 (최우선 순위)
    private val _locationMarkerCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val locationMarkerCache = _locationMarkerCache.asStateFlow()

    // [B] 유저 프로필 이미지용 (작성자 + 댓글 작성자)
    private val _profileCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val profileCache = _profileCache.asStateFlow()

    // [C] 상세/확대 보기용 고해상도 이미지
    private val _fullImageCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val fullImageCache = _fullImageCache.asStateFlow()

    val addressUiState: StateFlow<AddressModel?> = locationRepository.addressState

    // 🔹 단계별로 분리해서 저장
    private val _districtLocations = MutableStateFlow<List<AdmVO>>(emptyList())
    private val _dongLocations = MutableStateFlow<List<AdmVO>>(emptyList())

    val districtLocations: StateFlow<List<AdmVO>> = _districtLocations
    val dongLocations: StateFlow<List<AdmVO>> = _dongLocations

    // 로딩 상태도 단계별로
    private val _isLoadingDistrict = MutableStateFlow(false)
    private val _isLoadingDong = MutableStateFlow(false)
    val isLoadingDistrict: StateFlow<Boolean> = _isLoadingDistrict
    val isLoadingDong: StateFlow<Boolean> = _isLoadingDong

    init {
        requestInitialAddress()
        observeRefreshEvents()
    }

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
                val currentFilter = _communityUiState.value.filterState

                // 🔹 [핵심] 스코프에 따른 ID 리스트 준비
                val targetIds = when (currentFilter.scope) {
                    ViewScope.MINE -> listOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    ViewScope.FRIENDS -> {
                        val result = userRepository.getFriendUids()
                        if (result is AuthResult.Success) {
                            result.data.ifEmpty { emptyList() }
                        } else {
                            emptyList()
                        }
                    }
                    ViewScope.ALL -> emptyList()
                }

                // 마지막 보던 곳에서 최대 3개 가져오기
                val result = dataSource.getPosts(lastVisibleSnapshot, 3L, currentFilter, friendIds = targetIds)

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

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            // SharedFlow를 통해 전역적으로 발생하는 리프레시 신호를 수집합니다 👂
            refreshManager.refreshEvent.collectLatest {
                // isInitial = true -> 페이징 초기화
                // forceRefresh = true -> 로딩 바 활성화 및 캐시 무시
                fetchPosts(isInitial = true, forceRefresh = true)
            }
        }
    }

    private fun preloadBitmaps(posts: List<Post>, onComplete: () -> Unit = {}) {
        if (posts.isEmpty()) { onComplete(); return }

        val urlMap = imagePreloader.extractUrlsFromPosts(posts)

        val markerImages = urlMap["MARKER"] as List<PostImage>
        val authorUrls = urlMap["AUTHOR"] as List<String>
        val commentUrls = urlMap["COMMENT"] as List<String>
        val fullUrls = urlMap["FULL"] as List<String>

        // ── [1단계: ESSENTIAL] 마커 + 작성자 프로필 (onComplete 트리거) ──
        val totalEssential = markerImages.size + authorUrls.size
        var essentialLoadedCount = 0

        if (totalEssential == 0) {
            onComplete()
        } else {
            // [A] 마커 이미지 로드 (locationMarkerCache 저장)
            markerImages.forEach { img ->
                viewModelScope.launch(Dispatchers.IO) {
                    val targetUrl = img.thumbnailUrl.ifEmpty { img.url }
                    val bitmap = imagePreloader.loadBitmap(targetUrl, 150)
                    bitmap?.let { b ->
                        // 🔹 핵심: 저장 키는 반드시 원본 img.url 사용!
                        _locationMarkerCache.update { it + (img.url to b) }
                    }
                    launch(Dispatchers.Main) {
                        essentialLoadedCount++
                        if (essentialLoadedCount >= totalEssential) onComplete()
                    }
                }
            }

            // [B] 작성자 프로필 로드 (profileCache 저장)
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

        // ── [2단계: BG_THUMB] 댓글 작성자 프로필 (profileCache 저장) ──
        commentUrls.forEach { url ->
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = imagePreloader.loadBitmap(url, 150)
                bitmap?.let { b -> _profileCache.update { it + (url to b) } }
            }
        }

        // ── [3단계: FULL] 고해상도 이미지 (fullImageCache 저장) ──
        fullUrls.forEach { url ->
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = imagePreloader.loadBitmap(url, 800)
                bitmap?.let { b -> _fullImageCache.update { it + (url to b) } }
            }
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

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            // 1. 데이터소스(서버)에 삭제 요청
            val result = dataSource.deleteComment(postId, commentId)

            if (result is AuthResult.Success) {
                // 3. 목록 화면(posts) 및 댓글 리스트 UI 즉시 반영 🔹
                _communityUiState.update { state ->
                    // 메인 리스트의 댓글 수 -1
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            post.copy(commentCount = (post.commentCount - 1).coerceAtLeast(0))
                        } else post
                    }

                    // 현재 열려있는 댓글 리스트에서 삭제된 댓글 제거
                    val updatedComments = state.comments.filterNot { it.commentId == commentId }

                    state.copy(
                        posts = updatedPosts,
                        comments = updatedComments
                    )
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
            comments = emptyList()
        ) }
    }

    fun uploadPost(content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 피드 상태의 로딩 시작
            _postUploadUiState.update { it.copy(isLoading = true) }

            val selectedRecord = _postUploadUiState.value.selectedRunRecord

            // 1. 🔹 주소를 추출할 타겟 좌표 결정 (코스 중점 우선)
            val targetLocation = if (selectedRecord != null) {
                // 코스의 위도/경도 중점 계산
                val centerLat = (selectedRecord.course.minLat + selectedRecord.course.maxLat) / 2.0
                val centerLng = (selectedRecord.course.minLng + selectedRecord.course.maxLng) / 2.0
                GeoPoint(centerLat, centerLng)
            } else {
                // 만약 러닝 기록 없이 글만 쓰는 경우라면 현재 위치 사용
                locationRepository.currentLocation.value
            }

            // 2. 🔹 결정된 좌표로 주소(AddressModel) 변환
            val address = targetLocation?.let {
                locationRepository.getAddressFromCoords(it.latitude, it.longitude)
            }

            val result = dataSource.uploadPost(content, selectedLocationImageUris, selectedCommonImageUris,selectedRecord, address)

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
                            post.copy(likes = newLikes,
                                isLiked = isLiked)
                        } else post
                    }
                    state.copy(posts = updatedPosts)
                }
            }
        }
    }

    // 상태 업데이트
    fun fetchMyRunRecords(isInitial: Boolean = true) {
        if (isInitial) {
            _postUploadUiState.update { it.copy(
                runRecords = emptyList(),
                lastDate = null,
                hasMore = true,
                isLoading = true
            ) }
        }

        val currentState = _postUploadUiState.value
        if (!currentState.hasMore || currentState.isPaging) return

        viewModelScope.launch {
            if (!isInitial) _postUploadUiState.update { it.copy(isPaging = true) }

            // 🔹 Repository의 페이지네이션 함수 호출 (전체 필터, 5개씩)
            val result = userRepository.getRunsPaged(
                filter = RunFilter.ALL,
                lastDate = currentState.lastDate,
                pageSize = 5
            )

            if (result is AuthResult.Success) {
                val newRecords = result.data
                _postUploadUiState.update { state ->
                    state.copy(
                        runRecords = state.runRecords + newRecords,
                        lastDate = newRecords.lastOrNull()?.recordDate ?: state.lastDate,
                        hasMore = newRecords.size == 5,
                        isLoading = false,
                        isPaging = false
                    )
                }
            } else {
                _postUploadUiState.update { it.copy(isLoading = false, isPaging = false) }
            }
        }
    }

    fun setSheetOpen(isOpen: Boolean) {
        _postUploadUiState.update { it.copy(isSheetOpen = isOpen) }
    }

    fun selectRunRecord(record: RunRecord?) {
        _postUploadUiState.update { it.copy(selectedRunRecord = record, isSheetOpen = false) }
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

            // 필터가 바뀌었으니 데이터 새로고침
            fetchPosts(isInitial = true, forceRefresh = true)
        }
    }
    fun setViewScope(scope: ViewScope) {
        viewModelScope.launch {
            // 1. 스코프 변경
            _communityUiState.update { it.copy(
                filterState = it.filterState.copy(scope = scope)
            ) }
            // 2. 새로운 필터로 포스트 다시 불러오기 (페이징 초기화 포함)
            fetchPosts(isInitial = true, forceRefresh = true)
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

    fun onFollowClick(postId: String, onCourseReady: (Post) -> Unit) {
        viewModelScope.launch {
            val result = dataSource.followRunning(postId)
            if (result is AuthResult.Success) {
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                var targetPost: Post? = null // 🔹 저장을 위한 임시 변수

                // 1. 리스트 상태 즉시 업데이트
                _communityUiState.update { state ->
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            val alreadyFollowed = post.followedBy.contains(myUid)
                            val newCount = if (alreadyFollowed) post.followCount else post.followCount + 1
                            val newFollowedBy = if (alreadyFollowed) post.followedBy else post.followedBy + myUid

                            val updatedPost = post.copy(followCount = newCount, followedBy = newFollowedBy)
                            targetPost = updatedPost // 🔹 업데이트된 데이터를 밖으로 빼냄
                            updatedPost
                        } else post
                    }
                    state.copy(posts = updatedPosts)
                }

                // ── 🔹 2. [핵심] 따라뛰기 5회 달성 시 공식 코스로 등록 ── 📍
                targetPost?.let { post ->
                    // followCount가 딱 5가 된 순간 + 코스 데이터가 실재할 때 실행
                    if (post.followCount == 5 && post.runRecord != null) {
                        val saveResult = courseRepository.saveCourse(post.runRecord.course)

                        if (saveResult is AuthResult.Success) {
                            Log.d("CoursePromotion", "축하합니다! 인기가 많아 공식 코스로 등록되었습니다: ${post.postId}")
                        } else {
                            Log.e("CoursePromotion", "공식 코스 등록 실패")
                        }
                    }
                }

                // 3. 메인으로 코스 데이터 전달 🏃‍♂️
                targetPost?.let { onCourseReady(it) }
            }
        }
    }
}

