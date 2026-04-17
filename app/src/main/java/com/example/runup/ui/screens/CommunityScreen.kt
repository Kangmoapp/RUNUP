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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.lerp
import coil.ImageLoader
import coil.request.ImageRequest
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.runup.BuildConfig
import com.example.runup.ui.util.mapper.TimeMapper.formatTimestamp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBackClick: () -> Unit, // 뒤로가기 클릭
    onPostClick: (String) -> Unit, // 포스트 클릭
    onUploadClick: () -> Unit, // 업로드 버튼 클릭
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val communityState by viewModel.communityUiState.collectAsState()

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedScrollOffset
    )

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
                    Icon(
                        Icons.Default.Add,
                        "글쓰기",
                        tint = WhiteTextColor,
                        modifier = Modifier.size(28.dp).clickable{onUploadClick()}
                    )
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ){
            // 메인 콘텐츠: 초기 로딩이 끝났을 때만 보여줌
            if (!communityState.isInitialLoading) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(communityState.posts, key = { it.postId }) { post ->
                        PostItem(
                            post = post,
                            onClick = { onPostClick(post.postId) },
                            onLikeClick = { viewModel.onLikeClick(post.postId) },
                            viewModel = viewModel
                        )
                    }

                    // 추가 로딩 바
                    if (communityState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.fillMaxSize().background(BackGroudColor),
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
}

@OptIn(ExperimentalNaverMapApi::class, ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    viewModel: CommunityViewModel // 1. ViewModel 추가
) {
    // 용도별 캐시 구독
    val mapSnapshotCache by viewModel.mapSnapshotCache.collectAsState()
    val thumbnailCache by viewModel.thumbnailCache.collectAsState()
    val fullBitmapCache by viewModel.fullBitmapCache.collectAsState()

    // 현재 포스트의 캐시 데이터 추출
    val cachedSnapshot = mapSnapshotCache[post.postId]

    // 상태 초기화 (캐시가 있으면 캐시값 사용)
    var markerPositions by remember(post.postId) {
        mutableStateOf(cachedSnapshot?.markerPositions ?: emptyMap())
    }
    var closerOffsets by remember(post.postId) {
        mutableStateOf(cachedSnapshot?.closerOffsets ?: emptyMap())
    }
    var courseBounds by remember(post.postId) {
        mutableStateOf(cachedSnapshot?.courseBounds)
    }

    // 작성자 프로필은 썸네일 캐시나 별도 로직 유지
    val authorBitmap = thumbnailCache[post.authorProfileUrl]
    var enlargedImageUri by remember { mutableStateOf<String?>(null) }

    // 삭제 메뉴 상태 및 본인 확인
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val isMyPost = post.authorId == currentUserId

    // 페이저 상태 관리 (총 페이지 수 = 지도(1) + 일반 이미지 개수)
    val totalPages = 1 + post.commonImages.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    var isCapturing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 유저 헤더 영역
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray), // 이미지가 없을 때나 로딩 전 기본 배경
                contentAlignment = Alignment.Center
            ) {
                if (authorBitmap != null) {
                    Image(
                        bitmap = authorBitmap.asImageBitmap(),
                        contentDescription = "작성자 프로필",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (post.authorProfileUrl.isNotEmpty()) {
                    // 캐시에 없는데 URL은 있다면 차선책으로 AsyncImage 실행
                    AsyncImage(
                        model = post.authorProfileUrl,
                        contentDescription = "작성자 프로필",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 사진이 아예 없는 경우 기본 아이콘
                    Text("👤", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            // --- [수정] 본인일 경우에만 삭제 메뉴 노출 ---
            if (isMyPost) {
                Row(
                    verticalAlignment = Alignment.CenterVertically // 아이콘과 배지 높이 맞춤
                ) {
                    // 1. [추가] "MY" 배지 표시
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f), // 은은한 노란색 배경 (골드)
                        shape = CircleShape, // 둥근 모양
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)), // 흐릿한 노란색 테두리
                        modifier = Modifier.padding(end = 4.dp) // 삭제 메뉴와 간격
                    ) {
                        Text(
                            text = "MY",
                            color = Color(0xFFFFD700), // 진한 노란색 글씨
                            fontSize = 10.sp, // 작게
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp) // 내부 여백
                        )
                    }

                    // 2. [기존 유지] 삭제 메뉴 (MoreVert 아이콘)
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp) // 아이콘 터치 영역 살짝 조절
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = WhiteTextColor.copy(alpha = 0.7f) // 아이콘은 살짝 연하게 처리해서 배지를 돋보이게 함
                            )
                        }

                        // 삭제 드롭다운 메뉴 (기존과 동일)
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF2C2C2C))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("게시글 삭제", color = Color.Red, fontWeight = FontWeight.Medium)
                                },
                                onClick = {
                                    viewModel.deletePost(post)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 지도 및 마커 오버레이 영역
        // --- 수정된 메인 콘텐츠 영역 (페이저 적용) ---
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f) // 정사각형 비율 유지
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // [페이지 0] 기존의 지도 및 마커 오버레이 영역
                    post.runRecord?.let { record ->
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val density = LocalDensity.current
                            val mapWidthPx = constraints.maxWidth.toFloat()
                            val mapHeightPx = constraints.maxHeight.toFloat()
                            val markerSizePx = with(density) { 64.dp.toPx() }

                            // 캐시가 없을 때만 무거운 계산 수행
                            LaunchedEffect(post.postId) {
                                if (cachedSnapshot == null) {
                                    val centerLat = (record.course.minLat + record.course.maxLat) / 2
                                    val centerLng = (record.course.minLng + record.course.maxLng) / 2

                                    // 줌 계산
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

                                    // URL 및 좌표 계산
                                    val url = buildNaverStaticMapUrl(centerLat, centerLng, dynamicZoom.toInt(), constraints.maxWidth, constraints.maxHeight)
                                    val pMin = latLngToPixel(record.course.minLat, record.course.minLng, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx)
                                    val pMax = latLngToPixel(record.course.maxLat, record.course.maxLng, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx)
                                    val newBounds = Rect(minOf(pMin.x, pMax.x), minOf(pMin.y, pMax.y), maxOf(pMin.x, pMax.x), maxOf(pMin.y, pMax.y))

                                    val newPositions = post.locationImages.filter { it.location != null }.associate { img ->
                                        img.url to latLngToPixel(img.location!!.latitude, img.location!!.longitude, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx)
                                    }

                                    // 슬롯 배정 및 최종 오프셋 계산
                                    val slots = assignSlots(post.locationImages.filter { it.location != null }, centerLat, centerLng)
                                    val newCloserOffsets = slots.entries.associate { (postImage, slot) ->
                                        val orig = newPositions[postImage.url]!!
                                        postImage.url to calculateCloserOffset(orig.x, orig.y, slot, mapWidthPx, mapHeightPx, newBounds.left, newBounds.right, newBounds.top, newBounds.bottom, markerSizePx)
                                    }

                                    // 상태 업데이트 및 저장
                                    markerPositions = newPositions
                                    closerOffsets = newCloserOffsets
                                    courseBounds = newBounds
                                    viewModel.saveMapSnapshot(post.postId, MapSnapshot(url, newPositions, newCloserOffsets, newBounds))
                                }
                            }

                            Box {
                                // 🔹 1. 지도 이미지 (캐시된 URL 우선)
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(cachedSnapshot?.staticMapUrl ?: /* URL 생성 로직 */ "")
                                        .addHeader("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_API_KEY)
                                        .addHeader("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_API_SECRET_KEY)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )

                                // 🔹 2. 경로 선 및 시작/종료 마커 그리기
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // (1) 경로 그리기
                                    val centerLat = (record.course.minLat + record.course.maxLat) / 2
                                    val centerLng = (record.course.minLng + record.course.maxLng) / 2

                                    // 캐시가 있든 없든 줌은 다시 계산해야 선이 그려집니다.
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
                                        latLngToPixel(it.locationPoint.latitude, it.locationPoint.longitude, centerLat, centerLng, dynamicZoom, mapWidthPx, mapHeightPx)
                                    }

                                    for (i in 0 until points.size - 1) {
                                        val currentNode = record.course.locationPoints[i]

                                        // 정지 상태(isStop)가 true라면 다음 점과 잇지 않고 건너뜀
                                        if (currentNode.stop) continue

                                        drawLine(
                                            color = Color.Black,
                                            start = points[i],
                                            end = points[i + 1],
                                            strokeWidth = 4f
                                        )
                                    }

                                    // (2) 시작/종료 마커 그리기
                                    if (points.isNotEmpty()) {
                                        drawMarker(center = points.first(), color = Color(0xFF4CAF50), text = "START")
                                        drawMarker(center = points.last(), color = Color(0xFFF44336), text = "END")
                                    }

                                    // (3) 사진 연결선 및 실제 점(Circle) 그리기
                                    closerOffsets.forEach { (url, closerPos) ->
                                        val origPos = markerPositions[url] ?: return@forEach
                                        drawLine(color = Color.Black.copy(alpha = 0.8f), start = origPos, end = closerPos, strokeWidth = 2f)
                                        drawCircle(color = Color.Black, radius = 5f, center = origPos)
                                    }
                                }

                                // 🔹 3. 사진 마커 (이미 계산된 closerOffsets 사용)
                                closerOffsets.forEach { (url, closerPos) ->
                                    val bitmap = thumbnailCache[url] ?: return@forEach
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset((closerPos.x - markerSizePx / 2).toInt(), (closerPos.y - markerSizePx / 2).toInt()) }
                                            .clickable { enlargedImageUri = url }
                                    ) {
                                        PhotoMarkerFromBitmap(bitmap = bitmap)
                                    }
                                }
                            }
                        }
                    } ?: run {
                        // 러닝 기록이 없는 경우에 대한 예외 처리 (검은 바탕 등)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                    }
                } else {
                    // [페이지 1 ~ N] commonImages 보여주기
                    val imageIndex = page - 1
                    val commonImage = post.commonImages[imageIndex]
                    val preloadedBitmap = fullBitmapCache[commonImage.url]
                    if (preloadedBitmap != null) {
                        Image(
                            bitmap = preloadedBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 아직 백그라운드 로딩이 안 끝났다면 일반 AsyncImage로 로딩 처리
                        AsyncImage(
                            model = commonImage.url, // 일반 이미지는 원본 URL 바로 사용
                            contentDescription = "Post Image ${imageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { enlargedImageUri = commonImage.url }, // 클릭 시 확대
                            contentScale = ContentScale.Crop // 영역에 맞게 자름
                        )
                    }
                }
            }

            // --- 상단 페이지 네비게이션 (예: 1/3) ---
            if (totalPages > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/$totalPages",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- 하단 인디케이터 (점) 영역 ---
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
            // 이미지가 한 장일 때는 인디케이터 대신 여백
            Spacer(modifier = Modifier.height(8.dp))
        }


        // 하단 좋아요/댓글 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // vertical을 0.dp로 하거나 아주 작게(2.dp) 조정하여 위아래 간격을 줄임
                .padding(start = 10.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 좋아요 섹션
            Box(
                modifier = Modifier
                    // 높이를 40.dp에서 32.dp 정도로 줄여서 위아래 여백을 제거
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onLikeClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.likes > 0) Color.Red else WhiteTextColor,
                    modifier = Modifier.size(24.dp) // 아이콘 크기를 살짝 줄여 더 밀착시킴
                )
                if (post.likes > 0) {
                    Text(
                        text = "${post.likes}",
                        color = WhiteTextColor,
                        fontSize = 10.sp, // 숫자 크기도 살짝 조절
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = -4.dp, y = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. 댓글 섹션
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clickable { onClick() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "💬",
                    fontSize = 18.sp, // 이모지 크기도 살짝 조절
                    modifier = Modifier.padding(bottom = 0.dp)
                )

                if (post.commentCount > 0) {
                    Text(
                        text = "${post.commentCount}",
                        color = WhiteTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = -4.dp, y = 2.dp)
                    )
                }
            }
        }

        // 내용 영역
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row {
                Text(post.authorName, color = WhiteTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.content, color = WhiteTextColor)
            }

            // [추가] 날짜 영역
            Spacer(modifier = Modifier.height(4.dp)) // 내용과 날짜 사이 간격
            Text(
                text = formatTimestamp(post.timestamp),
                color = WhiteTextColor.copy(alpha = 0.5f), // 날짜는 약간 흐릿하게
                fontSize = 12.sp
            )
        }
    }

    if (enlargedImageUri != null) {
        EnlargedImageDialog(imageUrl = enlargedImageUri!!, onDismiss = { enlargedImageUri = null })
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
            Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
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


