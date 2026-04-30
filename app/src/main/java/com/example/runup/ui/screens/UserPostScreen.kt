package com.example.runup.ui.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.runup.BuildConfig
import com.example.runup.domain.model.FilterType
import com.example.runup.domain.model.Post
import com.example.runup.ui.components.CommentBottomSheet
import com.example.runup.ui.components.CustomLocationPicker
import com.example.runup.ui.components.EnlargedImageDialog
import com.example.runup.ui.components.LocationMenuContent
import com.example.runup.ui.components.PostItem
import com.example.runup.ui.components.ProfileMiniPopup
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.components.getSelectedLocationText
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.ui.util.assignSlots
import com.example.runup.ui.util.calculateCloserOffset
import com.example.runup.ui.util.latLngToPixel
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.example.runup.viewmodel.HomeViewModel
import com.example.runup.viewmodel.MapSnapshot
import com.example.runup.viewmodel.UserPostViewModel
import com.example.runup.viewmodel.UserTotalStats
import com.google.firebase.firestore.GeoPoint


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostScreen(
    targetUid: String,
    onBackClick: () -> Unit,
    onNavigateToUser: (String) -> Unit,
    onFollowClick: () -> Unit,
    viewModel: UserPostViewModel = hiltViewModel(),
    mainViewModel: HomeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val uiState by viewModel.communityUiState.collectAsState()
    val addressUiState by viewModel.addressUiState.collectAsState()
    val selectedPostForPopup by viewModel.selectedPostForPopup.collectAsState()
    var selectedMiniProfileId by remember { mutableStateOf<String?>(null) }

    val selectedTab by viewModel.currentTab.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val myUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }

    val topBarTitle = if (targetUid == myUid) {
        "나의 활동"
    } else {
        if (stats.userName.isNotEmpty()) "${stats.userName}님의 활동" else "활동 내역"
    }

    // 🔹 [중요] 실시간 데이터 동기화: 팝업에 띄울 포스트를 전체 리스트에서 실시간으로 찾습니다.
    // 이렇게 하면 뷰모델에서 좋아요 수가 변할 때 팝업 내 숫자도 즉시 바뀝니다.
    val livePopupPost = remember(uiState.posts, selectedPostForPopup) {
        uiState.posts.find { it.postId == selectedPostForPopup?.postId }
    }

    //상태 관리
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterSubMenu by remember { mutableStateOf("MAIN") }
    var enlargedImageUri by remember { mutableStateOf<String?>(null) } // 🔹 이미지 확대 상태 추가


    val mapSnapshotsCache by viewModel.mapSnapshotCache.collectAsState()
    val locationMarkerCache by viewModel.locationMarkerCache.collectAsState() // 마커 location 썸네일 이미지
    val profileCache by viewModel.profileCache.collectAsState()               // 프로필용 (포스트 작성자 프로필 + 댓글 작성자 프로필)
    val fullImageCache by viewModel.fullImageCache.collectAsState()           // 고해상도용 (location 이미지 원본 + common 이미지 원본)

    // 바텀 시트 상태 관리
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCommentSheet by remember { mutableStateOf(false) }

    val districtList by viewModel.districtLocations.collectAsState()
    val dongList by viewModel.dongLocations.collectAsState()
    val isLoadingDistrict by viewModel.isLoadingDistrict.collectAsState()
    val isLoadingDong by viewModel.isLoadingDong.collectAsState()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val popupWidthPx = screenWidthPx * 0.95f


    LaunchedEffect(targetUid) {
        // 기존의 개별 호출을 하나로 통합하여 순차/원자적 실행 보장
        viewModel.initUserPage(targetUid)
    }

    BackHandler(enabled = showCommentSheet) {
        // 아무것도 안 함 (CommentBottomSheet가 처리)
    }

    // 포스트 팝업만 열렸을 때
    BackHandler(enabled = !showCommentSheet && selectedPostForPopup != null) {
        viewModel.selectPost(null)
    }

    // 아무것도 없을 때
    BackHandler(enabled = !showCommentSheet && selectedPostForPopup == null) {
        onBackClick()
    }


    Scaffold(
        containerColor = BackGroudColor,
        topBar = {
            TopBar(
                onBackClick = onBackClick,
                text = topBarTitle,
                isMenu = false,
                insteadMenuComponent = {
                    // 🔹 전체를 감싸는 Box (아이콘 + 드롭다운 메뉴용)
                    Box {
                        // 🔹 아이콘과 텍스트가 놓일 기준점 (40.dp 고정)
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. 필터 아이콘 버튼
                            IconButton(onClick = {
                                showFilterMenu = !showFilterMenu
                                filterSubMenu = "MAIN"
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "필터",
                                    tint = if (uiState.filterState.type == FilterType.ALL)
                                        WhiteTextColor else PointColor
                                )
                            }

                            // 2. [추가] 지역 필터 텍스트 (아이콘 밑에 작게 표시)
                            val filter = uiState.filterState
                            if (filter.type == FilterType.CUSTOM_LOCATION) {
                                val locationText = listOfNotNull(
                                    filter.city.takeIf { it.isNotEmpty() },
                                    filter.district.takeIf { it.isNotEmpty() },
                                    filter.dong.takeIf { it.isNotEmpty() }
                                ).joinToString(" ")

                                Text(
                                    text = locationText,
                                    color = PointColor,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Medium,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 4.dp) // 아이콘 바로 밑으로 살짝 내림
                                        .wrapContentWidth(
                                            align = Alignment.CenterHorizontally,
                                            unbounded = true // 🔹 글자가 길어져도 Box 크기를 키우지 않음
                                        )
                                        .background(
                                            PointColor.copy(alpha = 0.1f),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // 3. 필터 드롭다운 메뉴 본체
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF1A1A1A))
                                .widthIn(min = if (filterSubMenu == "CUSTOM") 300.dp else 200.dp)
                        ) {
                            if (filterSubMenu == "MAIN") {
                                Text(
                                    "어느 지역의 포스트를 볼까요?",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(16.dp)
                                )

                                DropdownMenuItem(
                                    text = {
                                        LocationMenuContent("🌍", "모든 지역", "전체 피드 보기",
                                            uiState.filterState.type == FilterType.ALL)
                                    },
                                    onClick = {
                                        viewModel.setFilter(FilterType.ALL)
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        LocationMenuContent("📍", "지역 직접 선택",
                                            getSelectedLocationText(uiState.filterState),
                                            uiState.filterState.type == FilterType.CUSTOM_LOCATION)
                                    },
                                    onClick = { filterSubMenu = "CUSTOM" }
                                )
                            } else {
                                CustomLocationPicker(
                                    currentAddress = addressUiState,
                                    districtList = districtList,
                                    dongList = dongList,
                                    isLoadingDistrict = isLoadingDistrict,
                                    isLoadingDong = isLoadingDong,
                                    onLoadDistricts = { code, name -> viewModel.loadDistricts(code, name) },
                                    onLoadDongs = { code, city, district -> viewModel.loadDongs(code, city, district) },
                                    onApply = { c, d, dg ->
                                        viewModel.setFilter(FilterType.CUSTOM_LOCATION, c, d, dg)
                                        showFilterMenu = false
                                    },
                                    onBack = { filterSubMenu = "MAIN" }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── [1] 상단 통계 영역 ──
            StatsHeader(
                stats = stats,
                selectedTab = selectedTab,
                onTabClick = { tab -> viewModel.onTabSelected(tab) }
            )

            if (uiState.isInitialLoading) {
                // ── 데이터를 불러오는 동안 보여줄 로딩 화면 ── 🔹
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PointColor)
                }
            } else {
                // ── [2] 게시물 그리드 영역 (데이터 로드가 완료되면 그리드 표시) ──
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.fetchUserPosts(targetUid, isInitial = true, forceRefresh = true) },
                    modifier = Modifier.weight(1f)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.posts, key = { it.postId }) { post ->
                            val cachedSnapshot = mapSnapshotsCache[post.postId]
                            val context = LocalContext.current
                            val density = LocalDensity.current

                            // 기준점: 팝업이 차지하는 95% 너비
                            val screenWidthPx = remember { context.resources.displayMetrics.widthPixels.toFloat() }
                            val popupWidthPx = screenWidthPx * 0.95f
                            val markerSizePx = with(density) { 64.dp.toPx() }

                            LaunchedEffect(post.postId) {
                                if (cachedSnapshot == null && post.runRecord != null) {
                                    val record = post.runRecord
                                    val centerLat = (record.course.minLat + record.course.maxLat) / 2
                                    val centerLng = (record.course.minLng + record.course.maxLng) / 2

                                    val maxDiff = maxOf(record.course.maxLat - record.course.minLat, record.course.maxLng - record.course.minLng)
                                    val dynamicZoom = when {
                                        maxDiff > 0.04 -> 14.0
                                        maxDiff > 0.015 -> 15.0
                                        maxDiff > 0.005 -> 16.0
                                        maxDiff > 0.002 -> 17.0
                                        else -> 18.0
                                    }

                                    val apiW = popupWidthPx.toInt().coerceAtMost(1024)
                                    val url = buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom.toInt(), apiW, apiW)

                                    // 코스 전체 경로 및 중단점 미리 계산 📍
                                    val computedPathPoints = record.course.locationPoints.map {
                                        latLngToPixel(
                                            it.locationPoint.latitude, it.locationPoint.longitude,
                                            centerLat, centerLng, dynamicZoom, popupWidthPx, popupWidthPx
                                        ) to it.stop
                                    }
                                    // 실제 위치 계산
                                    val newPositions = post.locationImages.filter { it.location != null }.associate { img ->
                                        img.url to latLngToPixel(img.location!!.latitude, img.location!!.longitude, centerLat, centerLng, dynamicZoom, popupWidthPx, popupWidthPx)
                                    }

                                    // 2. 경계 계산
                                    val pMin = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, popupWidthPx, popupWidthPx)
                                    val pMax = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, popupWidthPx, popupWidthPx)
                                    val newBounds = Rect(minOf(pMin.x, pMax.x), minOf(pMin.y, pMax.y), maxOf(pMin.x, pMax.x), maxOf(pMin.y, pMax.y))

                                    // 3. 슬롯 및 클로저 오프셋 계산 (popupWidthPx 기준)
                                    val slots = assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
                                    val newCloserOffsets = slots.entries.associate { (postImage, slot) ->
                                        val orig = newPositions[postImage.url]!!
                                        postImage.url to calculateCloserOffset(
                                            orig.x, orig.y, slot, popupWidthPx, popupWidthPx,
                                            newBounds.left, newBounds.right, newBounds.top, newBounds.bottom, markerSizePx
                                        )
                                    }

                                    viewModel.saveMapSnapshot(
                                        post.postId,
                                        MapSnapshot(
                                            staticMapUrl = url,
                                            pathPoints = computedPathPoints,
                                            startPoint = computedPathPoints.firstOrNull()?.first,
                                            endPoint = computedPathPoints.lastOrNull()?.first,
                                            markerPositions = newPositions,
                                            closerOffsets = newCloserOffsets,
                                            courseBounds = newBounds
                                        )
                                    )
                                }
                            }

                            PostThumbnail(
                                post = post,
                                myUid = myUid,
                                snapshot = cachedSnapshot,
                                fullScreenWidthPx = popupWidthPx,
                                onClick = { viewModel.selectPost(post) }
                            )
                        }

                        // 조건: 로딩 중이 아니고, 마지막 페이지가 아닐 때만 노출
                        if (!uiState.isLastPage && uiState.posts.isNotEmpty()) {
                            item(span = { GridCells.Fixed(2).let { androidx.compose.foundation.lazy.grid.GridItemSpan(2) } }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoading) {
                                        // 버튼 대신 로딩 인디케이터 표시
                                        CircularProgressIndicator(color = PointColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        // 더보기 버튼 UI
                                        Surface(
                                            modifier = Modifier
                                                .clickable { viewModel.fetchUserPosts(targetUid, isInitial = false) }, // 추가 로드 호출
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "더보기 ▾",
                                                color = Color.Gray,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // [LAYER 2] 게시물 상세 오버레이 (Dialog 대체)
    androidx.compose.animation.AnimatedVisibility(
        visible = livePopupPost != null,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        if (livePopupPost != null) {
            val postId = livePopupPost.postId

            // 팝업에 필요한 비트맵만 쏙쏙 골라내기 ✂️
            val relevantLocationBitmaps = remember(postId, locationMarkerCache) {
                livePopupPost.locationImages.mapNotNull { img ->
                    locationMarkerCache[img.url]?.let { img.url to it }
                }.toMap()
            }
            val relevantCommonBitmaps = remember(postId, fullImageCache) {
                livePopupPost.commonImages.mapNotNull { img ->
                    fullImageCache[img.url]?.let { img.url to it }
                }.toMap()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { viewModel.selectPost(null) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight().clickable(enabled = false) {},
                    color = BackGroudColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    PostItem(
                        post = livePopupPost,
                        maxWidthPx = popupWidthPx,
                        mapSnapShots = mapSnapshotsCache[livePopupPost.postId],
                        authorProfileBitmap = profileCache[livePopupPost.authorProfileUrl],
                        locationBitmaps = relevantLocationBitmaps, // 👈 필터링된 맵 전달
                        commonImageBitmaps = relevantCommonBitmaps, // 👈 필터링된 맵 전달
                        onLikeClick = { viewModel.onLikeClick(livePopupPost.postId) },
                        onFollowClick = {
                            viewModel.onFollowClick(livePopupPost.postId) { readyPost ->

                                // 1. runRecord가 null이면 바로 종료 (Safe Call + Return)
                                val runRecord = readyPost.runRecord ?: return@onFollowClick

                                // 2. course와 좌표 데이터 추출
                                val course = runRecord.course
                                val locationNodes = course.locationPoints

                                // 3. 좌표가 하나도 없으면 진행할 의미가 없으니 체크!
                                if (locationNodes.isEmpty()) {
                                    Log.e("RUNUP_DEBUG", "코스 좌표 데이터가 비어있습니다.")
                                    return@onFollowClick
                                }

                                // 4. 좌표 변환 (Node -> GeoPoint)
                                val pathPoints = locationNodes.map { it.locationPoint }

                                // 5. 중심점 계산 (Bounding Box 중앙값)
                                val centerLat = if (course.minLat != 0.0) (course.minLat + course.maxLat) / 2.0 else pathPoints.first().latitude
                                val centerLng = if (course.minLng != 0.0) (course.minLng + course.maxLng) / 2.0 else pathPoints.first().longitude

                                // 6. Path 객체 생성 (도메인 모델에 맞춰서)
                                val extractedPath = com.example.runup.domain.model.Path( // 패키지명 주의
                                    distance = course.distance,
                                    points = pathPoints,
                                    centerPoint = GeoPoint(centerLat, centerLng)
                                )

                                // 7. [핵심] HomeViewModel(MainViewModel)로 데이터 전달 📍
                                // UserPostScreen이 MainViewModel을 주입받거나, 콜백으로 던져줘야 합니다.
                                mainViewModel.setCourseFromCommunity(
                                    path = extractedPath,
                                    authorName = livePopupPost.authorName
                                )

                                // 8. 상세 팝업을 닫고 메인(홈) 화면으로 복귀
                                viewModel.selectPost(null)
                                onFollowClick() // 이 함수가 호출되면 홈 화면으로 돌아가겠죠?
                            }
                        },
                        onCommentClick = {
                            // 댓글창 열기 (동시에 postId를 별도로 관리할 필요 없이 livePopupPost 사용)
                            showCommentSheet = true
                        },
                        onDeletePost = { viewModel.deletePost(livePopupPost); viewModel.selectPost(null) },
                        onImageClick = { enlargedImageUri = it },
                        onSaveMapSnapshot = { viewModel.saveMapSnapshot(livePopupPost.postId, it) },
                        onProfileClick = { clickedUid ->
                            selectedMiniProfileId = clickedUid
                        }
                    )
                }
            }
        }
    }



    // [LAYER 3] 댓글 바텀 시트
    if (showCommentSheet && livePopupPost != null) {
        LaunchedEffect(livePopupPost.postId) {
            viewModel.observeComments(livePopupPost.postId)
            viewModel.fetchPostDetail(livePopupPost.postId)
        }

        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = sheetState,
            containerColor = BackGroudColor,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false  // 백버튼 dismiss를 ModalBottomSheet에게 맡기지 않음
            )
        ) {
            CommentBottomSheet(
                postId = livePopupPost.postId,
                comments = uiState.comments, // 🔹 UserPostViewModel의 댓글 리스트
                onAddComment = { content ->
                    viewModel.addComment(livePopupPost.postId, content)
                },
                onDeleteComment = { pId, cId ->
                    viewModel.deleteComment(pId, cId)
                },
                bitmapCache = profileCache,
                onPostClick = { clickedUid ->
                    showCommentSheet = false      // 1. 일단 댓글창을 닫는다.
                    viewModel.selectPost(null)   // 2. 혹시 상세 팝업이 떠있다면 그것도 닫는다.
                    onNavigateToUser(clickedUid)  // 3. 해당 유저 페이지로 이동한다! 🏃‍♂️
                },
                onDismiss = { showCommentSheet = false },
            )
        }
    }

    // 확대할 이미지가 생겼을 때, 캐시에서 미리 비트맵을 찾아둠
    val displayBitmap = remember(enlargedImageUri) {
        enlargedImageUri?.let { uri ->
            // 원본 캐시 확인 -> 썸네일 캐시 확인 순서
            fullImageCache[uri + "_full"] ?: fullImageCache[uri] ?: locationMarkerCache[uri]
        }
    }
    // 3. 다이얼로그 호출 (이제 필요한 값만 넘김)
    if (enlargedImageUri != null) {
        EnlargedImageDialog(
            imageUrl = enlargedImageUri!!,
            displayBitmap = displayBitmap,
            onDismiss = { enlargedImageUri = null }
        )
    }

    if (selectedMiniProfileId != null) {
        ProfileMiniPopup(
            userId = selectedMiniProfileId!!,
            onDismiss = { selectedMiniProfileId = null },
            onViewPosts = { uid ->
                selectedMiniProfileId = null // 팝업 닫기

                // 만약 현재 보고 있는 페이지의 주인과 다른 사람이라면 이동
                if (uid != targetUid) {
                    viewModel.selectPost(null) // 상세 팝업도 닫아주기
                    onNavigateToUser(uid)
                }
            }
        )
    }
}

@Composable
fun StatsHeader(
    stats: UserTotalStats,
    selectedTab: String,
    onTabClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .background(Color.White.copy(0.03f), RoundedCornerShape(16.dp))
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Posts", stats.totalPosts.toString(), selectedTab == "POSTS") { onTabClick("POSTS") }
        StatItem("Likes", stats.totalLikes.toString(), selectedTab == "HEARTS") { onTabClick("HEARTS") }
        StatItem("Comments", stats.totalComments.toString(), selectedTab == "COMMENTS") { onTabClick("COMMENTS") }
        StatItem("Follows", stats.totalFollows.toString(), selectedTab == "FOLLOWS") { onTabClick("FOLLOWS")}
    }
}

@Composable
fun PostThumbnail(
    post: Post,
    myUid: String?,
    snapshot: MapSnapshot?,
    fullScreenWidthPx: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // 🔹 이미지 로드 상태 (키를 postId로 주어 재사용 방지)
    var isImageLoaded by remember(post.postId) { mutableStateOf(false) }

    // 내가 좋아요를 눌렀는지 확인
    val isLiked = remember(post.likedBy, myUid) {
        myUid != null && post.likedBy.contains(myUid)
    }

    // ── 🔹 내가 이 코스를 팔로우(따라뛰기) 중인지 확인 ── 📍
    val isFollowed = remember(post.followedBy, myUid) {
        myUid != null && post.followedBy.contains(myUid)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (snapshot != null) {
                // ── [1] 지도 이미지 (실제 로드 대상) ──
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(snapshot.staticMapUrl)
                        .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                        .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                        .crossfade(true) // 🔹 자체 페이드 효과 사용
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // 🔹 성공 시 상태 변경!
                    onSuccess = { isImageLoaded = true },
                    onError = { Log.e("MapError", "Image load failed for ${post.postId}") }
                )

                // ── [2] 경로(Polyline) 그리기 ──
                // 이미지가 로드된 후에만 선을 그립니다.
                if (isImageLoaded && post.runRecord != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 🔹 계산된 popupWidthPx(상세창 기준)와 현재 썸네일 크기의 비율 계산
                        val scale = size.width / fullScreenWidthPx

                        val path = Path().apply {
                            snapshot.pathPoints.forEachIndexed { i, (currentPos, isStop) ->
                                // 스케일 적용 📍
                                val scaledPos = Offset(currentPos.x * scale, currentPos.y * scale)

                                if (i == 0) {
                                    moveTo(scaledPos.x, scaledPos.y)
                                } else {
                                    val (prevPos, prevStop) = snapshot.pathPoints[i - 1]
                                    if (prevStop) {
                                        moveTo(scaledPos.x, scaledPos.y) // 끊겼으면 점프
                                    } else {
                                        lineTo(scaledPos.x, scaledPos.y) // 안 끊겼으면 선 긋기
                                    }
                                }
                            }
                        }
                        // 그리기 (계산 로직이 없어 매우 가벼움!)
                        drawPath(path, Color.Black, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawPath(path, PointColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                // ── [3] 로딩 바 (이미지 뒤에 배치) ──
                if (!isImageLoaded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PointColor.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                // 스냅샷 계산 자체가 안 된 경우
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PointColor.copy(alpha = 0.3f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }

            // ── [4] 정보 오버레이 (주소, 좋아요/댓글, 날짜) ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
                    .padding(8.dp)
            ) {
                // 1. 풀주소
                Text(
                    text = "${post.city} ${post.district} ${post.dong}".trim().ifEmpty { "Location" },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 2. 좋아요 & 댓글
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,tint = if (isLiked) Color.Red else WhiteTextColor, modifier = Modifier.size(10.dp))
                        Text(" ${post.likes}", color = Color.White, fontSize = 9.sp)
                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "댓글",
                            tint = WhiteTextColor, // 좋아요의 기본 색상과 통일
                            modifier = Modifier.size(10.dp) // 크기 24dp로 고정
                        )
                        Text(" ${post.commentCount}", color = Color.White, fontSize = 9.sp)
                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = if (isFollowed) PointColor else WhiteTextColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(" ${post.followCount}", color = Color.White, fontSize = 9.sp)
                    }

                    // 3. 날짜
                    Text(
                        text = formatTimestamp(post.timestamp),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (isSelected) WhiteTextColor else Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = if (isSelected) PointColor else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // 🔹 현재 선택된 탭 아래에 작은 점이나 선 표시
        if (isSelected) {
            Box(Modifier.padding(top=4.dp).size(4.dp).background(PointColor, CircleShape))
        }
    }
}