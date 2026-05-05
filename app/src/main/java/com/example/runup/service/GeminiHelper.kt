package com.example.runup.service

import android.util.Log
import com.example.runup.BuildConfig
import com.example.runup.data.local.UserPreferenceDataSource
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.local.objectbox.entity.CourseEntity_
import com.example.runup.domain.model.Course
import com.example.runup.ui.util.mapper.CourseMapper
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder

class GeminiHelper(
    private val embeddingHelper: EmbeddingHelper, // 검색을 위해 필요
    private val courseBox: Box<CourseEntity>,
    private val courseMapper: CourseMapper,
    private val userPreferenceDataSource: UserPreferenceDataSource
) {
    private val TAG = "RUNUP_GEMINI_SEARCH"

    // 1. Gemini 모델 설정
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-flash-lite-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.1f // 판단의 일관성을 위해 낮게 설정
            topK = 40
            topP = 0.95f
        }
    )

    private fun getSearchResult(
        queryText: String,
        userCity: String,
        userLoc: GeoPoint,
        maxSearchDistance: Int,
        targetDist: Double
    ): List<CourseEntity> {

        val userQueryVector = embeddingHelper.getEmbedding(queryText)
        val maxRadius = maxSearchDistance * 0.00001
        val minCourseDistance = targetDist.toLong()

        // 1단계: 주변 코스 필터링
        val localCandidates = courseBox.query {
            greater(CourseEntity_.maxLat, userLoc.latitude - maxRadius)
            less(CourseEntity_.minLat, userLoc.latitude + maxRadius)
            greater(CourseEntity_.maxLng, userLoc.longitude - maxRadius)
            less(CourseEntity_.minLng, userLoc.longitude + maxRadius)
            contains(CourseEntity_.address, userCity, QueryBuilder.StringOrder.CASE_INSENSITIVE)

            greaterOrEqual(CourseEntity_.distance, minCourseDistance)
        }.find()

        if (localCandidates.isEmpty()) {
            Log.d(TAG, "📍 [검색 결과] 주변에 조건에 맞는 코스가 없습니다.")
            return emptyList()
        }

        // 2단계: 필터링된 후보군 내에서 벡터 검색
        val finalResults = courseBox.query {
            greater(CourseEntity_.maxLat, userLoc.latitude - maxRadius)
            less(CourseEntity_.minLat, userLoc.latitude + maxRadius)
            greater(CourseEntity_.maxLng, userLoc.longitude - maxRadius)
            less(CourseEntity_.minLng, userLoc.longitude + maxRadius)
            contains(CourseEntity_.address, userCity, QueryBuilder.StringOrder.CASE_INSENSITIVE)
            // 📍 2단계 쿼리에도 동일하게 거리 필터 적용
            greaterOrEqual(CourseEntity_.distance, minCourseDistance)
            nearestNeighbors(CourseEntity_.vector, userQueryVector, localCandidates.size)
        }.findWithScores()

        // ── 🔹 3단계: 로그 출력 및 최종 5개 반환 📍 ──
        val topResults = finalResults.take(5)

        Log.d(TAG, "==========================================================")
        Log.d(TAG, "🚀 AI 코스 추천 최종 결과 (Top ${topResults.size})")
        Log.d(TAG, "🔍 검색어: $queryText")
        Log.d(TAG, "----------------------------------------------------------")

        topResults.forEachIndexed { index, scoreObject ->
            val entity = scoreObject.get()
            val score = scoreObject.score

            Log.d(TAG, "[$index] ID: ${entity.firebaseId}")
            Log.d(TAG, "    - 유사도 점수: $score")
            Log.d(TAG, "    - 임베딩 텍스트: ${entity.embeddingText}")
            Log.d(TAG, "----------------------------------------------------------")
        }
        Log.d(TAG, "==========================================================")

        return topResults.map { it.get() }
    }

    suspend fun performAiSearch( // AI 검색 통합 함수
        userPrompt: String,
        userCity : String,
        userLoc: GeoPoint,
        maxSearchDistance: Int,
        targetDist: Double
    ): List<Pair<Course, String>> {
        Log.d(TAG, "🚀 Gemini AI 검색 시작 | 입력: '$userPrompt'")

        return try {
            // 벡터 검색으로 후보군 가져오기
            val topEntities = getSearchResult(userPrompt, userCity, userLoc, maxSearchDistance, targetDist)
            // ── 🔹 후보가 없으면 여기서 종료 (Gemini 호출 안 함, false 반환) 📍
            if (topEntities.isEmpty()) return emptyList()

            val contextText = topEntities.mapIndexed { index, entity ->
                "[후보 ${index + 1}]\n- ID: ${entity.firebaseId}\n- 상세: ${entity.embeddingText}"
            }.joinToString("\n\n")

            // 3. Gemini 호출
            val aiResponse = fetchGeminiResponse(userPrompt, contextText) ?: ""
            Log.d(TAG, aiResponse)

            if (aiResponse.isNotBlank()) {
                userPreferenceDataSource.incrementAiSearchCount()
            }

            // 4. 여러 개의 추천 결과 파싱 (ID: ..., Reason: ... 쌍을 모두 찾음)
            val recommendedList = mutableListOf<Pair<Course, String>>()

            // 응답에서 ID와 Reason 추출 로직 (정규식이나 줄 단위 분석)
            val lines = aiResponse.lines()
            var currentId: String? = null

            lines.forEach { line ->
                // 한 줄에 ID:와 Reason:이 모두 포함되어 있는지 확인
                if (line.contains("ID:", ignoreCase = true) && line.contains("Reason:", ignoreCase = true)) {

                    // 1. ID 추출: "ID:"와 "|" 사이의 값을 가져옴
                    val extractedId = line.substringAfter("ID:")
                        .substringBefore("|")
                        .trim()

                    // 2. Reason 추출: "Reason:" 이후의 모든 값을 가져옴
                    val extractedReason = line.substringAfter("Reason:").trim()

                    // 3. 추출된 ID로 실제 Course 객체 매핑
                    val course = topEntities.find { it.firebaseId == extractedId }?.let { entity ->
                        courseMapper.toDomain(entity)
                    }

                    if (course != null) {
                        recommendedList.add(course to extractedReason)
                        Log.d(TAG, "✅ 파싱 성공: ${course.id} / $extractedReason")
                    } else {
                        Log.e(TAG, "❌ 해당 ID($extractedId)에 맞는 코스 데이터를 찾을 수 없습니다.")
                    }
                }
            }

            recommendedList

        } catch (e: Exception) {
            Log.e(TAG, "💥 예외 발생: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Gemini API 실제 호출부
     */
    private suspend fun fetchGeminiResponse(userPrompt: String, contextText: String): String? {
        val finalPrompt = """
        당신은 러닝 코스 전문가입니다. 
        제공된 [Context]의 후보들 중 사용자의 요청([Query])에 가장 적합한 코스를 **최대 2개** 선정하여 우선순위가 높은 순서대로 나열하세요.
        
        [Context]
        $contextText
        
        [Query]
        "$userPrompt"
        
        [특징 분류 기준]
        - High (0.66 이상): 밝음 / 많음 / 높음
        - Ordinary (0.33 이상): 평범함
        - Low (0.0 이상): 어두움 / 적음 / 낮음
        * '평범함'은 사용자의 특정 요청(밝음/어두움 등)에 대해 완벽한 일치가 아닌 '일부 불일치'로 간주합니다.(단 사용자가 평범함을 요청했을 경우, 평범함은 완벽한 일치 입니다)
        
        [Writing Rules (경우의 수)]
        1. **모든 조건 만족 시**: "[해당하는 특징]을 만족하는 코스에요!" 라고 작성할 것.
        2. **장소 관련 얘기만 있을시**: "[Query의 장소명] 주변의 코스에요!" 라고 간결하게 대답. 
        3. **일부 조건 불일치 시 (예: 밝은 곳 요청 시 '평범함' 데이터)**: "[일치하는 특징]은 만족하지만 [일치하지 않는 특징]인데 괜찮으실까요?"라고 질문할 것.
        4. **장소는 일치하지만 특징이 정반대일 시**: "[Query의 장소명] 주변 코스이지만, 요청하신 [요청한 특징]과는 반대로 [현재 특징]인 코스인데 괜찮으실까요?"라고 질문할 것.
        5. **장소 불일치/부재 시**: [Query]에 특정 장소가 명시되었으나 [Context]의 '주변 장소' 데이터에 해당 키워드가 전혀 존재하지 않는다면, 무조건 "[Query의 장소명] 주변의 코스는 없는 것 같아요..."라고 작성하고 ID는 'None'으로 표기할 것. (유사도가 높아도 장소 이름이 다르면 추천 금지)
        6. [Query] 에 장소에 대한 얘기가 없을 시 - [일치하는 특징] 이 존재하면 3번과 같이 출력 / [일치하는 특징] 이 존재하지 않으면 "적절한 장소가 없는 것 같아요..." 로 출력
        
        [Constraint]
        - Reason은 반드시 한 문장으로 간결하게 작성할 것.
        - 불필요한 인사나 부연 설명은 절대 금지.
        - 적합한 코스가 2개 미만이라면 찾은 만큼만 결과물에 포함하세요.
        - 사용자의 요청과 특징이 정반대이더라도, [Writing Rules] 4번에 해당하는 경우에만 후보에 포함시키세요. 그 외에 장소마저 일치하지 않으면서 특징이 정반대인 경우는 제외하세요.
        
        [Output Format]
        ID: [선택한 코스의 ID] | Reason: [Writing Rules를 참고하여 작성]
        ID: [선택한 코스의 ID] | Reason: [Writing Rules를 참고하여 작성]
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(finalPrompt)
            response.text
        } catch (e: Exception) {
            // 이 로그가 모든 걸 말해줄 겁니다!
            Log.e("RUNUP_GEMINI_ERROR", "🔥 에러 발생 원인: ${e.message}", e)
            null
        }
    }
}