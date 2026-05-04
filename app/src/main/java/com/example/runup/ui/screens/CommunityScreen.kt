package com.example.runup.ui.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.example.runup.ui.theme.PointColor
import com.example.runup.domain.model.FilterType
import com.example.runup.domain.model.ViewScope
import com.example.runup.ui.components.CommentBottomSheet
import com.example.runup.ui.components.CustomLocationPicker
import com.example.runup.ui.components.EnlargedImageDialog
import com.example.runup.ui.components.LocationMenuContent
import com.example.runup.ui.components.PostItem
import com.example.runup.ui.components.ProfileMiniPopup
import com.example.runup.ui.components.ScopeButton
import com.example.runup.ui.components.getSelectedLocationText
import com.example.runup.R
import com.example.runup.domain.model.Path
import com.example.runup.viewmodel.HomeViewModel
import com.example.runup.domain.model.GeoPoint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    onBackClick: () -> Unit,
    onUploadClick: () -> Unit,
    onFollowClick: () -> Unit,
    onAuthorProfileClick: (String) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel(),
    mainViewModel: HomeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val myUid = viewModel.myUid
    val communityState by viewModel.communityUiState.collectAsState() // 커뮤니티 Ui 상태
    val addressUiState by viewModel.addressUiState.collectAsState() // 현재 주소 상태

    var showFilterMenu by remember { mutableStateOf(false) } // 필터아이콘 클릭 -> 필터 메뉴
    var filterSubMenu by remember { mutableStateOf("MAIN") } // "MAIN", "CUSTOM"

    // postItem 요소 미리 계산해서 캐시에 저장
    val mapSnapshots by viewModel.mapSnapshotCache.collectAsState()
    val profilesCaches by viewModel.profileCache.collectAsState()
    val locationMarkerCaches by viewModel.locationMarkerCache.collectAsState()
    val fullImageCaches by viewModel.fullImageCache.collectAsState()

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
    )

    var enlargedImageUri by remember { mutableStateOf<String?>(null) }

    var selectedProfileId by remember { mutableStateOf<String?>(null) }

    // 바텀 시트 상태 추가
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostIdForComment by remember { mutableStateOf<String?>(null) }

    // 지역 필터링 상태 추가
    val districtList by viewModel.districtLocations.collectAsState()
    val dongList by viewModel.dongLocations.collectAsState()
    val isLoadingDistrict by viewModel.isLoadingDistrict.collectAsState()
    val isLoadingDong by viewModel.isLoadingDong.collectAsState()

    // 화면 너비 계산
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    LaunchedEffect(Unit) {
        viewModel.fetchPosts(isInitial = true)
    }

    // 스크롤 저장용 추가
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            viewModel.saveScrollState(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset
            )
        }
    }

    // 무한 스크롤 감지 (기존 유지)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= scrollState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !communityState.isLoading) {
            viewModel.fetchPosts(isInitial = false)
        }
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = {
            TopBar(
                onBackClick = onBackClick,
                titleContent = {
                    Image(
                        painter = painterResource(id = R.drawable.coursepick),
                        contentDescription = "CoursePick Logo",
                        modifier = Modifier
                            .height(28.dp) // 로고 높이를 고정하면 가로는 비율에 맞춰 자동으로 조절됩니다 🔹
                            .padding(bottom = 2.dp), // 시각적으로 중앙을 맞추기 위한 미세 조정
                        contentScale = ContentScale.Fit
                    )
                },
                text = "",
                isMenu = false,
                insteadMenuComponent = {
                    // 🔹 [수정] 아이콘 밑으로 늘어지는 메뉴 구조
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 🔹 필터 섹션: 가로/세로 크기를 아이콘 버튼에 딱 맞게 고정 (Anchor 역할)
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 1. 아이콘 버튼 (이 녀석이 Box의 주인이 되어 자리를 지킵니다)
                                IconButton(
                                    onClick = {
                                        showFilterMenu = !showFilterMenu
                                        filterSubMenu = "MAIN"
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "필터",
                                        tint = if (communityState.filterState.type == FilterType.ALL)
                                            WhiteTextColor else PointColor
                                    )
                                }

                                // 2. 지역 텍스트 (아이콘 자리에 영향을 주지 않도록 설정)
                                val filter = communityState.filterState
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
                                            .offset(y = 4.dp)
                                            // 텍스트가 아무리 길어져도 Box의 크기에 영향을 주지 않도록 wrapContentWidth
                                            .wrapContentWidth(
                                                align = Alignment.CenterHorizontally,
                                                unbounded = true
                                            )
                                            .background(
                                                PointColor.copy(alpha = 0.1f),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // 포스트 게시
                            Icon(
                                Icons.Default.Add, "글쓰기",
                                tint = WhiteTextColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onUploadClick() }
                            )
                        }

                        // ── 필터 메뉴 본체 ──
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF1A1A1A))
                                .widthIn(min = if (filterSubMenu == "CUSTOM") 300.dp else 280.dp)
                        ) {
                            when (filterSubMenu) {
                                "MAIN" -> {
                                    // 보기 범위
                                    Text(
                                        "누구의 포스트를 볼까요?",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 전체 / 친구 / 나 버튼 (ScopeSelector)
                                        ScopeButton("전체", ViewScope.ALL, communityState.filterState.scope, Modifier.weight(1f)) {
                                            viewModel.setViewScope(ViewScope.ALL)
                                        }
                                        ScopeButton("친구", ViewScope.FRIENDS, communityState.filterState.scope, Modifier.weight(1f)) {
                                            viewModel.setViewScope(ViewScope.FRIENDS)
                                        }
                                        ScopeButton("나", ViewScope.MINE, communityState.filterState.scope, Modifier.weight(1f)) {
                                            viewModel.setViewScope(ViewScope.MINE)
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                                    Spacer(Modifier.height(8.dp))

                                    // ── [SECTION 2] 지역 필터 (Where) ──
                                    Text(
                                        "어느 지역의 포스트를 볼까요?",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    // 모든 지역
                                    DropdownMenuItem(
                                        text = {
                                            LocationMenuContent("🌍", "모든 지역", "전국 피드 보기",
                                                communityState.filterState.type == FilterType.ALL)
                                        },
                                        onClick = {
                                            viewModel.setFilter(FilterType.ALL)
                                            showFilterMenu = false
                                        }
                                    )

                                    // 지역 직접 선택
                                    DropdownMenuItem(
                                        text = {
                                            LocationMenuContent("📍", "지역 직접 선택",
                                                getSelectedLocationText(communityState.filterState),
                                                communityState.filterState.type == FilterType.CUSTOM_LOCATION)
                                        },
                                        onClick = { filterSubMenu = "CUSTOM" }
                                    )
                                }
                                // 지역 직접 선택 -> 선택창 띄움
                                "CUSTOM" -> {
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
                }
            ) },
    ) { padding ->
        PullToRefreshBox(
            // 로딩 아이콘을 보여줄지 말지 ViewModel 상태에 맡깁니다.
            isRefreshing = communityState.isRefreshing,
            onRefresh = {
                // 위로 당기면 실행될 로직
                viewModel.fetchPosts(isInitial = true, forceRefresh = true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ){
// 🌟 1. 로딩 중일 때는 로딩 오버레이 표시
            if (communityState.isInitialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackGroudColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("다른 러너들과 연결 중입니다...", color = Color.White)
                    }
                }
            } else {
                // 🌟 2. 로딩이 끝났는데 데이터가 비어있다면? -> Empty View!
                if (communityState.posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()), // 당겨서 새로고침 가능
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🏃‍♂️💨", fontSize = 50.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("아직 러닝 기록이 없어요!", color = WhiteTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("이 지역의 첫 번째 러너가 되어 멋진 코스를 남겨보세요.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(communityState.posts, key = { it.postId }) { post ->
                            val snapshot = mapSnapshots[post.postId]
                            val authorBitmap = profilesCaches[post.authorProfileUrl]
                            // ── 🔹 [핵심 최적화] 내 포스트에 필요한 비트맵들만 필터링 ── 📍
                            val relevantLocationBitmaps =
                                remember(post.postId, locationMarkerCaches) {
                                    post.locationImages
                                        .mapNotNull { img -> locationMarkerCaches[img.url]?.let { img.url to it } }
                                        .toMap()
                                }

                            val relevantCommonBitmaps = remember(post.postId, fullImageCaches) {
                                post.commonImages
                                    .mapNotNull { img -> fullImageCaches[img.url]?.let { img.url to it } }
                                    .toMap()
                            }

                            PostItem(
                                post = post,
                                myUid = myUid,
                                maxWidthPx = screenWidthPx,
                                mapSnapShots = snapshot,     // ✅ 이 포스트용 지도 정보
                                authorProfileBitmap = authorBitmap,   // ✅ 이 작성자 프로필 사진
                                locationBitmaps = relevantLocationBitmaps,    // ✅ 마커용 썸네일들
                                commonImageBitmaps = relevantCommonBitmaps,  // ✅ 페이저용 원본들
                                onLikeClick = { viewModel.onLikeClick(post.postId) },
                                onFollowClick = {
                                    viewModel.onFollowClick(post.postId) { readyPost ->
                                        // 1. runRecord가 null이면 바로 종료 (Safe Call + Return) 📍
                                        val runRecord = readyPost.runRecord ?: return@onFollowClick

                                        // 2. 이제 runRecord는 non-null 상태입니다. course를 안전하게 가져옵니다.
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
                                        // min/max 값이 0.0이면 첫 좌표를 중심으로 사용
                                        val centerLat =
                                            if (course.minLat != 0.0) (course.minLat + course.maxLat) / 2.0 else pathPoints.first().latitude
                                        val centerLng =
                                            if (course.minLng != 0.0) (course.minLng + course.maxLng) / 2.0 else pathPoints.first().longitude

                                        // 6. Path 객체 생성
                                        val extractedPath = Path(
                                            distance = course.distance,
                                            points = pathPoints,
                                            centerPoint = GeoPoint(centerLat, centerLng)
                                        )

                                        // 7. HomeViewModel로 전달 및 화면 이동
                                        mainViewModel.setCourseFromCommunity(
                                            path = extractedPath,
                                            authorName = post.authorName
                                        )

                                        // communityscreen 의 onfollow click 메인(홈) 화면으로 복귀
                                        onFollowClick()
                                    }
                                },
                                onCommentClick = {
                                    selectedPostIdForComment = post.postId
                                    showCommentSheet = true
                                },
                                onDeletePost = { viewModel.deletePost(post) },
                                onImageClick = { url -> enlargedImageUri = url },
                                onSaveMapSnapshot = { viewModel.saveMapSnapshot(post.postId, it) },
                                onProfileClick = { selectedProfileId = post.authorId },
                            )
                        }

                        // 추가 로딩 바
                        if (communityState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp), contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 전체 화면 로딩 오버레이
            // 데이터 수신 + 비트맵 캐시 완료까지 이 화면이 유지됩니다.
            if (communityState.isInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackGroudColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("다른 러너들과 연결 중입니다...", color = Color.White)
                    }
                }
            }
        }
    }
    // 1. 댓글 바텀 시트 (윤석님이 넣어야 할 부분!)
    if (showCommentSheet && selectedPostIdForComment != null) {
        LaunchedEffect(selectedPostIdForComment) {
            viewModel.observeComments(selectedPostIdForComment!!)
        }

        ModalBottomSheet(
            onDismissRequest = {
                showCommentSheet = false
                selectedPostIdForComment = null
            },
            sheetState = sheetState,
            containerColor = BackGroudColor,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false
            )
        ) {
            CommentBottomSheet(
                postId = selectedPostIdForComment ?: "",
                myUid = myUid,
                comments = communityState.comments, // 🔹 CommunityViewModel의 데이터
                onAddComment = { content ->
                    viewModel.addComment(selectedPostIdForComment!!, content)
                },
                onDeleteComment = { postId, commentId ->
                    viewModel.deleteComment(postId, commentId)
                },
                bitmapCache = profilesCaches,
                onDismiss = { showCommentSheet = false },
                onPostClick = { uid -> onAuthorProfileClick(uid) }
            )
        }
    }

    // 확대할 이미지가 생겼을 때, 캐시에서 미리 비트맵을 찾아둠
    val displayBitmap = remember(enlargedImageUri) {
        enlargedImageUri?.let { uri ->
            // 원본 캐시 확인 -> 썸네일 캐시 확인 순서
            fullImageCaches[uri + "_full"] ?: fullImageCaches[uri] ?: locationMarkerCaches[uri]
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

    // 프로필 터치 -> 미니 팝업
    if (selectedProfileId != null) {
        ProfileMiniPopup(
            userId = selectedProfileId!!,
            onDismiss = { selectedProfileId = null },
            onViewPosts = { uid ->
                onAuthorProfileClick(uid)
                selectedProfileId = null // 이동 시 팝업 닫기
            }
        )
    }
}

fun buildNaverStaticMapUrl(
    centerLat: Double,
    centerLng: Double,
    zoom: Int,
    width: Int,
    height: Int
): String {
    return "https://maps.apigw.ntruss.com/map-static/v2/raster" +
            "?w=$width&h=$height" +
            "&center=$centerLng,$centerLat" +
            "&level=$zoom" +
            "&maptype=basic" +
            "&format=png" +
            "&scale=2"
}

// Canvas 내에서 마커 모양을 그리기 위한 함수
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarker(
    center: Offset,
    color: Color,
    text: String // "START" 또는 "END"
) {
    val markerRadius = 22f // 텍스트 공간을 위해 반지름을 살짝 키움
    val markerHeight = 55f // 높이도 살짝 조절

    // 1. 물방울 모양 (핀) 경로 정의
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - markerRadius, center.y - markerHeight + markerRadius)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center.x - markerRadius,
                center.y - markerHeight,
                center.x + markerRadius,
                center.y - markerHeight + (markerRadius * 2)
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(center.x, center.y)
        close()
    }

    // 2. 마커 몸체와 테두리 그리기
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    // 3. 마커 위에 텍스트 그리기 (Android Native Canvas 활용)
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        this.textSize = 12f
        this.isFakeBoldText = true
        this.textAlign = android.graphics.Paint.Align.CENTER
    }

    drawContext.canvas.nativeCanvas.drawText(
        text,
        center.x,
        center.y - markerHeight + (markerRadius * 1.3f), // 마커 머리 부분 중앙에 위치
        paint
    )
}