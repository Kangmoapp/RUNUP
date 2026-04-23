package com.example.runup.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.runup.domain.model.MarkerSlot
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.PostImage
import com.example.runup.ui.components.TopBar
import com.example.runup.ui.theme.BackGroudColor
import com.example.runup.ui.theme.WhiteTextColor
import com.example.runup.viewmodel.CommunityViewModel
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.request.ImageRequest
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.runup.BuildConfig
import com.example.runup.data.cityList
import com.example.runup.domain.model.AddressModel
import com.example.runup.ui.theme.PointColor
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
import com.example.runup.viewmodel.FilterType
import com.example.runup.viewmodel.MapSnapshot

import com.google.firebase.firestore.GeoPoint
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LineCap
import com.naver.maps.map.compose.LineJoin
import com.naver.maps.map.compose.MapEffect
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapType
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerDefaults
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PolylineOverlay
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.OverlayImage
import okhttp3.OkHttpClient
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBackClick: () -> Unit, // 뒤로가기 클릭
    onPostClick: (String) -> Unit, // 포스트 클릭
    onUploadClick: () -> Unit, // 업로드 버튼 클릭
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val communityState by viewModel.communityUiState.collectAsState()
    val addressUiState by viewModel.addressUiState.collectAsState()

    // 🔹 [수정] 다이얼로그 대신 메뉴 관련 상태로 변경
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterSubMenu by remember { mutableStateOf("MAIN") } // "MAIN", "MY", "CUSTOM"

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
    )

    var enlargedImageUri by remember { mutableStateOf<String?>(null) }

    // 처음 진입 시 데이터 호출
    LaunchedEffect(Unit) {
        viewModel.fetchPosts(isInitial = true) // 포스트 불러옴
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

    BackHandler { onBackClick() }

    Scaffold(
        containerColor = BackGroudColor,
        topBar = {
            TopBar(
                onBackClick = onBackClick,
                text = "커뮤니티",
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
                                            // 🔹 [핵심] 텍스트가 아무리 길어져도 Box의 크기에 영향을 주지 않도록 wrapContentWidth 사용
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

                            // 🔹 이제 이 Spacer는 항상 '40.dp 아이콘' 바로 옆에서 시작됩니다.
                            Spacer(Modifier.width(8.dp))

                            // 3. 글쓰기 아이콘
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
                                .widthIn(min = if (filterSubMenu == "CUSTOM") 300.dp else 180.dp)
                        ) {
                            when (filterSubMenu) {
                                "MAIN" -> {
                                    // 🔹 전체 포스트
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (communityState.filterState.type == FilterType.ALL)
                                                                PointColor.copy(alpha = 0.15f)
                                                            else
                                                                Color.White.copy(alpha = 0.05f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("🌍", fontSize = 14.sp)
                                                }
                                                Column {
                                                    Text(
                                                        "전체 포스트",
                                                        color = if (communityState.filterState.type == FilterType.ALL)
                                                            PointColor else WhiteTextColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (communityState.filterState.type == FilterType.ALL)
                                                            FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        "모든 지역 피드",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                if (communityState.filterState.type == FilterType.ALL) {
                                                    Spacer(Modifier.weight(1f))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(PointColor)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setFilter(FilterType.ALL)
                                            showFilterMenu = false
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    // 🔹 지역 직접 선택
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (communityState.filterState.type == FilterType.CUSTOM_LOCATION)
                                                                PointColor.copy(alpha = 0.15f)
                                                            else
                                                                Color.White.copy(alpha = 0.05f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("📍", fontSize = 14.sp)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "지역 직접 선택",
                                                        color = if (communityState.filterState.type == FilterType.CUSTOM_LOCATION)
                                                            PointColor else WhiteTextColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (communityState.filterState.type == FilterType.CUSTOM_LOCATION)
                                                            FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                    // 현재 선택된 지역 표시
                                                    val currentFilter = communityState.filterState
                                                    if (currentFilter.type == FilterType.CUSTOM_LOCATION) {
                                                        Text(
                                                            text = listOfNotNull(
                                                                currentFilter.city.takeIf { it.isNotEmpty() },
                                                                currentFilter.district.takeIf { it.isNotEmpty() },
                                                                currentFilter.dong.takeIf { it.isNotEmpty() }
                                                            ).joinToString(" "),
                                                            color = PointColor.copy(alpha = 0.7f),
                                                            fontSize = 10.sp,
                                                            maxLines = 1
                                                        )
                                                    } else {
                                                        Text(
                                                            "시/도 · 구/군 · 동 선택",
                                                            color = Color.Gray,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                                Text(
                                                    ">",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        },
                                        onClick = { filterSubMenu = "CUSTOM" },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Spacer(Modifier.height(4.dp))
                                }

                                "CUSTOM" -> {
                                    CustomLocationPicker(
                                        currentAddress = addressUiState,
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
            val mapSnapshots by viewModel.mapSnapshotCache.collectAsState()
            val thumbnails by viewModel.thumbnailCache.collectAsState()
            val fullBitmaps by viewModel.fullBitmapCache.collectAsState()

            // 메인 콘텐츠: 초기 로딩이 끝났을 때만 보여줌
            if (!communityState.isInitialLoading) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(communityState.posts, key = { it.postId }) { post ->
                        val snapshot = mapSnapshots[post.postId]
                        val authorBitmap = thumbnails[post.authorProfileUrl]

                        PostItem(
                            post = post,
                            cachedSnapshot = snapshot,     // ✅ 이 포스트용 지도 정보
                            authorBitmap = authorBitmap,   // ✅ 이 작성자 프로필 사진
                            thumbnailCache = thumbnails,    // ✅ 마커용 썸네일들
                            fullBitmapCache = fullBitmaps,  // ✅ 페이저용 원본들
                            onLikeClick = { viewModel.onLikeClick(post.postId) },
                            onPostClick = { onPostClick(post.postId) },
                            onDeletePost = { viewModel.deletePost(post) },
                            onImageClick = { url -> enlargedImageUri = url },
                            onSaveSnapshot = { viewModel.saveMapSnapshot(post.postId, it) }
                        )
                    }

                    // 추가 로딩 바
                    if (communityState.isLoading) {
                        item {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
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
    // 🔹 [여기!] Scaffold가 끝나고, 함수가 닫히기 직전에 이 블록을 넣으세요.
    if (enlargedImageUri != null) {
        EnlargedImageDialog(
            imageUrl = enlargedImageUri!!,
            onDismiss = { enlargedImageUri = null } // 닫을 때 다시 null로 초기화하여 화면에서 제거
        )
    }
}

@OptIn(ExperimentalNaverMapApi::class, ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    cachedSnapshot: MapSnapshot?,         // 👈 추가
    authorBitmap: Bitmap?,                // 👈 추가
    thumbnailCache: Map<String, Bitmap>,   // 👈 추가
    fullBitmapCache: Map<String, Bitmap>,  // 👈 추가
    onLikeClick: () -> Unit,
    onPostClick: () -> Unit,              // 👈 onClick을 더 명확하게
    onDeletePost: () -> Unit,             // 👈 삭제 콜백
    onImageClick: (String) -> Unit,
    onSaveSnapshot: (MapSnapshot) -> Unit // 👈 계산된 지도 저장 콜백
) {
    // 삭제 메뉴 상태 및 본인 확인
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isMyPost = post.authorId == currentUserId

    // 페이저 상태 관리 (총 페이지 수 = 지도(1) + 일반 이미지 개수)
    val totalPages = 1 + post.commonImages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val density = LocalDensity.current
    val context = LocalContext.current

    // 🔹 BoxWithConstraints 대신 화면 폭을 기준으로 픽셀값 미리 계산 (스크롤 성능 핵심)
    val screenWidthPx = remember { context.resources.displayMetrics.widthPixels.toFloat() }
    val markerSizePx = with(density) { 64.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // --- [1] 유저 헤더 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (authorBitmap != null) {
                    Image(bitmap = authorBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("👤", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (isMyPost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                    ) {
                        Text("MY", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, "더보기", tint = WhiteTextColor.copy(alpha = 0.7f))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF2C2C2C))) {
                            DropdownMenuItem(text = { Text("게시글 삭제", color = Color.Red) }, onClick = { onDeletePost(); showMenu = false })
                        }
                    }
                }
            }
        }

        // --- [2] 메인 콘텐츠 (페이저) ---
        Box(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                // HorizontalPager의 page == 0 내부
                if (page == 0) {
                    var isMapLoaded by remember(post.postId) { mutableStateOf(false) }
                    post.runRecord?.let { record ->
                        // 🔹 1. BoxWithConstraints 대신 화면의 가로 픽셀을 직접 가져옵니다.
                        val context = LocalContext.current
                        val density = LocalDensity.current
                        val screenWidthPx = remember { context.resources.displayMetrics.widthPixels.toFloat() }
                        val markerSizePx = with(density) { 64.dp.toPx() }

                        // 🔹 2. 일반 Box 사용 (성능 최적화)
                        Box(modifier = Modifier.fillMaxSize()) {

                            LaunchedEffect(post.postId) {
                                if (cachedSnapshot == null) {
                                    val centerLat = (record.course.minLat + record.course.maxLat) / 2
                                    val centerLng = (record.course.minLng + record.course.maxLng) / 2

                                    val latDiff = record.course.maxLat - record.course.minLat
                                    val lngDiff = record.course.maxLng - record.course.minLng
                                    val maxDiff = maxOf(latDiff, lngDiff)
                                    val dynamicZoom = when {
                                        maxDiff > 0.04 -> 14.0
                                        maxDiff > 0.015 -> 15.0
                                        maxDiff > 0.005 -> 16.0
                                        maxDiff > 0.002 -> 17.0
                                        else -> 18.0
                                    }

                                    // 윤석님이 성공하신 'apiW = 화면폭' 로직 그대로 유지
                                    val apiW = screenWidthPx.toInt().coerceAtMost(1024)
                                    val apiH = apiW

                                    val url = buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom.toInt(), apiW, apiH)

                                    // 좌표 계산도 screenWidthPx(이전의 mapWidthPx) 기준으로 1:1 유지
                                    val pMin = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, screenWidthPx, screenWidthPx)
                                    val pMax = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, screenWidthPx, screenWidthPx)
                                    val newBounds = Rect(minOf(pMin.x, pMax.x), minOf(pMin.y, pMax.y), maxOf(pMin.x, pMax.x), maxOf(pMin.y, pMax.y))

                                    val newPositions = post.locationImages.filter { it.location != null }.associate { img ->
                                        img.url to latLngToPixel(img.location!!.latitude, img.location!!.longitude, centerLat, centerLng, dynamicZoom, screenWidthPx, screenWidthPx)
                                    }

                                    val slots = assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
                                    val newCloserOffsets = slots.entries.associate { (postImage, slot) ->
                                        val orig = newPositions[postImage.url]!!
                                        postImage.url to calculateCloserOffset(orig.x, orig.y, slot, screenWidthPx, screenWidthPx, newBounds.left, newBounds.right, newBounds.top, newBounds.bottom, markerSizePx)
                                    }

                                    onSaveSnapshot(MapSnapshot(url, newPositions, newCloserOffsets, newBounds))
                                }
                            }

                            // --- 지도 및 오버레이 그리기 영역 (동일) ---
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(cachedSnapshot?.staticMapUrl ?: "")
                                        .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                                        .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds,
                                    onSuccess = { isMapLoaded = true }
                                )

                                if (isMapLoaded && cachedSnapshot != null) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // 🔹 4. 그리기 로직에서도 mapWidthPx(화면 실제 크기)를 사용합니다.
                                        val centerLat =
                                            (record.course.minLat + record.course.maxLat) / 2
                                        val centerLng =
                                            (record.course.minLng + record.course.maxLng) / 2
                                        val latDiff = record.course.maxLat - record.course.minLat
                                        val lngDiff = record.course.maxLng - record.course.minLng
                                        val maxDiff = maxOf(latDiff, lngDiff)
                                        val dynamicZoom = when {
                                            maxDiff > 0.04 -> 14.0
                                            maxDiff > 0.015 -> 15.0
                                            maxDiff > 0.005 -> 16.0
                                            maxDiff > 0.002 -> 17.0
                                            else -> 18.0
                                        }
                                        val points = record.course.locationPoints.map {
                                            latLngToPixel(
                                                it.locationPoint.latitude,
                                                it.locationPoint.longitude,
                                                centerLat,
                                                centerLng,
                                                dynamicZoom,
                                                screenWidthPx,
                                                screenWidthPx
                                            )
                                        }

                                        val path = Path().apply {
                                            points.forEachIndexed { i, p ->
                                                if (i == 0) moveTo(p.x, p.y)
                                                else if (!record.course.locationPoints[i - 1].stop) lineTo(
                                                    p.x,
                                                    p.y
                                                )
                                                else moveTo(p.x, p.y)
                                            }
                                        }
                                        drawPath(
                                            path,
                                            Color.Black,
                                            style = Stroke(
                                                14f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                        drawPath(
                                            path,
                                            PointColor,
                                            style = Stroke(
                                                8f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )

                                        if (points.isNotEmpty()) {
                                            drawMarker(points.first(), Color(0xFF4CAF50), "START")
                                            drawMarker(points.last(), Color(0xFFF44336), "END")
                                        }

                                        cachedSnapshot.closerOffsets.forEach { (url, closerPos) ->
                                            val origPos = cachedSnapshot.markerPositions[url]
                                                ?: return@forEach
                                            drawLine(Color.Black.copy(0.8f), origPos, closerPos, 2f)
                                            drawCircle(Color.Black, 5f, origPos)
                                        }
                                    }

                                    cachedSnapshot.closerOffsets.forEach { (url, closerPos) ->
                                        val bitmap = thumbnailCache[url] ?: return@forEach
                                        Box(modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    (closerPos.x - markerSizePx / 2).toInt(),
                                                    (closerPos.y - markerSizePx / 2).toInt()
                                                )
                                            }
                                            .clickable { onImageClick(url) }) {
                                            PhotoMarkerFromBitmap(bitmap)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = PointColor,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- 일반 이미지 페이지 ---
                    val imageUrl = post.commonImages[page - 1].url
                    val bitmap = fullBitmapCache[imageUrl]
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier
                            .fillMaxSize()
                            .clickable { onImageClick(imageUrl) }, contentScale = ContentScale.Crop)
                    }
                }
            }

            // 페이지 인디케이터 (1/3)
            if (totalPages > 1) {
                Surface(color = Color.Black.copy(0.6f), shape = CircleShape, modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)) {
                    Text("${pagerState.currentPage + 1}/$totalPages", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        // --- [3] 하단 인디케이터 & 좋아요/댓글/주소 ---
        if (totalPages > 1) {
            Row(
                Modifier
                    .height(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { iteration ->
                    val color = if (pagerState.currentPage == iteration) WhiteTextColor else WhiteTextColor.copy(alpha = 0.3f)
                    val size = if (pagerState.currentPage == iteration) 8.dp else 6.dp
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(size)
                    )
                }
            }
        } else {
            // 이미지가 한 장일 때 지도와 하단 아이콘 사이 간격을 최소화하기 위한 여백
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 🔹 좋아요/댓글/주소 영역 (윤석님의 원본 레이아웃 복구)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 16.dp, top = 0.dp, bottom = 4.dp), // top을 0으로 해서 위로 밀착
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 좋아요 섹션 (배지 스타일)
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onLikeClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.likes > 0) Color.Red else WhiteTextColor,
                    modifier = Modifier.size(24.dp)
                )
                if (post.likes > 0) {
                    Text(
                        text = "${post.likes}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 2.dp) // 우상단 45도 위치
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 댓글 섹션 (배지 스타일)
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onPostClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = "💬", fontSize = 18.sp)
                if (post.commentCount > 0) {
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 2.dp) // 우상단 45도 위치
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // 주소를 오른쪽으로 밀어냄

            // 3. 주소 표시
            if (post.dong.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WhiteTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${post.city} ${post.district} ${post.dong}",
                        color = WhiteTextColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 🔹 3. 주소 표시 영역 (추가)
            Spacer(modifier = Modifier.weight(1f)) // 왼쪽 아이콘들을 밀어냄

            if (post.dong.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn, // 위치 아이콘 추가 시 가독성 상승
                        contentDescription = null,
                        tint = WhiteTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        // "북구 대현동" 형식으로 표시
                        text = "${post.city} ${post.district} ${post.dong}",
                        color = WhiteTextColor.copy(alpha = 0.5f), // 작은 글씨이므로 약간 연하게
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // --- [4] 본문 및 날짜 ---
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row {
                Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.content, color = WhiteTextColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(post.timestamp),
                color = WhiteTextColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

fun latLngToPixel(
    lat: Double,
    lng: Double,
    centerLat: Double,
    centerLng: Double,
    zoom: Double,
    mapWidth: Float,
    mapHeight: Float
): Offset {

    val scale = 2.0 // Static Map에서 scale=2 썼으니까 반드시 맞춰야 함
    val tileSize = 256.0 * scale
    val worldSize = tileSize * 2.0.pow(zoom)

    fun mercatorX(lng: Double): Double {
        return (lng + 180.0) / 360.0
    }

    fun mercatorY(lat: Double): Double {
        val sinLat = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        return 0.5 - ln((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)
    }

    val centerX = mercatorX(centerLng) * worldSize
    val centerY = mercatorY(centerLat) * worldSize

    val targetX = mercatorX(lng) * worldSize
    val targetY = mercatorY(lat) * worldSize

    val dx = (targetX - centerX).toFloat()
    val dy = (targetY - centerY).toFloat()

    return Offset(
        x = mapWidth / 2f + dx,
        y = mapHeight / 2f + dy
    )
}

fun assignSlots(
    images: List<PostImage>,
    centerLat: Double,
    centerLng: Double
): Map<PostImage, MarkerSlot> {
    val result = mutableMapOf<PostImage, MarkerSlot>()
    val unassignedImages = images.filter { it.location != null }.toMutableList()
    val availableSlots = MarkerSlot.entries.toMutableList()

    // 1단계: 각 사진을 사분면별로 그룹화 (우상, 우하, 좌하, 좌상)
    val quadrantGroups = mutableMapOf<MarkerSlot, MutableList<PostImage>>()
    MarkerSlot.entries.forEach { quadrantGroups[it] = mutableListOf() }

    unassignedImages.forEach { image ->
        val loc = image.location!!
        val dLat = loc.latitude - centerLat
        val dLng = loc.longitude - centerLng

        // 시계 방향 각도 계산 (12시 방향 0도 기준)
        var clockAngle = 90.0 - Math.toDegrees(Math.atan2(dLat, dLng))
        if (clockAngle < 0) clockAngle += 360.0
        if (clockAngle >= 360) clockAngle -= 360.0

        val preferredSlot = when {
            clockAngle in 0.0..90.0   -> MarkerSlot.TOP_RIGHT
            clockAngle in 90.0..180.0  -> MarkerSlot.BOTTOM_RIGHT
            clockAngle in 180.0..270.0 -> MarkerSlot.BOTTOM_LEFT
            else                       -> MarkerSlot.TOP_LEFT
        }
        quadrantGroups[preferredSlot]?.add(image)
    }

    // 2단계: 각 구역별로 '해당 슬롯'과 '사진' 사이의 거리를 계산하여 가장 가까운 것 배정
    quadrantGroups.forEach { (slot, candidateList) ->
        if (candidateList.isNotEmpty()) {
            // [핵심] 중심점이 아니라, 배정될 슬롯(구석)과 사진 사이의 거리 기준
            val bestMatch = candidateList.minBy { image ->
                calculateDistanceToSlot(image.location!!, slot, centerLat, centerLng)
            }
            result[bestMatch] = slot

            unassignedImages.remove(bestMatch)
            availableSlots.remove(slot)
        }
    }

    // 3단계: 남은 사진들을 남은 슬롯들과의 절대 거리가 가장 짧은 조합으로 매칭
    while (unassignedImages.isNotEmpty() && availableSlots.isNotEmpty()) {
        var minDistance = Double.MAX_VALUE
        var bestPair: Pair<PostImage, MarkerSlot>? = null

        for (image in unassignedImages) {
            for (slot in availableSlots) {
                val dist = calculateDistanceToSlot(image.location!!, slot, centerLat, centerLng)
                if (dist < minDistance) {
                    minDistance = dist
                    bestPair = image to slot
                }
            }
        }

        bestPair?.let { (image, slot) ->
            result[image] = slot
            unassignedImages.remove(image)
            availableSlots.remove(slot)
        } ?: break
    }

    return result
}

// 슬롯(화면 구석)과 사진 좌표 사이의 상대적 거리를 계산하는 보조 함수
private fun calculateDistanceToSlot(
    photoLoc: GeoPoint,
    slot: MarkerSlot,
    centerLat: Double,
    centerLng: Double
): Double {
    // 슬롯의 가상 좌표 설정 (중심에서 충분히 멀리 떨어진 구석 지점)
    // dLat, dLng의 부호만 중요하므로 방향성을 부여합니다.
    val targetLat = when (slot) {
        MarkerSlot.TOP_RIGHT, MarkerSlot.TOP_LEFT -> centerLat + 1.0 // 위쪽
        MarkerSlot.BOTTOM_RIGHT, MarkerSlot.BOTTOM_LEFT -> centerLat - 1.0 // 아래쪽
    }
    val targetLng = when (slot) {
        MarkerSlot.TOP_RIGHT, MarkerSlot.BOTTOM_RIGHT -> centerLng + 1.0 // 오른쪽
        MarkerSlot.BOTTOM_LEFT, MarkerSlot.TOP_LEFT -> centerLng - 1.0 // 왼쪽
    }

    val dLat = photoLoc.latitude - targetLat
    val dLng = photoLoc.longitude - targetLng
    return Math.sqrt(dLat * dLat + dLng * dLng)
}

// 비트맵 사진 마커 씌우는 컴포저블
@Composable
fun PhotoMarkerFromBitmap(bitmap: Bitmap) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 전체를 감싸는 검은색 프레임
        Box(
            modifier = Modifier
                .size(64.dp) // 크기를 살짝 조절
                .background(Color.Black, shape = MaterialTheme.shapes.small)
                .padding(3.dp) // 검은색 테두리 두께
        ) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall), // 사진 끝을 살짝만 굴림
                contentScale = ContentScale.Crop
            )
        }
    }
}

// 클릭한 마커의 사진 원본 가져오기
@Composable
fun EnlargedImageDialog(imageUrl: String, onDismiss: () -> Unit, viewModel: CommunityViewModel = hiltViewModel()) {
    val fullCache by viewModel.fullBitmapCache.collectAsState()
    val thumbCache by viewModel.thumbnailCache.collectAsState()

    // 2. 백그라운드에서 프리로드했던 '원본용 비트맵'이 있는지 확인 (키: url + "_full")
    val displayBitmap = fullCache[imageUrl + "_full"] ?: fullCache[imageUrl] ?: thumbCache[imageUrl]

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        confirmButton = {},
        containerColor = Color.Black.copy(alpha = 0.9f),
        text = {
            Box(modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }) {
                if (displayBitmap != null) {
                    // [케이스 1] 이미 프리로드된 비트맵이 있다면 즉시 표시
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // [케이스 2] 아직 프리로드가 안 끝났다면 서버에서 로드
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    )
}

fun calculateCloserOffset(
    actualX: Float,
    actualY: Float,
    slot: MarkerSlot,
    mapWidthPx: Float,
    mapHeightPx: Float,
    courseMinX: Float,
    courseMaxX: Float,
    courseMinY: Float,
    courseMaxY: Float,
    markerSizePx: Float
): Offset {
    val TAG = "OFFSET_FIX"

    // 1. 출발점: 구석 (Corner)
    val margin = markerSizePx * 0.8f
    val cornerX = if (slot == MarkerSlot.TOP_RIGHT || slot == MarkerSlot.BOTTOM_RIGHT) mapWidthPx - margin else margin
    val cornerY = if (slot == MarkerSlot.BOTTOM_RIGHT || slot == MarkerSlot.BOTTOM_LEFT) mapHeightPx - margin else margin

    // 2. 방향 벡터: 구석(Corner) -> 실제 좌표(Actual)
    val dx = actualX - cornerX
    val dy = actualY - cornerY
    val totalDist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

    if (totalDist < 1f) return Offset(cornerX, cornerY)

    // 3. 코스 경계선 (벽)
    val padding = markerSizePx * 0.7f
    val boundLeft = (minOf(courseMinX, courseMaxX) - padding).coerceAtLeast(margin)
    val boundRight = (maxOf(courseMinX, courseMaxX) + padding).coerceAtMost(mapWidthPx - margin)
    val boundTop = (minOf(courseMinY, courseMaxY) - padding).coerceAtLeast(margin)
    val boundBottom = (maxOf(courseMinY, courseMaxY) + padding).coerceAtMost(mapHeightPx - margin)

    // 4. t 계산 (구석에서 실제 좌표로 얼마나 갈 수 있는가)
    var t = 1.0f

    if (dx > 0) {
        if (cornerX < boundLeft) {
            val tx = (boundLeft - cornerX) / dx
            t = minOf(t, tx.coerceAtLeast(0f))
        }
    } else if (dx < 0) {
        if (cornerX > boundRight) {
            val tx = (boundRight - cornerX) / dx
            t = minOf(t, tx.coerceAtLeast(0f))
        }
    }

    if (dy > 0) {
        if (cornerY < boundTop) {
            val ty = (boundTop - cornerY) / dy
            t = minOf(t, ty.coerceAtLeast(0f))
        }
    } else if (dy < 0) {
        if (cornerY > boundBottom) {
            val ty = (boundBottom - cornerY) / dy
            t = minOf(t, ty.coerceAtLeast(0f))
        }
    }

    // 5. 최종 거리 결정
    val finalDist = if (t == 1.0f) {
        val minGap = markerSizePx * 0.8f
        (totalDist - minGap)
    } else {
        totalDist * t
    }

    // 6. 결과 산출 및 지도 밖 이탈 방지 (추가됨)
    var finalX = cornerX + (dx / totalDist) * finalDist
    var finalY = cornerY + (dy / totalDist) * finalDist

    // 마커가 지도 픽셀 밖으로 나가지 않도록 제한 (마커 절반 크기만큼 여유)
    val markerMargin = markerSizePx *2 / 3f
    finalX = finalX.coerceIn(markerMargin, mapWidthPx - markerMargin)
    finalY = finalY.coerceIn(markerMargin, mapHeightPx - markerMargin)

    Log.d(TAG, "Corner:($cornerX,$cornerY) -> Final:($finalX,$finalY) t=$t")
    return Offset(finalX, finalY)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomLocationPicker(
    currentAddress: AddressModel?,
    onApply: (String, String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val districtList by viewModel.districtLocations.collectAsState()
    val dongList by viewModel.dongLocations.collectAsState()
    val isLoadingDistrict by viewModel.isLoadingDistrict.collectAsState()
    val isLoadingDong by viewModel.isLoadingDong.collectAsState()

    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var dong by remember { mutableStateOf("") }
    var currentParentCode by remember { mutableStateOf("") }

    // 🔹 현재 보여줄 탭 단계 (0=시도, 1=구군, 2=동)
    var activeStep by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .width(300.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔹 뒤로가기 버튼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("<", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))

            Text(
                "지역 선택",
                color = WhiteTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.weight(1f))

            // 🔹 내 위치 버튼
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PointColor.copy(alpha = 0.1f))
                    .clickable {
                        city = currentAddress?.city ?: ""
                        district = currentAddress?.district ?: ""
                        dong = currentAddress?.dong ?: ""
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("📍", fontSize = 9.sp)
                Text(
                    "내 위치",
                    color = PointColor,
                    fontSize = 11.sp
                )
            }
        }

        // ── 탭 헤더 3개 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // 시/도 탭
            LocationTab(
                label = city.ifEmpty { "시/도" },
                isActive = activeStep == 0,
                isSelected = city.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { activeStep = 0 }

            // 구/군 탭
            LocationTab(
                label = district.ifEmpty { "구/군" },
                isActive = activeStep == 1,
                isSelected = district.isNotEmpty(),
                enabled = city.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { if (city.isNotEmpty()) activeStep = 1 }

            // 동/읍/면 탭
            LocationTab(
                label = dong.ifEmpty { "동/읍/면" },
                isActive = activeStep == 2,
                isSelected = dong.isNotEmpty(),
                enabled = district.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { if (district.isNotEmpty()) activeStep = 2 }
        }

        // 탭 아래 구분선
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp
        )

        // ── 리스트 본문 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)  // 고정 높이
        ) {
            when (activeStep) {

                // 0단계: 시/도 목록
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(cityList) { (name, code) ->
                            LocationRowItem(
                                label = name,
                                isSelected = city == name
                            ) {
                                city = name
                                currentParentCode = code
                                district = ""
                                dong = ""
                                viewModel.loadDistricts(code, name)
                                activeStep = 1  // 자동으로 다음 탭
                            }
                        }
                    }
                }

                // 1단계: 구/군 목록
                1 -> {
                    if (isLoadingDistrict) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = PointColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(districtList) { vo ->
                                LocationRowItem(
                                    label = vo.lowestAdmName,
                                    isSelected = district == vo.lowestAdmName
                                ) {
                                    district = vo.lowestAdmName
                                    currentParentCode = vo.admCode
                                    dong = ""
                                    viewModel.loadDongs(vo.admCode, city, vo.lowestAdmName)
                                    activeStep = 2  // 자동으로 다음 탭
                                }
                            }
                        }
                    }
                }

                // 2단계: 동/읍/면 목록
                2 -> {
                    if (isLoadingDong) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = PointColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // 🔹 구/군 전체 옵션
                            item {
                                LocationRowItem(
                                    label = "전체",
                                    isSelected = dong == "전체"
                                ) {
                                    dong = "전체"
                                }
                            }
                            items(dongList) { vo ->
                                LocationRowItem(
                                    label = vo.lowestAdmName,
                                    isSelected = dong == vo.lowestAdmName
                                ) {
                                    dong = vo.lowestAdmName
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // ── 선택된 지역 태그 (기존 유지) ──
        if (city.isNotEmpty() || district.isNotEmpty() || dong.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (city.isNotEmpty()) LocationTag(city) {
                    city = ""; district = ""; dong = ""; activeStep = 0
                }
                if (district.isNotEmpty()) LocationTag(district) {
                    district = ""; dong = ""; activeStep = 1
                }
                if (dong.isNotEmpty()) LocationTag(dong) {
                    dong = ""
                }
            }
        }

        // ── 하단 버튼 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("뒤로", color = Color.Gray, fontSize = 13.sp)
            }
            Button(
                onClick = { onApply(city, district, dong) },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = PointColor),
                shape = RoundedCornerShape(8.dp),
                enabled = city.isNotEmpty()
            ) {
                Text("적용", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── 탭 컴포넌트 ──
@Composable
fun LocationTab(
    label: String,
    isActive: Boolean,
    isSelected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = when {
        isActive -> PointColor
        isSelected -> WhiteTextColor
        !enabled -> Color(0xFF555555)
        else -> Color.Gray
    }

    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        // 활성 탭 밑줄
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isActive) PointColor else Color.Transparent)
        )
    }
}

// ── 리스트 아이템 컴포넌트 ──
@Composable
fun LocationRowItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) PointColor.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) PointColor else WhiteTextColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PointColor)
            )
        }
    }
}

@Composable
fun LocationTag(text: String, onDelete: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = WhiteTextColor, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Add, // 'x' 아이콘이 적절하나 없으면 Add를 45도 돌려서 사용 가능
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(rotationZ = 45f)
                    .clickable { onDelete() }
            )
        }
    }
}
