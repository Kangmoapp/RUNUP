package com.runit.runup.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.runit.runup.domain.model.Post
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePreloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 🔹 공통: URL 하나를 비트맵으로 로드
    suspend fun loadBitmap(url: String, size: Int): Bitmap? {
        if (url.isEmpty()) return null

        val request = ImageRequest.Builder(context)
            .data(url)
            .size(size)
            .allowHardware(false) // 비트맵 조작을 위해 hardware config 비활성화
            .build()

        val result = context.imageLoader.execute(request)
        return if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else {
            null
        }
    }

    // 🔹 선택사항: Post 리스트에서 모든 URL을 추출하는 유틸리티
    // ImagePreloader.kt 내부

    fun extractUrlsFromPosts(posts: List<Post>): Map<String, List<Any>> {
        // 1. [MARKER] 지도 마커용 (PostImage 객체 그대로 유지해서 url/thumbnailUrl 둘 다 활용)
        val markerImages = posts.flatMap { it.locationImages }.distinctBy { it.url }

        // 2. [AUTHOR] 게시글 작성자 프로필
        val authorUrls = posts.map { it.authorProfileUrl }.filter { it.isNotEmpty() }.distinct()

        // 3. [COMMENT] 댓글 작성자 프로필 (나중에 로드)
        val commentAuthorUrls = posts.flatMap { post ->
            post.comments.map { it.authorProfileUrlMini }
        }.filter { it.isNotEmpty() }.distinct()

        // 4. [FULL] 고해상도 이미지 URL
        val fullImages = (posts.flatMap { it.locationImages.map { it.url } } +
                posts.flatMap { it.commonImages.map { it.url } })
            .filter { it.isNotEmpty() }.distinct()

        return mapOf(
            "MARKER" to markerImages,
            "AUTHOR" to authorUrls,
            "COMMENT" to commentAuthorUrls,
            "FULL" to fullImages
        )
    }
}