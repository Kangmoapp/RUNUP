package com.runit.runup.domain.usecase

import com.runit.runup.domain.model.AuthResult
import com.runit.runup.domain.model.Course
import com.runit.runup.domain.model.Node
import com.runit.runup.domain.model.RunRecord
import com.runit.runup.domain.model.Scores
import com.runit.runup.domain.repository.UserRepository
import javax.inject.Inject

class RecordRunningUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        recordedNodes: List<Node>,
        totalDistance: Int,
        totalTime: Int,
        scores: Scores,
    ): AuthResult<Boolean> {

        // 1. 노드 리스트가 비어있을 경우에 대한 예외 처리
        if (recordedNodes.isEmpty()) {
            return AuthResult.Fail("기록된 경로가 없습니다.")
        }

        val updatedNodes = recordedNodes.map { node ->
            node.copy(
                score = Scores(
                    brightScore = scores.brightScore,
                    crowdedScore = scores.crowdedScore,
                    hardScore = scores.hardScore,
                )
            )
        }

        // 2. 경계 좌표(Bounding Box) 계산
        val minLat = recordedNodes.minOf { it.locationPoint.latitude }
        val maxLat = recordedNodes.maxOf { it.locationPoint.latitude }
        val minLng = recordedNodes.minOf { it.locationPoint.longitude }
        val maxLng = recordedNodes.maxOf { it.locationPoint.longitude }

        // 2. Scores 평균값 계산
        val nodeCount = updatedNodes.size.toDouble()
        val averageScores = Scores(
            brightScore = updatedNodes.sumOf { it.score.brightScore } / nodeCount,
            crowdedScore = updatedNodes.sumOf { it.score.crowdedScore } / nodeCount,
            hardScore = updatedNodes.sumOf { it.score.hardScore } / nodeCount
        )

        // 3. Course 객체 구성
        val course = Course(
            id = "course_${System.currentTimeMillis()}", // 고유 ID 생성 (필요에 따라 수정)
            distance = totalDistance,
            locationPoints = updatedNodes,
            minLat = minLat,
            maxLat = maxLat,
            minLng = minLng,
            maxLng = maxLng,
            scores = averageScores // 기본값 설정
        )

        // 4. RunRecord 객체 구성
        val runRecord = RunRecord(
            recordDate = System.currentTimeMillis(), // 저장 시점의 타임스탬프
            time = totalTime,
            course = course
        )

        // 5. Repository 호출
        return userRepository.saveRunRecord(runRecord)
    }
}