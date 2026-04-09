package com.example.runup.data.source.remote.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Comment
import com.example.runup.domain.model.Post
import com.example.runup.domain.model.PostImage
import com.example.runup.domain.model.RunRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
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
    suspend fun getPosts(
        lastVisibleSnapshot: DocumentSnapshot? = null,
        limit: Long = 3
    ): AuthResult<Pair<List<Post>, DocumentSnapshot?>> {
        return try {
            var query = firestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            // 이전에 읽은 마지막 문서가 있다면 그 다음부터 가져옴
            if (lastVisibleSnapshot != null) {
                query = query.startAfter(lastVisibleSnapshot)
            }

            val snapshot = query.get().await()
            val lastSnapshot = snapshot.documents.lastOrNull() // 이번에 읽은 마지막 문서 저장

            val postList = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                val locationImages = doc.get("locationImages") as? List<Map<String, Any>> ?: emptyList()
                val locationImagesList = locationImages.map { map ->
                    PostImage(
                        url = map["url"] as? String ?: "",
                        thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                        location = map["location"] as? GeoPoint
                    )
                }
                val commonImages = doc.get("commonImages") as? List<Map<String, Any>> ?: emptyList()
                val commonImagesList = commonImages.map { map ->
                    PostImage(
                        url = map["url"] as? String ?: "",
                    )
                }


                post?.copy(
                    postId = doc.id,
                    authorId = doc.getString("authorId") ?: "id",
                    authorName = doc.getString("authorName") ?: "익명",
                    authorProfileUrl = doc.getString("userProfileUrlMini") ?: "",
                    content = doc.getString("content") ?: "",
                    locationImages = locationImagesList,
                    commonImages = commonImagesList,
                    likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                    commentCount = (doc.get("commentCount") as? Number)?.toInt() ?: 0,
                    runRecord = doc.get("runRecord", RunRecord::class.java)
                )

            }
            // 데이터와 커서를 함께 반환
            AuthResult.Success(Pair(postList, lastSnapshot))

        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "로드 실패")
        }
    }

    // 2. 게시글 상세 정보 가져오기
    suspend fun getPostById(postId: String): AuthResult<Post> {
        return try {
            val doc = firestore.collection("Posts").document(postId).get().await()

            val post = doc.toObject(Post::class.java)
            val locationImages = doc.get("locationImages") as? List<Map<String, Any>> ?: emptyList()
            val LocationImagesList = locationImages.map { map ->
                PostImage(
                    url = map["url"] as? String ?: "",
                    location = map["location"] as? GeoPoint
                )
            }
            val commonImages = doc.get("commonImages") as? List<Map<String, Any>> ?: emptyList()
            val commonImagesList = commonImages.map { map ->
                PostImage(
                    url = map["url"] as? String ?: "",
                )
            }
            if (doc.exists()) {
                val post = Post(
                    postId = doc.id,
                    authorId = doc.getString("authorId") ?: "id",
                    authorName = doc.getString("authorName") ?: "익명",
                    authorProfileUrl = doc.getString("userProfileUrlMini") ?: "",
                    content = doc.getString("content") ?: "",
                    locationImages = LocationImagesList,
                    commonImages = commonImagesList,
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
            val userProfileUrlMini = userDoc.getString("userProfileUrlMini") ?: "userProfileUrl"

            val commentMap = mapOf(
                "authorName" to userName,
                "authorProfileUrlMini" to userProfileUrlMini,
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
                        authorProfileUrlMini = doc.getString("authorProfileUrlMini") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { subscription.remove() }
    }

    // 6. 게시글 업로드 (이미지 압축 포함)
    suspend fun uploadPost(
        content: String,
        LocaitonimageUris: List<Uri>,
        CommonimageUris: List<Uri>,
        runRecord: RunRecord?
    ): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = auth.currentUser?.uid ?: return@coroutineScope AuthResult.Fail("로그인 필요")
            val userDoc = firestore.collection("UserData").document(uid).get().await()
            val realUserName = userDoc.getString("userName") ?: "Runner"
            val profileMiniUrl = userDoc.getString("userProfileUrlMini") ?: ""

            // --- [수정 시작] 트랜잭션을 통한 Post ID 생성 로직 ---
            val metadataRef = firestore.collection("Metadata").document("postInfo")

            val customPostId = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(metadataRef)

                // lastPostNumber 필드에서 현재 번호를 가져옴 (없으면 0)
                val currentNumber = snapshot.getLong("lastPostNumber")?.toInt() ?: 0
                val nextNumber = currentNumber + 1

                // 번호 업데이트 (Int 형태로 다시 저장)
                transaction.update(metadataRef, "lastPostNumber", nextNumber)

                // "post1", "post2" 형태의 문자열 생성
                "post$nextNumber"
            }.await()

            val postRef = firestore.collection("Posts").document(customPostId)

            val locaitonUploadTasks = LocaitonimageUris.mapIndexed { index, uri ->
                async {
                    var geoPoint: GeoPoint? = null
                    try {
                        // Photo Picker URI는 setRequireOriginal을 지원하지 않으므로
                        // 권한이 있는 상태에서 원본 uri를 그대로 사용합니다.
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val exif = ExifInterface(inputStream)
                            val latLong = FloatArray(2)

                            if (exif.getLatLong(latLong)) {
                                if (latLong[0] != 0f || latLong[1] != 0f) {
                                    geoPoint = GeoPoint(latLong[0].toDouble(), latLong[1].toDouble())
                                    Log.d("ExifCheck", "좌표 추출 성공: ${geoPoint.latitude}, ${geoPoint.longitude}")
                                }
                            } else {
                                Log.d("ExifCheck", "Exif 데이터가 없거나 접근이 제한됨")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Exif", "스트림 읽기 실패: ${e.localizedMessage}")
                    }

                    // 원본 이미지 업로드
                    val originalFileName = "${customPostId}_L_${index}_orig.jpg"
                    val originalRef = storage.reference.child("posts/$uid/$originalFileName")
                    val originalData = compressImage(uri, quality = 80) // 일반 압축
                    originalRef.putBytes(originalData!!).await()
                    val originalUrl = originalRef.downloadUrl.await().toString()


                    // 마커용 초소형 썸네일 생성 및 업로드
                    val thumbFileName = "${customPostId}_L_${index}_thumb.jpg"
                    val thumbRef = storage.reference.child("posts/$uid/$thumbFileName")
                    // 150px 사이즈로 아주 작게 리사이징 (이게 로딩 속도를 결정함)
                    val thumbData = resizeAndCompressImage(uri, width = 150, height = 150)
                    thumbRef.putBytes(thumbData!!).await()
                    val thumbUrl = thumbRef.downloadUrl.await().toString()

                    mapOf(
                        "url" to originalUrl,
                        "thumbnailUrl" to thumbUrl, // 썸네일 주소 추가
                        "location" to geoPoint
                    )
                }
            }
            val locationImageUrls = locaitonUploadTasks.awaitAll().filterNotNull()

            val commonUploadTasks = CommonimageUris.mapIndexed { index, uri ->
                async {
                    val originalFileName = "${customPostId}_C_${index}_orig.jpg"
                    val originalRef = storage.reference.child("posts/$uid/$originalFileName")
                    val originalData = compressImage(uri, quality = 80)
                    originalRef.putBytes(originalData!!).await()
                    val originalUrl = originalRef.downloadUrl.await().toString()

                    mapOf(
                        "url" to originalUrl,
                    )
                }
            }
            val commonImageUrls = commonUploadTasks.awaitAll().filterNotNull()

            val postMap = mapOf(
                "postId" to customPostId,
                "authorId" to uid,
                "authorName" to realUserName,
                "userProfileUrlMini" to profileMiniUrl,
                "content" to content,
                "locationImages" to locationImageUrls,
                "commonImages" to commonImageUrls,
                "timestamp" to System.currentTimeMillis(),
                "likes" to 0,
                "commentCount" to 0,
                "runRecord" to runRecord
            )

            postRef.set(postMap).await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "업로드 실패")
        }
    }

    // 1. 원본 압축 함수 (기존 quality 80 용)
    private fun compressImage(uri: Uri, quality: Int): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

        // 사진 회전 각도 보정 (카메라로 찍은 사진이 누워있을 수 있음)
        val rotatedBitmap = rotateImageIfRequired(bitmap, uri)

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun resizeAndCompressImage(uri: Uri, width: Int, height: Int): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // [추가] 썸네일 생성 전에도 회전 각도 보정 실행
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, uri)

        // 지정된 크기로 리사이징
        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)

        val outputStream = ByteArrayOutputStream()
        // 퀄리티를 50~60 정도로 낮춰도 150px에서는 충분히 깨끗합니다.
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

        // 사용한 비트맵들 메모리 해제 (선택 사항이지만 권장)
        if (rotatedBitmap != originalBitmap) {
            rotatedBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    // [필수 보조] 사진 회전 방지 로직
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(selectedImage) ?: return img
        val ei = ExifInterface(input)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle() // 메모리 해제
        return rotatedImg
    }

    // 게시글 삭제 (Firestore 문서 + Storage 이미지 전체)
    suspend fun deletePost(post: Post): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = auth.currentUser?.uid ?: return@coroutineScope AuthResult.Fail("로그인 필요")

            // 1. 권한 확인 (본인 글인지 다시 한번 검증)
            if (post.authorId != uid) return@coroutineScope AuthResult.Fail("삭제 권한이 없습니다.")

            val deleteTasks = mutableListOf<Deferred<Unit>>()

            post.locationImages.forEach { image ->
                deleteTasks.add(async {
                    storage.getReferenceFromUrl(image.url).delete().await()
                    Unit // 명시적으로 Unit 반환
                })
                image.thumbnailUrl?.let { thumbUrl ->
                    deleteTasks.add(async {
                        storage.getReferenceFromUrl(thumbUrl).delete().await()
                        Unit // 명시적으로 Unit 반환
                    })
                }
            }

            post.commonImages.forEach { image ->
                deleteTasks.add(async {
                    storage.getReferenceFromUrl(image.url).delete().await()
                    Unit // 명시적으로 Unit 반환
                })
            }

            // 3. 모든 이미지 삭제 병렬 실행
            deleteTasks.awaitAll()

            // 4. Firestore 게시글 문서 삭제
            firestore.collection("Posts").document(post.postId).delete().await()

            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "삭제 실패")
        }
    }
}