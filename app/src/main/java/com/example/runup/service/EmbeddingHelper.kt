package com.example.runup.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null // 모델 추출기
    private val vocab = mutableMapOf<String, Int>() // 단어사전
    private val MAX_SEQ_LEN = 128 // 최대 임베딩 가능 문장 길이

    // 모델 로드 생성자
    init {
        try {
            val model = FileUtil.loadMappedFile(context, "multilingual_minilm.tflite")
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(model, options)
            loadVocab()
            Log.d("RUNUP_MODEL", "✅ 최종 모델 & Vocab 준비 완료")
        } catch (e: Exception) {
            Log.e("RUNUP_MODEL", "❌ 초기화 실패: ${e.message}")
        }
    }

    // assets/vocab.txt (단어사전) 가져오는 함수
    private fun loadVocab() {
        try {
            vocab.clear()
            val assetManager = context.assets
            var currentId = 0
            assetManager.open("vocab.txt").bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val word = line.trim()
                    if (word.isNotEmpty() && !word.startsWith("[unused")) { // 비어있거나 unused 제공하고 전부 단어 사전에 추가
                        vocab[word] = currentId
                    }
                    currentId++
                }
            }
            Log.d("RUNUP_MODEL", "📊 Vocab 로드 완료! (총 ID 범위: $currentId)")
        } catch (e: Exception) {
            Log.e("RUNUP_MODEL", "❌ Vocab 로드 실패: ${e.message}")
        }
    }

    // 단어 사전 보고 사용자의 쿼리 번호로 매핑
    private fun wordPieceTokenize(text: String): IntArray {
        val ids = mutableListOf<Int>()

        // 시작 토큰을 0 (<s>)으로 변경
        ids.add(0)

        val spSpace = "\u2581" // ▁ 기호

        // 서버는 ':'와 '.' 앞에 공백(▁)을 붙이지 않고 독립적으로 처리할 때가 많음.
        val normalizedText = spSpace + text.replace(" ", spSpace)
            .replace(":", ":") // 기호 앞 공백 방지
            .replace(".", ".")

        var i = 0
        while (i < normalizedText.length) {
            var subword = ""
            var end = Math.min(i + 20, normalizedText.length)
            var found = false

            while (end > i) {
                val candidate = normalizedText.substring(i, end)
                if (vocab.containsKey(candidate)) {
                    subword = candidate
                    ids.add(vocab[candidate]!!)
                    i = end
                    found = true
                    break
                }
                end--
            }

            if (!found) {
                ids.add(vocab["<unk>"] ?: 3)
                i++
            }
        }

        // 종료 토큰을 2 (</s>)로 변경
        if (ids.size < MAX_SEQ_LEN) ids.add(2)

        // 패딩 토큰을 1 (<pad>)로 변경 (XLM-R 표준)
        val result = IntArray(MAX_SEQ_LEN) { 1 }
        for (idx in ids.indices) {
            if (idx >= MAX_SEQ_LEN) break
            result[idx] = ids[idx]
        }
        return result
    }

    fun getEmbedding(text: String): FloatArray {
        val inputIds = wordPieceTokenize(text) // [0, 152942, ...] 확인 완료된 토크나이저
        val attentionMask = IntArray(MAX_SEQ_LEN) { 0 }
        for (i in inputIds.indices) {
            if (inputIds[i] != 1) attentionMask[i] = 1
        }

        // 입력 구성 (서버와 동일한 input_1, input_3 순서)
        val inputs = arrayOf(
            arrayOf(attentionMask),
            arrayOf(inputIds)
        )

        // 출력 설정: 서버와 동일하게 [1][384] (첫 번째 토큰의 벡터만 받기)
        // TFLite 모델이 1x128x384를 뱉는다면, 우리는 그중 [0][0]만 쓸 것입니다.
        val output = Array(1) { FloatArray(384) }
        val outputs = mutableMapOf<Int, Any>(0 to output)

        try {
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)

            // 서버의 [:, 0, :] 와 동일하게 첫 번째 벡터 추출
            val clsVector = output[0] // 이미 0번째 인덱스의 384개 값이 담겨있음
            Log.d("RUNUP_VECTOR_DEBUG", "--- [Android] Raw Vector (정규화 전 10개) ---")
            Log.d("RUNUP_VECTOR_DEBUG", clsVector.take(10).joinToString(", "))

            // L2 정규화 (서버의 np.linalg.norm과 동일한 로직)
            var sumSquared = 0f
            for (v in clsVector) sumSquared += v * v
            val norm = Math.sqrt(sumSquared.toDouble()).toFloat()

            val normalizedVector = FloatArray(384) { i ->
                if (norm > 1e-6f) clsVector[i] / norm else clsVector[i]
            }
            Log.d("RUNUP_VECTOR_DEBUG", "--- [Android] normalized Vector (정규화 후 10개) ---")
            Log.d("RUNUP_VECTOR_DEBUG", normalizedVector.take(10).joinToString(", "))
            return normalizedVector


        } catch (e: Exception) {
            Log.e("RUNUP_MODEL", "❌ 추론 실패: ${e.message}")
            return FloatArray(384)
        }
    }
}

