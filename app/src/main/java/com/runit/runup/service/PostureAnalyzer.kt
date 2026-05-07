package com.runit.runup.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostureAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tflite: Interpreter? = null

    // 🌟 1. 하드코딩된 배열 대신 빈 리스트로 선언
    private var aiLabels: List<String> = emptyList()

    // 입력 버퍼는 50 x 20 고정 (센서 스펙이 바뀌지 않는 한 고정)
    private val aiInput = Array(1) { Array(50) { FloatArray(20) } }

    // 🌟 2. 출력 배열도 초기에는 0으로 두고, 라벨을 읽은 후 동적으로 크기 할당
    private var aiOutput = Array(1) { FloatArray(0) }

    init {
        try {
            // 🌟 3. 파일에서 라벨을 읽어옵니다. (7개면 7개, 10개면 10개로 자동 세팅)
            aiLabels = loadLabels("aiLabels.txt")

            // 🌟 4. 읽어온 라벨 개수에 맞춰서 모델 출력 배열의 크기를 동적으로 세팅합니다.
            aiOutput = Array(1) { FloatArray(aiLabels.size) }

            // 5. TFLite 모델 로드
            val fileDescriptor = context.assets.openFd("posture_model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val mappedByteBuffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
            tflite = Interpreter(mappedByteBuffer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🌟 텍스트 파일을 읽어서 List<String>으로 만들어주는 도우미 함수
    private fun loadLabels(fileName: String): List<String> {
        return try {
            // 파일을 읽어서 줄바꿈 단위로 쪼개고, 앞뒤 공백을 없애서 리스트로 만듭니다.
            context.assets.open(fileName).bufferedReader().readLines().map { it.trim() }
        } catch (e: Exception) {
            e.printStackTrace()
            // 만약 파일이 없거나 오류가 나면 뻗지 않도록 기본값 제공 (안전장치)
            listOf("정지", "걷기", "뛰기", "왼쪽 짝다리", "오른쪽 짝다리", "팔자 걸음", "과도한 뒤꿈치 착지")
        }
    }

    // 50개 데이터를 분석해서 (라벨, 확률) 반환
    fun analyze(window: List<FloatArray>): Pair<String, Float>? {
        // 라벨이 없거나 모델이 없으면 조기 종료
        if (tflite == null || window.size < 50 || aiLabels.isEmpty()) return null

        for (t in 0 until 50) {
            for (c in 0 until 20) {
                aiInput[0][t][c] = window[t][c]
            }
        }

        // 추론 실행
        tflite?.run(aiInput, aiOutput)

        val probabilities = aiOutput[0]
        var maxIdx = 0
        var maxProb = probabilities[0]

        // 🌟 5. 하드코딩된 숫자 '7' 대신 라벨의 개수(aiLabels.size)만큼 반복!
        for (i in 1 until aiLabels.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        return Pair(aiLabels[maxIdx], maxProb)
    }

    fun close() {
        tflite?.close()
    }
}