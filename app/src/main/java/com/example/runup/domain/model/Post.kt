package com.example.runup.domain.model

import com.google.firebase.firestore.GeoPoint

data class Post(
    val postId: String = "",
    val authorName: String = "",
    val content: String = "",
    val locationImages: List<PostImage> = emptyList(),
    val commonImages: List<PostImage> = emptyList(),
    val runRecord: RunRecord? = null,
    val likes: Int = 0,
    val commentCount: Int = 0,
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = 0L
)

data class Comment(
    val commentId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

data class PostImage(
    val url: String = "",           // 원본 (확대 시 사용)
    val thumbnailUrl: String = "",  // 마커용 (매우 작음, 로딩 속도 핵심)
    val location: GeoPoint? = null
)

enum class MarkerSlot {
    TOP_RIGHT,    // 45도
    BOTTOM_RIGHT, // 135도
    BOTTOM_LEFT,  // 225도
    TOP_LEFT      // 315도
}
