package com.example.runup.service

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

class GemmaHelper(private val context: Context) {
    private var llmInference: LlmInference? = null
    private val TAG = "RUNUP_GEMMA"
    private val MODEL_NAME = "gemma-2b-it-cpu-int4.bin" // assets에 넣은 파일명과 일치시켜주세요.

    init {
        try {
            // 1. 모델 경로 확보 (복사 로직 포함)
            val modelPath = prepareModelPath()

            // 2. LlmInference 설정
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(128)    // 검색 쿼리는 짧으므로 제한
                .setTopK(40)          // 답변의 다양성 조절
                .setTemperature(0.1f) // 0에 가까울수록 형식이 고정됨 (중요!)
                .setRandomSeed(42)
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
        val modelFile = File(context.filesDir, MODEL_NAME)

        // 파일이 없거나 크기가 0인 경우에만 복사 (1.3GB이므로 매번 복사하면 안 됨)
        if (!modelFile.exists() || modelFile.length() <= 0) {
            Log.d(TAG, "🚚 모델 복사 시작 (약 1.3GB)... 잠시만 기다려주세요.")
            context.assets.open(MODEL_NAME).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "✅ 모델 복사 완료!")
        }
        return modelFile.absolutePath
    }

    /**
     * 사용자 질문을 검색 쿼리 형식으로 변환합니다.
     */
    fun convertToSearchQuery(userPrompt: String): String {
        if (llmInference == null) return userPrompt // 초기화 실패 시 원문 반환

        // 💡 [핵심] Gemma에게 역할을 부여하는 프롬프트 엔지니어링
        val prompt = """
            <start_of_turn>user
            당신은 러닝 코스 검색 전문가입니다. 사용자 질문을 분석해서 오직 아래 형식으로만 한 문장으로 변환하세요. 다른 설명은 절대 하지 마세요.
            
            형식:'근처 장소: 장소1, 장소2, 장소3. 주소: 대구광역시 북구 산격동. 특징: 조명이 매우 어두움, 주변에 사람 매우 적은 분위기, 러닝 난이도 매우 낮은 수준입니다.'
            
            사용자 질문: "$userPrompt"
            
            조건1: 장소, 주소, 특징에 해당하지 않는 항목이 있다면 억지로 채우지 말고 비워 높으세요. 
            조건2: 특징은 다음과 같이 구성하시면 됩니다. 조명 - [매우/적당히] [밝음/어두움] 중 선택하거나 평범함, 주변 사람 - [매우/적당히] [많은/적은] 중 선택하거나 평범한, 러닝 난이도 - [매우/적당히] [높은/낮은] 중 선택하거나 평범함
            
            <end_of_turn>
            <start_of_turn>model
        """.trimIndent()

        return try {
            val response = llmInference?.generateResponse(prompt) ?: ""
            Log.d(TAG, "🤖 Gemma 변환 결과: $response")
            response.trim()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 추론 실패: ${e.message}")
            userPrompt
        }
    }
}