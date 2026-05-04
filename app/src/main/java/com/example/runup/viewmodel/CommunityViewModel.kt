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
import com.example.runup.data.source.local.SessionManager
import com.example.runup.data.source.remote.community.CommunityDataSourceImpl
import com.example.runup.domain.model.*
import com.example.runup.domain.repository.CourseRepository
import com.example.runup.domain.repository.LocationRepository
import com.example.runup.domain.repository.UserRepository
import com.example.runup.ui.util.CommunityRefreshManager
import com.example.runup.ui.util.ImagePreloader
import com.example.runup.domain.model.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    val hasMore: Boolean = true, // 더 가져올 러닝 기록 남아있는지
    val lastDate: Long? = null, // 더 가져올때 참조할 러닝 날짜
    val isPaging: Boolean = false //
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
private var isLastPage = false

private var lastPostId: String? = null // 🔹 DocumentSnapshot 대신 ID를 저장합니다.

@HiltViewModel
class CommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: CommunityDataSourceImpl,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val refreshManager: CommunityRefreshManager,
    private val imagePreloader: ImagePreloader,
    private val courseRepository: CourseRepository,
    private val sessionManager: SessionManager // 🔹 새로 추가!
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
        if (!forceRefresh && isInitial && _communityUiState.value.posts.isNotEmpty()) return
        if (_communityUiState.value.isLoading || (isLastPage && !isInitial)) return

        if (isInitial) {
            lastPostId = null // 🔹 초기화
            isLastPage = false
            _communityUiState.update { it.copy(
                posts = emptyList(),
                isInitialLoading = !forceRefresh,
                isRefreshing = forceRefresh
            ) }
        }

        viewModelScope.launch {
            _communityUiState.update { it.copy(isLoading = true) }
            try {
                val currentFilter = _communityUiState.value.filterState
                val myUid = sessionManager.getUid()
                val targetIds = when (currentFilter.scope) {
                    // 🔹 FirebaseAuth.getInstance()... 대신 myUid 사용
                    ViewScope.MINE -> listOf(myUid)
                    ViewScope.FRIENDS -> {
                        val result = userRepository.getFriendUids()
                        if (result is AuthResult.Success) result.data.ifEmpty { emptyList() } else emptyList()
                    }
                    ViewScope.ALL -> emptyList()
                }

                // 🔹 lastPostId 파라미터 전달
                val result = dataSource.getPosts(lastPostId, 3L, currentFilter, friendIds = targetIds)

                if (result is AuthResult.Success) {
                    val (newPosts, lastId) = result.data // 🔹 Pair 해체
                    if (newPosts.isEmpty() || newPosts.size < 3) {
                        isLastPage = true
                    }
                    lastPostId = lastId // 🔹 커서 업데이트

                    _communityUiState.update { state ->
                        state.copy(posts = if (isInitial) newPosts else state.posts + newPosts)
                    }

                    preloadBitmaps(newPosts) {
                        if (isInitial) {
                            _communityUiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _communityUiState.update { it.copy(isInitialLoading = false) }
            } finally {
                _communityUiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshManager.refreshEvent.collectLatest {
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

    // CommunityViewModel.kt 내의 uploadPost 함수를 통째로 교체하세요.

    fun uploadPost(content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 1. 로딩 시작
            _postUploadUiState.update { it.copy(isLoading = true) }

            try {
                val selectedRecord = _postUploadUiState.value.selectedRunRecord

                // 2. 타겟 좌표 결정
                val targetLocation = if (selectedRecord != null) {
                    val centerLat = (selectedRecord.course.minLat + selectedRecord.course.maxLat) / 2.0
                    val centerLng = (selectedRecord.course.minLng + selectedRecord.course.maxLng) / 2.0
                    GeoPoint(centerLat, centerLng)
                } else {
                    locationRepository.currentLocation.value
                }

                // 🌟 [진실의 로그 1] 내 GPS 좌표가 제대로 잡히고 있나?
                Log.d("UploadDebug", "1. 현재 잡힌 타겟 좌표: 위도=${targetLocation?.latitude}, 경도=${targetLocation?.longitude}")

                // 🌟 1. 러닝 기록이 있다면 그 중앙 좌표를, 없다면 0.0으로 둡니다.
                val centerLat = if (selectedRecord != null) (selectedRecord.course.minLat + selectedRecord.course.maxLat) / 2.0 else 0.0
                val centerLng = if (selectedRecord != null) (selectedRecord.course.minLng + selectedRecord.course.maxLng) / 2.0 else 0.0

                // 🌟 2. 주소 결정 로직 (아주 안전하게!)
                val address = if (centerLat != 0.0 && centerLng != 0.0) {
                    // 러닝 기록의 좌표가 정상(0.0이 아님)일 때만 네이버 API를 호출합니다.
                    try {
                        locationRepository.getAddressFromCoords(centerLat, centerLng)
                    } catch (e: Exception) {
                        Log.e("UploadDebug", "러닝 기록 주소 변환 실패, 내 현재 위치로 대체합니다.")
                        addressUiState.value // 실패하면 앱이 켜질 때 잡아둔 내 현재 주소를 씁니다.
                    }
                } else {
                    // 일반 포스트 업로드이거나 좌표가 0.0일 때는 굳이 API를 안 부르고, 이미 갖고 있는 주소를 바로 씁니다!
                    Log.d("UploadDebug", "일반 업로드 또는 좌표 0.0 -> 기존 주소(${addressUiState.value?.dong}) 바로 사용!")
                    addressUiState.value
                }

                Log.d("UploadDebug", "최종 결정된 서버 전송 주소: ${address?.city} ${address?.district} ${address?.dong}")

                // 4. 업로드 API 호출
                val result = dataSource.uploadPost(
                    content = content,
                    locationImageUris = selectedLocationImageUris,
                    commonImageUris = selectedCommonImageUris,
                    runRecord = selectedRecord,
                    address = address
                )

                // 5. 결과 처리
                if (result is AuthResult.Success) {
                    clearSelectedImages()
                    _postUploadUiState.update { it.copy(selectedRunRecord = null) }
                    onSuccess()
                    fetchPosts(isInitial = true, forceRefresh = true)
                } else if (result is AuthResult.Fail) {
                    Log.e("UploadDebug", "업로드 API 서버 통신 실패: ${result.message}")
                }

            } catch (e: Exception) {
                Log.e("UploadDebug", "업로드 로직 전체에서 예상치 못한 에러: ${e.message}")
            } finally {
                // 🌟 6. [핵심 수정] 성공하든 에러가 나든 무조건 로딩 바를 꺼줍니다.
                // 그래야 화면이 멈춘 것처럼 보이지 않습니다.
                _postUploadUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetUploadState() {
        // 1. 이미지 리스트 초기화
        selectedLocationImageUris = emptyList()
        selectedCommonImageUris = emptyList()

        // 2. 업로드 UI 상태 초기화 (선택된 기록, 로딩 상태 등 모두 초기값으로)
        _postUploadUiState.update {
            PostUploadUiState() // 데이터 클래스를 초기 생성자로 덮어씌움
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
                val myUid = sessionManager.getUid()
                var targetPost: Post? = null

                // 1. 화면의 하트/팔로우 숫자 즉시 업데이트
                _communityUiState.update { state ->
                    val updatedPosts = state.posts.map { post ->
                        if (post.postId == postId) {
                            val alreadyFollowed = post.followedBy.contains(myUid)
                            // 팔로우 중이었으면 취소(-1), 아니었으면 추가(+1)
                            val newCount = if (alreadyFollowed) post.followCount - 1 else post.followCount + 1
                            val newFollowedBy = if (alreadyFollowed) post.followedBy - myUid else post.followedBy + myUid

                            val updatedPost = post.copy(followCount = newCount, followedBy = newFollowedBy)
                            targetPost = updatedPost
                            updatedPost
                        } else post
                    }
                    state.copy(posts = updatedPosts)
                }

                // 🚨 기존에 있던 'post.followCount == 5 이면 courseRepository.saveCourse()' 하던 로직은 삭제!
                // (서버가 알아서 MySQL 공식 코스 DB에 예쁘게 넣어줍니다)

                // 2. 누르자마자 홈 화면으로 넘어가서 바로 뛸 수 있게 코스 데이터 전달 🏃‍♂️
                targetPost?.let { onCourseReady(it) }
            }
        }
    }

    fun selectRunRecordFromMyPage(record: RunRecord) { // 마이페이지 -> 포스트 업로드 스크린으로 가져온 기록을 selectRunRecord 에 업데이트
        _postUploadUiState.update {
            it.copy(
                selectedRunRecord = record,
                isSheetOpen = false // 이미 선택했으니 시트는 닫힘 상태로
            )
        }
    }


}

