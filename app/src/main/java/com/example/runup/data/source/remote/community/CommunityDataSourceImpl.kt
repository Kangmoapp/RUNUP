package com.example.runup.data.source.remote.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Comment
import com.example.runup.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CommunityDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    // 1. 게시글 목록 가져오기
    suspend fun getPosts(): AuthResult<List<Post>> {
        return try {
            val snapshot = firestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            val postList = snapshot.documents.mapNotNull { doc ->
                Post(
                    postId = doc.id,
                    authorName = doc.getString("authorName") ?: "익명",
                    content = doc.getString("content") ?: "",
                    images = doc.get("images") as? List<String> ?: emptyList(),
                    likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                    commentCount = (doc.get("commentCount") as? Number)?.toInt() ?: 0
                )
            }
            AuthResult.Success(postList)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "로드 실패")
        }
    }

    // 2. 게시글 상세 정보 가져오기
    suspend fun getPostById(postId: String): AuthResult<Post> {
        return try {
            val doc = firestore.collection("Posts").document(postId).get().await()
            if (doc.exists()) {
                val post = Post(
                    postId = doc.id,
                    authorName = doc.getString("authorName") ?: "익명",
                    content = doc.getString("content") ?: "",
                    images = doc.get("images") as? List<String> ?: emptyList(),
                    likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                    commentCount = (doc.get("commentCount") as? Number)?.toInt() ?: 0
                )
                AuthResult.Success(post)
            } else AuthResult.Fail("게시물 없음")
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "로드 실패")
        }
    }

    // CommunityDataSourceImpl.kt 의 toggleLike 함수 수정
    suspend fun toggleLike(postId: String): AuthResult<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val postRef = firestore.collection("Posts").document(postId)
            val likeRef = postRef.collection("Likes").document(uid)

            val isAlreadyLiked = likeRef.get().await().exists()

            if (isAlreadyLiked) {
                // 이미 눌렀다면: 좋아요 취소
                likeRef.delete().await()
                postRef.update("likes", FieldValue.increment(-1)).await()
            } else {
                // 안 눌렀다면: 좋아요 추가
                likeRef.set(mapOf("timestamp" to System.currentTimeMillis())).await()
                postRef.update("likes", FieldValue.increment(1)).await()
            }

            AuthResult.Success(!isAlreadyLiked) // true면 좋아요됨, false면 취소됨
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "좋아요 처리 실패")
        }
    }

    // 4. 댓글 추가 로직
    suspend fun addComment(postId: String, content: String): AuthResult<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
            val userDoc = firestore.collection("UserData").document(uid).get().await()
            val userName = userDoc.getString("userName") ?: "익명"

            val commentMap = mapOf(
                "authorName" to userName,
                "content" to content,
                "timestamp" to System.currentTimeMillis()
            )

            val postRef = firestore.collection("Posts").document(postId)
            postRef.collection("Comments").add(commentMap).await()
            postRef.update("commentCount", FieldValue.increment(1)).await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "댓글 작성 실패")
        }
    }

    // 5. 실시간 댓글 리스너 (이 부분이 'getCommentsFlow' 입니다!)
    fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("Posts").document(postId)
            .collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    Comment(
                        commentId = doc.id,
                        authorName = doc.getString("authorName") ?: "익명",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { subscription.remove() }
    }

    // 6. 게시글 업로드 (이미지 압축 포함)
    suspend fun uploadPost(content: String, imageUris: List<Uri>): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = auth.currentUser?.uid ?: return@coroutineScope AuthResult.Fail("로그인 필요")
            val userDoc = firestore.collection("UserData").document(uid).get().await()
            val realUserName = userDoc.getString("userName") ?: "Runner"

            val postRef = firestore.collection("Posts").document()
            val postId = postRef.id

            val uploadTasks = imageUris.mapIndexed { index, uri ->
                async {
                    val fileName = "post_${System.currentTimeMillis()}_$index.jpg"
                    val storageRef = storage.reference.child("posts/$uid/$fileName")
                    val compressedData = compressImageWithRotation(uri)
                    if (compressedData != null) {
                        storageRef.putBytes(compressedData).await()
                        storageRef.downloadUrl.await().toString()
                    } else null
                }
            }
            val imageUrls = uploadTasks.awaitAll().filterNotNull()

            val postMap = mapOf(
                "postId" to postId,
                "authorId" to uid,
                "authorName" to realUserName,
                "content" to content,
                "images" to imageUrls,
                "timestamp" to System.currentTimeMillis(),
                "likes" to 0,
                "commentCount" to 0
            )

            postRef.set(postMap).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "업로드 실패")
        }
    }

    private fun compressImageWithRotation(uri: Uri): ByteArray? {
        return try {
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap == null) return null
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                originalBitmap.recycle()
                rotated
            } else originalBitmap
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val result = outputStream.toByteArray()
            finalBitmap.recycle()
            result
        } catch (e: Exception) { null }
    }
}