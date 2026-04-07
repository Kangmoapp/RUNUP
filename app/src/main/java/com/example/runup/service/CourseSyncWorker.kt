package com.example.runup.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.domain.model.Course
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.objectbox.Box
import kotlinx.coroutines.tasks.await


@HiltWorker
class CourseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val courseBox: Box<CourseEntity>,
    private val gson: Gson
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RUNUP_SYNC", "🚀 백그라운드 동기화 작업 시작...")

        return try {
            // 1. 서버(Firestore)에서 최신 데이터 가져오기
            val snapshot = firestore.collection("Course").get().await()
            val remoteIds = snapshot.documents.map { it.id }.toSet()

            // 2. 로컬에만 있는 '유령 데이터' 삭제
            val localEntities = courseBox.all
            val toDelete = localEntities.filter { it.firebaseId !in remoteIds }
            if (toDelete.isNotEmpty()) {
                courseBox.remove(toDelete)
                Log.d("RUNUP_SYNC", "🗑️ 유령 데이터 ${toDelete.size}건 삭제 완료")
            }

            // 3. 최신 데이터 갱신 및 추가
            snapshot.documents.forEach { doc ->
                val courseModel = doc.toObject(Course::class.java) ?: return@forEach

                // 벡터 데이터 변환 (Double List -> FloatArray)
                val vectorList = doc.get("vector") as? List<Double>
                val floatVector = vectorList?.map { it.toFloat() }?.toFloatArray()

                // 수정된 CourseEntity 구조에 맞게 매핑
                val entity = CourseEntity(
                    // 기존에 동일한 firebaseId가 있다면 해당 ID를 찾아 유지해야 중복 생성이 안 됨
                    id = localEntities.find { it.firebaseId == doc.id }?.id ?: 0,
                    firebaseId = doc.id,

                    // 핵심 필드 꺼내기 (검색/필터링용)
                    distance = courseModel.distance,
                    minLat = courseModel.minLat,
                    maxLat = courseModel.maxLat,
                    minLng = courseModel.minLng,
                    maxLng = courseModel.maxLng,

                    // 리스트와 객체는 JSON으로 직렬화
                    locationPointsJson = gson.toJson(courseModel.locationPoints),
                    scoresJson = gson.toJson(courseModel.scores),

                    // AI 및 검색 관련 필드
                    embeddingText = doc.get("embedding_text") as? String,
                    address = doc.get("address") as? String,
                    landmark = doc.get("landmark") as? String,
                    vector = floatVector
                )

                courseBox.put(entity)
            }

            Log.d("RUNUP_SYNC", "✅ 동기화 완료 (현재 로컬 총 개수: ${courseBox.count()})")
            Result.success()
        } catch (e: Exception) {
            Log.e("RUNUP_SYNC", "❌ 동기화 실패: ${e.message}")
            Result.retry()
        }
    }
}