package com.example.runup.service

import android.content.Context
import android.util.Log
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.local.objectbox.entity.CourseEntity_
import com.example.runup.data.source.remote.course.CourseDataSource
import com.example.runup.domain.model.Course
import com.google.gson.Gson
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import java.io.File


class GemmaHelper(
    private val context: Context,
    private val embeddingHelper: EmbeddingHelper,
    private val courseBox: Box<CourseEntity>,
    private val gson: Gson // Hilt에서 주입
) {
    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null
    private val TAG = "RUNUP_GEMMA"
    private val MODEL_NAME = "gemma-3n-E2B-it-int4.litertlm" // assets에 넣은 파일명과 일치시켜주세요.

    init {
        try {
            // 1. 모델 경로 확보 (복사 로직 포함)
            val modelPath = prepareModelPath()

            // 2. LlmInference 설정 (모델 로드만 담당)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(750)
                .build()

            // 3. 엔진 생성
            llmInference = LlmInference.createFromOptions(context, options)



            Log.d(TAG, "✅ Gemma 모델 로드 성공!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gemma 초기화 실패: ${e.message}")
        }
    }

    /**
     * Assets의 모델을 내부 저장소로 복사하고 경로를 반환합니다.
     */
    private fun prepareModelPath(): String {
        // 내부 저장소(Android/data/com.example.runup/files) 경로
        val modelFile = File(context.filesDir, MODEL_NAME)

        if (modelFile.exists()) {
            Log.d(TAG, "✅ 외부에서 넣은 모델 발견: ${modelFile.absolutePath} (크기: ${modelFile.length()})")
            return modelFile.absolutePath
        } else {
            // 파일이 없으면 에러 로그만 찍고 빈 문자열 반환
            Log.e(TAG, "❌ 모델 파일이 없습니다! 수동으로 넣어주세요.")
            return ""
        }
    }

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
     * 사용자 질문을 검색 쿼리 형식으로 변환합니다.
     */
    fun convertToSearchQuery(userPrompt: String): String {
        if (llmInference == null) return userPrompt // 초기화 실패 시 원문 반환

        // 💡 [핵심] Gemma에게 역할을 부여하는 프롬프트 엔지니어링
        val prompt = """
        <start_of_turn>user
        당신은 사용자 질문을 '러닝 코스 검색 쿼리'로 변환하는 기계적인 전문가입니다. 
        불필요한 설명이나 질문 반복은 절대로 하지 마세요. 오직 [출력 형식]만 뱉으세요.
    
        [출력 형식]
        지역: ~시 ~군. 근처 장소: 장소1, 장소2. 특징: 조명 상태, 주변 사람 상태, 러닝 난이도 상태.
    
        [작성 규칙]
        1. **근처 장소**: 질문에 언급된 고유 명사나 장소를 최대 3개까지 콤마(,)로 구분하여 나열하세요.
        2. 정보가 없는 항목은 '.'만 찍으세요.
        3. 특징은 반드시 다음 키워드 중 선택하세요: [매우/적당히] [밝음/어두움], [매우/적당히] [많은/적은], [매우/적당히] [높은/낮은], 딱히 해당되지 않으면 [평범함] 으로 선택하세요
    
        [변환 예시]
        사용자 질문: "부산 다대포 해수욕장 근처 밝은 코스 알려줘"
        모델 출력: 지역: 부산. 근처 장소: 다대포 해수욕장. 특징: 조명 적당히 밝음, 주변 사람 평범한 분위기, 러닝 난이도 평범한 수준입니다.
        
        *주의* 변환 예시는 형식에 대한 예시일 뿐입니다. 형식만 참고하세요!
    
        사용자 질문: "$userPrompt"
        <end_of_turn>
        <start_of_turn>model
        지역: """.trimIndent() // 👈 여기에 "근처 장소: "를 미리 줍니다.

        return try {
            val response = llmInference?.generateResponse(prompt) ?: ""
            Log.d(TAG, "🤖 Gemma 변환 결과: $response")
            response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 추론 실패: ${e.message}")
            userPrompt
        }
    }

    /**
     * 🚀 최종 통합 AI 검색 함수
     * 1. Gemma가 질문을 정규화된 쿼리로 변환
     * 2. 변환된 쿼리를 벡터로 변환
     * 3. ObjectBox에서 유사도 검색 실행
     */
    suspend fun performAiSearch(userPrompt: String): Pair<Course?, String> {
        val TAG = "RUNUP_AI_SEARCH"
        Log.d(TAG, "🚀 AI 검색 시작 | 사용자 입력: '$userPrompt'")

        return try {
            // DB에서 벡터 검색 실행 (Top 3)
            Log.d(TAG, "❷ 벡터 검색 실행 중...")
            val topEntities = getSearchResult(userPrompt, "대구")
            Log.d(TAG, "➡️ 검색된 후보 개수: ${topEntities.size}")
            topEntities.forEachIndexed { index, entity ->
                Log.d(TAG, "      [$index] 후보 ID: ${entity.firebaseId}")
                Log.d(TAG, "      텍스트: ${entity.embeddingText}")
            }

            if (topEntities.isEmpty()) {
                Log.w(TAG, "⚠️ 검색 결과가 없어 종료합니다.")
                return Pair(null, "죄송합니다. 근처에서 적절한 코스를 찾지 못했어요.")
            }

            // Gemma에게 최종 판단 맡기기
            Log.d(TAG, "❸ 최종 추천 분석 중 (Gemma 호출)...")
            val aiResponse = recommendFinalCourse(userPrompt, topEntities)
            Log.d(TAG, "➡️ Gemma 원문 응답:\n$aiResponse")

            // 응답 파싱 수정
            // 숫자만 있거나 course+숫자 형태를 모두 찾음
            val idRegex = "(course)?(\\d+)".toRegex()
            val matchResult = idRegex.find(aiResponse)

            val recommendedId = if (matchResult != null) {
                val numberPart = matchResult.groupValues[2] // 숫자 부분만 추출 (ex: "36")
                "course$numberPart" // 무조건 "course"를 붙여서 표준화
            } else {
                null
            }

            val reasonRegex = "추천이유:\\s*(.*)".toRegex()
            val rawReason = reasonRegex.find(aiResponse)?.groupValues?.get(1)
                ?: aiResponse.replace(idRegex, "").replace(Regex("[*#]"), "").trim()

            val recommendationReason = if (rawReason.length > 5) rawReason else "요청하신 조건에 가장 적합한 코스입니다."

            Log.d(TAG, "❹ 파싱 결과 - 추천ID: $recommendedId | 이유: $recommendationReason")

            // 5단계: ID로 로컬 DB에서 실제 Course 객체 찾기
            val finalCourse = topEntities.find { it.firebaseId == recommendedId }?.let { entity ->
                entity.courseDataJson?.let { json ->
                    gson.fromJson(json, Course::class.java)
                }
            }

            if (finalCourse != null) {
                Log.d(TAG, "✅ 최종 코스 매칭 성공: ${finalCourse.id}")
            } else {
                Log.e(TAG, "❌ 파싱된 ID($recommendedId)와 일치하는 코스를 후보군에서 찾을 수 없습니다.")
            }

            Pair(finalCourse, recommendationReason)

        } catch (e: Exception) {
            Log.e(TAG, "💥 AI 검색 중 예외 발생: ${e.message}", e)
            Pair(null, "검색 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
        }
    }

    fun recommendFinalCourse(userPrompt: String, searchResults: List<CourseEntity>): String {
        // 1. 검색된 코스들을 텍스트로 변환
        val contextText = searchResults.mapIndexed { index, entity ->
            """
            [후보 ${index + 1}]
            - 코스ID: ${entity.firebaseId}
            - 상세내용: ${entity.embeddingText}
            
            """.trimIndent()
        }.joinToString("\n\n")
        Log.d("check", contextText)

        val finalPrompt = """
        <start_of_turn>user
        [Context]
        $contextText
        
        [Query]
        "$userPrompt"
        
        [Judgment Rules]
        - IF category NOT in [Query] -> (Middle)
        - IF category matches [Query] -> (High)
        - IF category is opposite (밝음<->어두움, 많음<->적음, 높음<->낮음) -> (Low)
        - Place: Only name from [Query]
        
        [Example]
        Query: "IT대 근처 밝은 코스"
        Context: {Place: IT대, 밝기: 어두움, 사람: 적음, 난이도: 낮음}
        Output:
        ID: course00
        Place: IT대
        밝기: 매우 어두움(Low)
        사람: 매우 적음(Middle)
        난이도: 매우 낮음(Middle)
        
        [Output Format]
        ID: [ID]
        Place: [Name]
        밝기: [Context_Value](Rating)
        사람: [Context_Value](Rating)
        난이도: [Context_Value](Rating)
        <end_of_turn>
        <start_of_turn>model
        ID: """.trimIndent()

        return try {
            val engine = llmInference ?: run {
                Log.e("GEMMA_DEBUG", "❌ llmInference가 null입니다!")
                return ""
            }

            Log.d("GEMMA_DEBUG", "1. 세션 생성 시도")
            val session = LlmInferenceSession.createFromOptions(
                engine,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(0)
                    .setTopP(0.0f)
                    .setTemperature(0.0f)
                    .build()
            )
            Log.d("GEMMA_DEBUG", "2. 세션 생성 완료")

            session.addQueryChunk(finalPrompt)
            Log.d("GEMMA_DEBUG", "3. addQueryChunk 완료 | 프롬프트 길이: ${finalPrompt.length}")

            val result = session.generateResponse() ?: ""
            Log.d("GEMMA_DEBUG", "4. 응답 수신: '$result'")

            session.close()
            result
        } catch (e: Exception) {
            Log.e("GEMMA_DEBUG", "❌ 예외 발생: ${e.message}", e)
            ""
        }
    }
}