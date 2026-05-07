package com.runit.runup.data.source.remote.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.runit.runup.domain.model.AddressModel
import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.Comment
import com.runit.runup.domain.model.FilterState
import com.runit.runup.domain.model.FilterType
import com.runit.runup.domain.model.Post
import com.runit.runup.domain.model.PostImage
import com.runit.runup.domain.model.RunRecord
import com.runit.runup.domain.model.ViewScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CommunityDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    suspend fun getPosts(
        lastVisibleSnapshot: DocumentSnapshot? = null,
        limit: Long = 3,
        filter: FilterState,
        friendIds: List<String> = emptyList() // 🔹 친구 ID 리스트를 인자로 받음
    ): AuthResult<Pair<List<Post>, DocumentSnapshot?>> {
        return try {
            var query: Query = firestore.collection("Posts")
            val uid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")

            // 🔹 [1순위 필터] 보기 범위 설정 (친구/전체)
            when (filter.scope) {
                ViewScope.FRIENDS -> query = query.whereIn("authorId", friendIds)
                ViewScope.MINE -> query = query.whereEqualTo("authorId", uid)
                ViewScope.ALL -> { /* 필터 없음 */ }
            }

            // 🔹 [2순위 필터] 지역 필터링 (기존 로직 유지)
            if (filter.type != FilterType.ALL) {
                if (filter.city.isNotEmpty()) query = query.whereEqualTo("city", filter.city)
                if (filter.district.isNotEmpty()) query = query.whereEqualTo("district", filter.district)
                if (filter.dong.isNotEmpty()) query = query.whereEqualTo("dong", filter.dong)
            }

            // 🔹 [3순위] 정렬 및 페이징
            query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)

            if (lastVisibleSnapshot != null) {
                query = query.startAfter(lastVisibleSnapshot)
            }

            val snapshot = query.get().await()
            val lastSnapshot = snapshot.documents.lastOrNull()

            // (이하 데이터 매핑 로직은 기존과 동일하므로 생략)
            val postList = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                Log.d("filtercheck", "${post?.city}")
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
                    isLiked = post.likedBy.contains(uid),
                    commentCount = (doc.get("commentCount") as? Number)?.toInt() ?: 0,
                    runRecord = doc.get("runRecord", RunRecord::class.java),
                    // 🔹 주소 정보 추가
                    city = doc.getString("city") ?: "",
                    district = doc.getString("district") ?: "",
                    dong = doc.getString("dong") ?: "",
                )

            }

            AuthResult.Success(Pair(postList, lastSnapshot))

        } catch (e: Exception) {
            Log.e("CommunityDataSource", "Combined Query Error: ${e.message}")
            AuthResult.Fail(e.localizedMessage ?: "로드 실패")
        }
    }

    suspend fun getTargetUserPosts(
        targetUid: String,
        tabType: String, // "POSTS", "HEARTS", "COMMENTS", "FOLLOWS"
        filter: FilterState,
        lastVisibleSnapshot: DocumentSnapshot? = null,
        limit: Long = 4L
    ): AuthResult<Triple<List<Post>, DocumentSnapshot?, Boolean>> {
        return try {
            var query: Query = firestore.collection("Posts")
            val myUid = auth.currentUser?.uid ?: ""

            // ── [1단계] 탭 타입에 따른 대상 필터링 ──
            query = when (tabType) {
                "POSTS" -> query.whereEqualTo("authorId", targetUid)
                "HEARTS" -> query.whereArrayContains("likedBy", targetUid)
                "COMMENTS" -> query.whereArrayContains("commentedBy", targetUid)
                "FOLLOWS" -> query.whereArrayContains("followedBy", targetUid)
                else -> query.whereEqualTo("authorId", targetUid)
            }

            // ── [2단계] 지역 필터링 (기존 로직 유지) ──
            if (filter.type != FilterType.ALL) {
                if (filter.city.isNotEmpty()) query = query.whereEqualTo("city", filter.city)
                if (filter.district.isNotEmpty()) query = query.whereEqualTo("district", filter.district)
                if (filter.dong.isNotEmpty()) query = query.whereEqualTo("dong", filter.dong)
            }

            // ── [3단계] 정렬 및 페이징 ──
            query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit + 1)

            if (lastVisibleSnapshot != null) {
                query = query.startAfter(lastVisibleSnapshot)
            }

            val snapshot = query.get().await()
            val documents = snapshot.documents

            // ── 🔹 [핵심] 5개가 왔다면 다음 페이지가 있는 것! 📍 ──
            val hasMore = documents.size > limit
            val displayDocs = if (hasMore) documents.take(limit.toInt()) else documents
            val lastSnapshot = displayDocs.lastOrNull()

            // 🔹 데이터 매핑 (기존과 동일)
            val postList = displayDocs.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                val locationImages = doc.get("locationImages") as? List<Map<String, Any>> ?: emptyList()
                val commonImages = doc.get("commonImages") as? List<Map<String, Any>> ?: emptyList()

                post?.copy(
                    postId = doc.id,
                    authorId = doc.getString("authorId") ?: "",
                    authorName = doc.getString("authorName") ?: "익명",
                    authorProfileUrl = doc.getString("userProfileUrlMini") ?: "",
                    content = doc.getString("content") ?: "",
                    locationImages = locationImages.map { map ->
                        PostImage(
                            url = map["url"] as? String ?: "",
                            thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                            location = map["location"] as? GeoPoint
                        )
                    },
                    commonImages = commonImages.map { map ->
                        PostImage(url = map["url"] as? String ?: "")
                    },
                    likes = (doc.get("likes") as? Number)?.toInt() ?: 0,
                    isLiked = (doc.get("likedBy") as? List<String>)?.contains(myUid) ?: false,
                    commentCount = (doc.get("commentCount") as? Number)?.toInt() ?: 0,
                    // ── 🔹 추가된 팔로우 데이터 매핑 ── 📍
                    followCount = (doc.get("followCount") as? Number)?.toInt() ?: 0,
                    followedBy = doc.get("followedBy") as? List<String> ?: emptyList(),
                    city = doc.getString("city") ?: "",
                    district = doc.getString("district") ?: "",
                    dong = doc.getString("dong") ?: "",
                    isOfficialCourse = doc.getBoolean("isOfficialCourse") ?: false,
                )
            }

            AuthResult.Success(Triple(postList, lastSnapshot, hasMore))
        } catch (e: Exception) {
            Log.e("DataSource", "TargetUser Query Error: ${e.message}")
            AuthResult.Fail(e.localizedMessage ?: "데이터 로드 실패")
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

    // CommunityDataSourceImpl.kt

    suspend fun toggleLike(postId: String): AuthResult<Boolean> {
        val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
        val postRef = firestore.collection("Posts").document(postId)

        return try {
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val likedBy = postSnapshot.get("likedBy") as? List<String> ?: emptyList()
                val isAlreadyLiked = likedBy.contains(myUid)

                // 🔹 [Posts] 업데이트: 좋아요 수 & ID 리스트 (필수 유지)
                if (isAlreadyLiked) {
                    transaction.update(postRef, "likes", FieldValue.increment(-1))
                    transaction.update(postRef, "likedBy", FieldValue.arrayRemove(myUid))
                } else {
                    transaction.update(postRef, "likes", FieldValue.increment(1))
                    transaction.update(postRef, "likedBy", FieldValue.arrayUnion(myUid))
                }

                // 🔹 [UserData] -> 내 PostStats/info 에 게시물 ID 기록만 남김
                val myStatsRef = firestore.collection("UserData").document(myUid)
                    .collection("PostStats").document("info")

                val updateAction = if (isAlreadyLiked) FieldValue.arrayRemove(postId) else FieldValue.arrayUnion(postId)
                transaction.set(myStatsRef, mapOf("likePostIds" to updateAction), SetOptions.merge())

                !isAlreadyLiked
            }.await().let { AuthResult.Success(it) }
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "좋아요 실패")
        }
    }

    suspend fun addComment(postId: String, content: String): AuthResult<Boolean> {
        val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
        val postRef = firestore.collection("Posts").document(postId)

        return try {
            val userDoc = firestore.collection("UserData").document(myUid).get().await()
            val userName = userDoc.getString("userName") ?: "익명"
            val userCommentProfileUrl = userDoc.getString("userProfileUrlMini") ?: ""

            firestore.runTransaction { transaction ->
                // 🔹 [Posts] 하위 Comments 추가 및 정보 업데이트
                val newCommentRef = postRef.collection("Comments").document()
                transaction.set(newCommentRef, mapOf(
                    "authorId" to myUid,
                    "authorName" to userName,
                    "authorProfileUrlMini" to userCommentProfileUrl,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis()
                ))

                // 🔹 [Posts] 댓글 수 증가 & 댓글 작성자 리스트 업데이트 (삭제 시 필요)
                transaction.update(postRef, "commentCount", FieldValue.increment(1))
                transaction.update(postRef, "commentedBy", FieldValue.arrayUnion(myUid))

                // 🔹 [UserData] -> 내 PostStats/info 에 기록
                val myStatsRef = firestore.collection("UserData").document(myUid)
                    .collection("PostStats").document("info")
                transaction.set(myStatsRef, mapOf("commentPostIds" to FieldValue.arrayUnion(postId)), SetOptions.merge())

                true
            }.await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "댓글 실패")
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): AuthResult<Boolean> {
        val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
        val postRef = firestore.collection("Posts").document(postId)
        val commentRef = postRef.collection("Comments").document(commentId)

        return try {
            firestore.runTransaction { transaction ->
                // 댓글 데이터 확인
                val commentSnapshot = transaction.get(commentRef)
                if (!commentSnapshot.exists()) throw Exception("존재하지 않는 댓글입니다.")
                if (commentSnapshot.getString("authorId") != myUid) throw Exception("삭제 권한이 없습니다.")

                // 댓글 삭제 및 카운트 감소
                transaction.delete(commentRef)
                transaction.update(postRef, "commentCount", FieldValue.increment(-1))

                null // 트랜잭션 내에서 비동기 쿼리를 직접 수행할 수 없으므로 일단 닫음
            }.await()

            // 3. [동기화 핵심] 이 포스트에 내가 쓴 다른 댓글이 있는지 확인
            val remainingComments = postRef.collection("Comments")
                .whereEqualTo("authorId", myUid)
                .limit(1)
                .get()
                .await()

            // 더 이상 내 댓글이 없다면 흔적 지우기 🔹
            if (remainingComments.isEmpty) {
                firestore.runBatch { batch ->
                    // 포스트의 '댓글 단 사람' 리스트에서 제거
                    batch.update(postRef, "commentedBy", FieldValue.arrayRemove(myUid))

                    // 내 통계 리스트에서 해당 포스트 ID 제거
                    val myStatsRef = firestore.collection("UserData").document(myUid)
                        .collection("PostStats").document("info")
                    batch.set(myStatsRef, mapOf("commentPostIds" to FieldValue.arrayRemove(postId)), SetOptions.merge())
                }.await()

                // 💡 ViewModel에게 "통계에서 이 포스트를 지워야 함"을 알리기 위해 true 반환
                AuthResult.Success(true)
            } else {
                // 아직 내 댓글이 다른 게 남아있음
                AuthResult.Success(false)
            }
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "삭제 실패")
        }
    }

    // 5. 실시간 댓글 리스너 (이 부분이 'getCommentsFlow' 입니다!)
    fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("Posts").document(postId)
            .collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // ── 🔹 [수정] 로그아웃 시 발생하는 권한 에러면 조용히 종료 📍 ──
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("Firestore", "권한 상실(로그아웃 등)로 인해 리스너를 닫습니다.")
                        close() // Flow를 닫아버림
                    } else {
                        close(error) // 그 외 진짜 에러는 던짐
                    }
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    Comment(
                        commentId = doc.id,
                        authorId = doc.getString("authorId") ?: "",
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

    suspend fun uploadPost(
        content: String,
        LocaitonimageUris: List<Uri>,
        CommonimageUris: List<Uri>,
        runRecord: RunRecord?,
        address: AddressModel?
    ): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = auth.currentUser?.uid ?: return@coroutineScope AuthResult.Fail("로그인 필요")
            val userDoc = firestore.collection("UserData").document(uid).get().await()
            val realUserName = userDoc.getString("userName") ?: "Runner"
            val profileMiniUrl = userDoc.getString("userProfileUrlMini") ?: ""

            val myStatsRef = firestore.collection("UserData").document(uid)
                .collection("PostStats").document("info")

            val postRef = firestore.collection("Posts").document()
            val customPostId = postRef.id

            myStatsRef.set( // 내 활동 통계(내가 쓴 글 목록)에 방금 만든 ID 추가
                mapOf("uploadPostIds" to FieldValue.arrayUnion(customPostId)),
                SetOptions.merge()
            ).await()

            // 위치 기반 이미지 병렬 처리
            val locaitonUploadTasks = LocaitonimageUris.mapIndexed { index, uri ->
                // Dispatchers.IO 명시 (멀티코어를 적극적으로 활용해 동시에 압축/업로드)
                async(Dispatchers.IO) {
                    try {
                        var geoPoint: GeoPoint? = null
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val exif = ExifInterface(inputStream)
                                val latLong = FloatArray(2)

                                if (exif.getLatLong(latLong)) {
                                    if (latLong[0] != 0f || latLong[1] != 0f) {
                                        geoPoint = GeoPoint(latLong[0].toDouble(), latLong[1].toDouble())
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Exif", "스트림 읽기 실패: ${e.localizedMessage}")
                        }

                        val originalData = compressImage(uri, quality = 80)
                            ?: return@async null // 압축 실패 시 이 사진만 건너뜀

                        val originalFileName = "${customPostId}_L_${index}_orig.jpg"
                        val originalRef = storage.reference.child("posts/$uid/$originalFileName")
                        originalRef.putBytes(originalData).await()
                        val originalUrl = originalRef.downloadUrl.await().toString()

                        // 썸네일 압축 및 업로드
                        val thumbData = resizeAndCompressImage(uri, width = 150, height = 150)
                            ?: return@async null

                        val thumbFileName = "${customPostId}_L_${index}_thumb.jpg"
                        val thumbRef = storage.reference.child("posts/$uid/$thumbFileName")
                        thumbRef.putBytes(thumbData).await()
                        val thumbUrl = thumbRef.downloadUrl.await().toString()

                        mapOf(
                            "url" to originalUrl,
                            "thumbnailUrl" to thumbUrl,
                            "location" to geoPoint
                        )
                    } catch (e: Exception) {
                        // 👈 핵심 3: 사진 하나 올리다 실패해도 전체 코루틴이 터지지 않게 방어
                        Log.e("Upload", "위치 사진 업로드 실패", e)
                        null
                    }
                }
            }
            val locationImageUrls = locaitonUploadTasks.awaitAll().filterNotNull()

            // ── 🌟 [2] 일반 이미지 병렬 처리 ──
            val commonUploadTasks = CommonimageUris.mapIndexed { index, uri ->
                // 👈 핵심 1: 여기도 Dispatchers.IO 명시
                async(Dispatchers.IO) {
                    try {
                        val originalFileName = "${customPostId}_C_${index}_orig.jpg"
                        val originalRef = storage.reference.child("posts/$uid/$originalFileName")

                        val originalData = compressImage(uri, quality = 80) // 👈 퀄리티 70
                            ?: return@async null

                        originalRef.putBytes(originalData).await()
                        val originalUrl = originalRef.downloadUrl.await().toString()

                        mapOf("url" to originalUrl)
                    } catch (e: Exception) {
                        Log.e("Upload", "일반 사진 업로드 실패", e)
                        null
                    }
                }
            }
            val commonImageUrls = commonUploadTasks.awaitAll().filterNotNull()

            // ── [3] Firestore 최종 저장 ──
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
                "runRecord" to runRecord,
                "city" to (address?.city ?: ""),
                "district" to (address?.district ?: ""),
                "dong" to (address?.dong ?: ""),
                "isOfficialCourse" to false,
            )

            postRef.set(postMap).await()
            AuthResult.Success(true)

        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "업로드 실패")
        }
    }

    suspend fun deletePost(post: Post): AuthResult<Boolean> = coroutineScope {
        try {
            val uid = auth.currentUser?.uid ?: return@coroutineScope AuthResult.Fail("로그인 필요")
            if (post.authorId != uid) return@coroutineScope AuthResult.Fail("삭제 권한이 없습니다.")

            val postRef = firestore.collection("Posts").document(post.postId)
            val commentsRef = postRef.collection("Comments")

            // 댓글 데이터 미리 가져오기
            val commentsSnapshot = commentsRef.get().await()

            // ── 🔹 [2] Firestore 트랜잭션 청소 ──
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)

                val actualLikedBy = postSnapshot.get("likedBy") as? List<String> ?: emptyList()
                val actualCommentedBy = postSnapshot.get("commentedBy") as? List<String> ?: emptyList()
                val actualFollowedBy = postSnapshot.get("followedBy") as? List<String> ?: emptyList() // 👈 추가📍

                // (1) 서브컬렉션(댓글) 삭제
                commentsSnapshot.forEach { commentDoc ->
                    transaction.delete(commentDoc.reference)
                }

                // (2) 포스트 본체 삭제
                transaction.delete(postRef)

                // (3) 작성자의 업로드 리스트에서 제거
                val myStatsRef = firestore.collection("UserData").document(uid).collection("PostStats").document("info")
                transaction.update(myStatsRef, "uploadPostIds", FieldValue.arrayRemove(post.postId))

                // (4) 좋아요 기록 전역 삭제
                actualLikedBy.forEach { userId ->
                    val ref = firestore.collection("UserData").document(userId).collection("PostStats").document("info")
                    transaction.update(ref, "likePostIds", FieldValue.arrayRemove(post.postId))
                }

                // (5) 댓글 기록 전역 삭제
                actualCommentedBy.forEach { userId ->
                    val ref = firestore.collection("UserData").document(userId).collection("PostStats").document("info")
                    transaction.update(ref, "commentPostIds", FieldValue.arrayRemove(post.postId))
                }

                // (6) [신규] 따라뛰기(Follow) 기록 전역 삭제 ── 👟📍
                actualFollowedBy.forEach { userId ->
                    val ref = firestore.collection("UserData").document(userId).collection("PostStats").document("info")
                    transaction.update(ref, "followPostIds", FieldValue.arrayRemove(post.postId))
                }
            }.await()


            //  Storage는 별도로 fire-and-forget (실패해도 흐름 안 막음)
            val allImageUrls = mutableListOf<String>()
            post.locationImages.forEach {
                if (it.url.isNotEmpty()) allImageUrls.add(it.url)
                if (it.thumbnailUrl.isNotEmpty()) allImageUrls.add(it.thumbnailUrl)
            }
            post.commonImages.forEach {
                if (it.url.isNotEmpty()) allImageUrls.add(it.url)
            }
            allImageUrls.distinct().forEach { url ->
                launch(Dispatchers.IO) { // await() 없이 그냥 실행
                    try { storage.getReferenceFromUrl(url).delete().await() }
                    catch (e: Exception) { Log.e("DeleteError", "이미지 삭제 실패: ${e.message}") }
                }
            }

            AuthResult.Success(true)
        } catch (e: Exception) {
            Log.e("DeleteError", "삭제 전체 공정 실패: ${e.localizedMessage}")
            AuthResult.Fail(e.localizedMessage ?: "삭제 실패")
        }
    }

    private fun compressImage(uri: Uri, quality: Int, maxWidth: Int = 1080): ByteArray? {
        val resolver = context.contentResolver

        // 1. 🌟 원본 이미지의 크기만 먼저 확인 (메모리에 안 올림, 0.01초 컷!)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        resolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        // 2. 얼마나 줄일지 비율(inSampleSize) 계산
        options.inSampleSize = calculateInSampleSize(options, maxWidth)

        // 3. 🌟 계산된 비율만큼 이미 작게 축소된 상태로 메모리에 로드 (여기서 속도 10배 차이 발생!)
        options.inJustDecodeBounds = false
        val sampledBitmap = resolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        } ?: return null

        // 4. 사진 회전 각도 보정
        val rotatedBitmap = rotateImageIfRequired(sampledBitmap, uri)

        // 5. 정확한 maxWidth로 미세 조정 (inSampleSize는 2의 배수로만 뭉텅이로 줄이기 때문)
        val finalBitmap = if (rotatedBitmap.width > maxWidth) {
            val scale = maxWidth.toFloat() / rotatedBitmap.width
            val newHeight = (rotatedBitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(rotatedBitmap, maxWidth, newHeight, true)
        } else {
            rotatedBitmap
        }

        // 6. JPEG 압축 진행
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // 7. 메모리 누수 방지
        if (sampledBitmap != rotatedBitmap) sampledBitmap.recycle()
        if (rotatedBitmap != finalBitmap) rotatedBitmap.recycle()
        finalBitmap.recycle()

        return outputStream.toByteArray()
    }

    // ── 🔹 메모리 낭비를 막아주는 마법의 계산 함수 📍 ──
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int {
        val width = options.outWidth
        var inSampleSize = 1

        if (width > reqWidth) {
            val halfWidth: Int = width / 2
            // 원본을 1/2, 1/4, 1/8... 로 계속 나누면서 요구 사이즈에 맞춤
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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

    suspend fun followRunning(postId: String): AuthResult<Boolean> {
        val myUid = auth.currentUser?.uid ?: return AuthResult.Fail("로그인 필요")
        val postRef = firestore.collection("Posts").document(postId)
        val myStatsRef = firestore.collection("UserData").document(myUid)
            .collection("PostStats").document("info")

        return try {
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                if (!postSnapshot.exists()) throw Exception("포스트가 존재하지 않습니다.")

                val followedBy = postSnapshot.get("followedBy") as? List<String> ?: emptyList()

                if (!followedBy.contains(myUid)) { // 이미 팔로우했는지 확인
                    // 처음 클릭한 경우에만 숫자 증가 및 ID 추가
                    transaction.update(postRef, "followCount", FieldValue.increment(1))
                    transaction.update(postRef, "followedBy", FieldValue.arrayUnion(myUid))
                }

                // ── [내 통계 업데이트] 중복되어도 arrayUnion이 알아서 처리 ──
                transaction.set(myStatsRef, mapOf(
                    "followPostIds" to FieldValue.arrayUnion(postId)
                ), SetOptions.merge())

                true
            }.await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.localizedMessage ?: "팔로우 실패")
        }
    }

    suspend fun promoteToOfficialCourse(postId: String): AuthResult<Boolean> {
        return try {
            firestore.collection("Posts").document(postId)
                .update("isOfficialCourse", true)
                .await()
            AuthResult.Success(true)
        } catch (e: Exception) {
            Log.e("CommunityDataSource", "공식 코스 등록 실패: ${e.message}")
            AuthResult.Fail(e.localizedMessage ?: "업데이트 실패")
        }
    }

    /**
     * [수정] 탈퇴 시 커뮤니티의 모든 데이터(내가 쓴 글 + 남의 글에 남긴 흔적)를 삭제
     */
    suspend fun deleteAllMyCommunityData(
        uid: String,
        uploadPostIds: List<String>,
        likePostIds: List<String>,
        commentPostIds: List<String>,
        followPostIds: List<String>
    ) = coroutineScope {

        cleanupUserInteractions(uid, likePostIds, commentPostIds, followPostIds)

        uploadPostIds.forEach { postId ->
            try {
                val postDoc = firestore.collection("Posts").document(postId).get().await()
                val post = postDoc.toObject(Post::class.java)?.copy(postId = postId)
                if (post != null) {
                    deletePost(post) // 👈 이미 잘 만들어두신 이 함수를 그대로 사용!
                }
            } catch (e: Exception) {
                Log.e("CommunityDelete", "게시글($postId) 삭제 실패: ${e.message}")
            }
        }
    }

    /**
     * [보조] 타인의 게시글에 남긴 흔적 삭제 (Batch 활용)
     */
    private suspend fun cleanupUserInteractions(
        uid: String,
        likePostIds: List<String>,
        commentPostIds: List<String>,
        followPostIds: List<String>
    ) {
        // 좋아요 & 팔로우 차감 (Batch)
        if (likePostIds.isNotEmpty() || followPostIds.isNotEmpty()) {
            firestore.runBatch { batch ->
                likePostIds.forEach { postId ->
                    val ref = firestore.collection("Posts").document(postId)
                    batch.update(ref, "likes", FieldValue.increment(-1), "likedBy", FieldValue.arrayRemove(uid))
                }
                followPostIds.forEach { postId ->
                    val ref = firestore.collection("Posts").document(postId)
                    batch.update(ref, "followCount", FieldValue.increment(-1), "followedBy", FieldValue.arrayRemove(uid))
                }
            }.await()
        }

        // 댓글 삭제 (서브컬렉션이므로 개별 처리)
        commentPostIds.forEach { postId ->
            val postRef = firestore.collection("Posts").document(postId)
            val myComments = postRef.collection("Comments").whereEqualTo("authorId", uid).get().await()
            if (!myComments.isEmpty) {
                firestore.runBatch { batch ->
                    myComments.forEach { batch.delete(it.reference) }
                    batch.update(postRef, "commentCount", FieldValue.increment(-myComments.size().toLong()))
                    batch.update(postRef, "commentedBy", FieldValue.arrayRemove(uid))
                }.await()
            }
        }
    }
}