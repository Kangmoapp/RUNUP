package com.example.runup.domain.model

import com.google.firebase.firestore.GeoPoint

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfileUrl: String = "",
    val content: String = "",
    val locationImages: List<PostImage> = emptyList(),
    val commonImages: List<PostImage> = emptyList(),
    val runRecord: RunRecord? = null,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(), // 🔹 좋아요 누른 유저 ID 리스트
    val isLiked: Boolean = false,            // UI 판단용 (서버 저장 X)
    val commentCount: Int = 0,
    val comments: List<Comment> = emptyList(),
    val commentedBy: List<String> = emptyList(),
    val followCount: Int = 0,
    val followedBy: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val city: String = "",
    val district: String = "",
    val dong: String = "",
)

data class Comment(
    val commentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfileUrlMini: String = "",
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

data class UserActivityStats(
    val userName: String = "",
    val uploadPostIds: List<String> = emptyList(),
    val likePostIds: List<String> = emptyList(),
    val commentPostIds: List<String> = emptyList(),
    val followPostIds: List<String> = emptyList()
)
