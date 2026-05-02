package com.example.runup.service

import android.util.Log
import com.example.runup.BuildConfig
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.local.objectbox.entity.CourseEntity_
import com.example.runup.domain.model.Course
import com.example.runup.ui.util.mapper.CourseMapper
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder

class GeminiHelper(
    private val embeddingHelper: EmbeddingHelper, // 검색을 위해 필요
    private val courseBox: Box<CourseEntity>,
    private val gson: Gson = Gson(),
    private val courseMapper: CourseMapper,
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

    private fun getSearchResult(queryText: String, userCity: String): List<CourseEntity> {
        // 문장을 384차원 벡터로 변환
        val userQueryVector = embeddingHelper.getEmbedding(queryText)

        val resultsWithScores = courseBox.query {
            // 1. 지역 필터링 추가 (매개변수 순서 주의!)
            contains(CourseEntity_.address, userCity, QueryBuilder.StringOrder.CASE_INSENSITIVE)
            // 2. 기존 벡터 검색 로직
            nearestNeighbors(CourseEntity_.vector, userQueryVector, 3)
        }.findWithScores()

        // 로그 출력 (디버깅용 - 기존 유지)
        Log.d("RUNUP_SEARCH_DEBUG", "🔎 검색 결과 개수: ${resultsWithScores.size} | 검색어: '$queryText'")

        resultsWithScores.forEachIndexed { index, scoreObject ->
            val entity = scoreObject.get()
            Log.d("RUNUP_SEARCH_DEBUG", "   [$index] ID: ${entity.firebaseId} | 점수: ${scoreObject.score}")
        }

        // UI에는 검색 결과 순서대로 Course 객체 리스트 반환
        return resultsWithScores.map { it.get() }
    }
    /**
     * 최종 AI 검색 통합 함수
     */
    suspend fun performAiSearch(userPrompt: String, userCity : String): List<Pair<Course, String>> {
        Log.d(TAG, "🚀 Gemini AI 검색 시작 | 입력: '$userPrompt'")

        return try {
            // 1. 벡터 검색으로 후보군 가져오기
            val topEntities = getSearchResult(userPrompt, userCity)
            if (topEntities.isEmpty()) return emptyList()

            val contextText = topEntities.mapIndexed { index, entity ->
                "[후보 ${index + 1}]\n- ID: ${entity.firebaseId}\n- 상세: ${entity.embeddingText}"
            }.joinToString("\n\n")

            // 3. Gemini 호출
            val aiResponse = fetchGeminiResponse(userPrompt, contextText) ?: ""
            Log.d(TAG, aiResponse)

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
        너는 러닝 코스 추천 전문가야. [Context]의 후보들 중 [Query]에 가장 적합한 **단 하나의 코스**를 선정해.

        [Context]
        $contextText

        [Query]
        "$userPrompt"

        [Writing Rules (경우의 수)]
        1. **모든 조건 만족 시**: "[해당하는 특징]을 모두 만족하여 이 코스가 가장 적절해요!"라고 작성할 것.
        2. **일부 조건 불일치 시**: "[일치하는 특징]은 만족하지만 [일치하지 않는 특징]인데 괜찮으실까요?"라고 질문할 것.
        3. **적절한 코스/장소 없을 시**: "[Query의 장소명] 주변의 코스는 없는 것 같아요..."라고 작성하고 ID는 'None'으로 표기할 것.
        4. [Query] 에 장소에 대한 얘기가 없을 시 - [일치하는 특징] 이 존재하면 2번과 같이 출력 / [일치하는 특징] 이 존재하지 않으면 "적절한 장소가 없는 것 같아요..." 로 출력

        [Constraint]
        - 반드시 한 문장으로 간결하게 작성할 것.
        - 불필요한 인사나 부연 설명은 절대 금지.

        [Output Format]
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