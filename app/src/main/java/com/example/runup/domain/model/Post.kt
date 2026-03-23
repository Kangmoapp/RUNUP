package com.example.runup.domain.model

data class Post(
    val postId: String = "",
    val authorName: String = "",
    val content: String = "",
    val images: List<String> = emptyList(),
    val likes: Int = 0,
    val commentCount: Int = 0,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val commentId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)