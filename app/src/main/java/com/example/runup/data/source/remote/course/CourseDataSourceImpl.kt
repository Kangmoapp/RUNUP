package com.example.runup.data.source.remote.course

import android.util.Log
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.local.objectbox.entity.CourseEntity_
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Scores
import com.example.runup.service.EmbeddingHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import io.objectbox.Box
import io.objectbox.kotlin.query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class CourseDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val courseBox: Box<CourseEntity>,
    private val embeddingHelper: EmbeddingHelper,
    private val gson: Gson // Hilt에서 주입
) : CourseDataSource {
    // #1. [코스 병합 및 저장하는 함수]
    override suspend fun saveCourse(course: Course): AuthResult<Boolean> {
        return try {
            mergeAndSaveCourse(course)
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 저장 실패", e)
        }
    }

    private suspend fun mergeAndSaveCourse(newCourse: Course){
        val eps = 0.0003 // 30m
        val margin = eps
        val minSamples = 2 // 그룹이 이루어질 수 있는 최소 노드 개수

        // Bounding Box 계산 및 1차/2차 필터링
        val newMinLat = newCourse.locationPoints.minOf { it.locationPoint.latitude }
        val newMaxLat = newCourse.locationPoints.maxOf { it.locationPoint.latitude }
        val newMinLng = newCourse.locationPoints.minOf { it.locationPoint.longitude }
        val newMaxLng = newCourse.locationPoints.maxOf { it.locationPoint.longitude }

        //위도로 1차필터링
        val candidates = firestore.collection("Course")
            .whereGreaterThanOrEqualTo("maxLat", newMinLat - margin)
            .whereLessThanOrEqualTo("minLat", newMaxLat + margin)
            .get().await().toObjects(Course::class.java)

        //경도로 2차필터링
        val targetClusters = candidates.filter { existing ->
            existing.maxLng >= (newMinLng - margin) && existing.minLng <= (newMaxLng + margin)
        }

        // 모든 좌표를 하나로 모아 클러스터링 준비
        val allNodes = mutableListOf<Node>().apply {
            addAll(newCourse.locationPoints)
            targetClusters.forEach { addAll(it.locationPoints) }
        }

        // DBSCAN 실행
        val clusters = performDBSCAN(allNodes, eps, minSamples)

        // Firestore 업데이트 (클러스터 단위로 문서 생성/삭제)
        firestore.runTransaction { transaction ->
            // 카운터 문서 참조 및 현재 번호 읽기
            val metaRef = firestore.collection("Metadata").document("courseInfo")
            val metaSnap = transaction.get(metaRef)

            // 문서가 없으면 0부터 시작
            var lastNum = if (metaSnap.exists()) metaSnap.getLong("lastCourseNumber") ?: 0L else 0L

            // 기존 병합 대상 문서 삭제
            targetClusters.forEach { oldCourse ->
                transaction.delete(firestore.collection("Course").document(oldCourse.id))
            }

            // DBSCAN 결과로 나온 각 그룹을 새 번호로 저장
            clusters.forEach { clusterPoints ->
                if (clusterPoints.isNotEmpty()) {
                    lastNum++ // 번호 증가
                    val customId = "course$lastNum" // 예: course3

                    val simplifiedPoints = gridSimplify(clusterPoints, 0.00005) // 5미터 정사각형 격자 안에 들어온 점들은 평균내서 하나의 점으로 합침
                    val newDocRef = firestore.collection("Course").document(customId)

                    // finalCourse 객체 생성 시 id도 customId로 전달
                    val finalCourse = createCourseFromPoints(customId, simplifiedPoints)
                    transaction.set(newDocRef, finalCourse)
                }
            }

            // 업데이트된 마지막 번호를 다시 저장
            transaction.set(metaRef, mapOf("lastCourseNumber" to lastNum))
        }.await()
    }

    private fun performDBSCAN(nodes: MutableList<Node>, eps: Double, minSamples: Int): List<List<Node>> {
        val visited = mutableSetOf<Int>()
        val clusters = mutableListOf<List<Node>>()
        val noise = mutableSetOf<Int>()

        for (i in nodes.indices) {
            if (i in visited) continue //방문한 좌표이면 스킵,
            visited.add(i) //방문하지 않은 좌표면 방문한 좌표에 추가

            val neighbors = findNeighbors(i, nodes, eps)
            if (neighbors.size < minSamples) { //이웃이 두개가 안된다면(자기 자신) noise(혼자 있는 그룹) 에 추가
                noise.add(i)
            } else {
                val cluster = mutableListOf<Int>() //하나의 그룹에 들어가는 인덱스들 모음 리스트
                expandCluster(i, neighbors, nodes, cluster, visited, eps, minSamples)
                clusters.add(cluster.map { nodes[it] })
            }
        }
        return clusters
    }

    private fun expandCluster(root: Int, neighbors: List<Int>, nodes: List<Node>, cluster: MutableList<Int>, visited: MutableSet<Int>, eps: Double, minSamples: Int) {
        cluster.add(root)
        val queue = neighbors.toMutableList()
        var idx = 0
        while (idx < queue.size) {
            val nextPointIdx = queue[idx++]
            if (nextPointIdx !in visited) { // 방문한 좌표 아니라면
                visited.add(nextPointIdx) //방문좌표에 더해주고
                val nextNeighbors = findNeighbors(nextPointIdx, nodes, eps)
                if (nextNeighbors.size >= minSamples) {
                    queue.addAll(nextNeighbors.filter { it !in queue })
                }
            }
            if (cluster.none { it == nextPointIdx }) { // 클러스터에 nextPointIdx가 이미 존재하지 않는다면 추가해줌
                cluster.add(nextPointIdx)
            }
        }
    }

    private fun findNeighbors(index: Int, points: List<Node>, eps: Double): List<Int> {
        val neighbors = mutableListOf<Int>() //index 좌표 주변에 있는 이웃좌표 목록
        val p1 = points[index] //현재좌표
        for (i in points.indices) {
            val p2 = points[i]
            val dist = Math.sqrt(Math.pow(p1.locationPoint.latitude - p2.locationPoint.latitude, 2.0) + Math.pow(p1.locationPoint.longitude - p2.locationPoint.longitude, 2.0))
            if (dist <= eps) neighbors.add(i) //거리가 eps 이내면 이웃에 추가
        }
        return neighbors
    }

    private fun gridSimplify(nodes: List<Node>, gridSize: Double): List<Node> {
        return nodes.groupBy {
            // Node 내부의 locationPoint 좌표를 기준으로 그리드 그룹화
            val latGrid = (it.locationPoint.latitude / gridSize).roundToInt()
            val lngGrid = (it.locationPoint.longitude / gridSize).roundToInt()
            Pair(latGrid, lngGrid)
        }.map { (_, group) ->
            // 좌표 평균 계산
            val avgLat = group.map { it.locationPoint.latitude }.average()
            val avgLng = group.map { it.locationPoint.longitude }.average()

            // 분위기 점수 평균 계산 (Long 타입이므로 평균 후 다시 Long으로 변환)
            val avgBright = group.map { it.score.brightScore }.average()
            val avgCrowded = group.map { it.score.crowdedScore }.average()
            val avgHard = group.map { it.score.hardScore }.average()

            // 평균값이 적용된 새로운 Node 반환
            Node(
                locationPoint = GeoPoint(avgLat, avgLng),
                score = Scores(avgBright, avgCrowded, avgHard)
            )
        }
    }

    private fun createCourseFromPoints(id: String, nodes: List<Node>): Course {
        if (nodes.isEmpty()) return Course(id = id, locationPoints = emptyList()) // 예외 처리

        // 1. [순서 정렬] 가장 가까운 점을 찾아가며 경로 순서 재구성 (Greedy 정렬)
        val sortedPoints = mutableListOf<Node>()
        val remaining = nodes.toMutableList()
        var current = remaining.removeAt(0)
        sortedPoints.add(current)

        var totalDistance = 0.0
        val radius = 6371000.0 // 지구 반지름

        // 모든 노드의 각 점수들을 합산한 뒤 평균을 내어 코스의 대표 점수로 설정합니다.
        val avgBright = nodes.map { it.score.brightScore }.average()
        val avgCrowded = nodes.map { it.score.crowdedScore }.average()
        val avgHard = nodes.map { it.score.hardScore }.average()
        val courseScores = Scores(avgBright, avgCrowded, avgHard)

        while (remaining.isNotEmpty()) {
            // 현재 점(current)에서 가장 가까운 다음 점 찾기
            val next = remaining.minByOrNull { p ->
                val dLat = Math.toRadians(p.locationPoint.latitude - current.locationPoint.latitude)
                val dLon = Math.toRadians(p.locationPoint.longitude - current.locationPoint.longitude)
                // 성능을 위해 여기서는 단순 피타고라스 근사치로 비교 (정렬용)
                Math.pow(dLat, 2.0) + Math.pow(dLon, 2.0)
            }!!

            // [거리 계산] 찾은 '다음 점'과의 하버사인 거리 계산 (실제 거리용)
            val lat1 = Math.toRadians(current.locationPoint.latitude)
            val lat2 = Math.toRadians(next.locationPoint.latitude)
            val dLat = Math.toRadians(next.locationPoint.latitude - current.locationPoint.latitude)
            val dLon = Math.toRadians(next.locationPoint.longitude - current.locationPoint.longitude) // 여기서 한 번에 계산

            val a = Math.sin(dLat / 2).let { it * it } +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(dLon / 2).let { it * it }

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            totalDistance += radius * c

            // 다음 스텝 준비
            remaining.remove(next)
            sortedPoints.add(next)
            current = next
        }

        // 3. 최종 객체 반환
        return Course(
            id = id,
            locationPoints = sortedPoints, // 정렬된 좌표 저장
            minLat = sortedPoints.minOf { it.locationPoint.latitude },
            maxLat = sortedPoints.maxOf { it.locationPoint.latitude},
            minLng = sortedPoints.minOf { it.locationPoint.longitude },
            maxLng = sortedPoints.maxOf { it.locationPoint.longitude },
            distance = Math.round(totalDistance).toInt(),
            scores = courseScores // 코스 전체 평균 점수 저장
        )
    }

    /**
     * 인자에 따라 거리순 또는 특징 점수순으로 코스를 탐색하여 반환합니다.
     * @param courseDistance 목표 거리
     * @param currentLocation 현재 위치
     * @param featureIndex 0: 가까운 순서, 1: 밝기순, 2: 혼잡도순, 3: 난이도순
     */
    // #2. [조건에 맞게 코스 가져오는 함수]
    override suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        featureIndex: Int
    ): AuthResult<List<List<Pair<Int, List<GeoPoint>>>>> {
        return try {
            // 방법 선택 (0이면 거리순, 1~3이면 특징순)
            val sourceCourses = if (featureIndex == 0) {
                findNearestStartPoints(currentLocation)
            } else {
                // featureIndex가 1, 2, 3인 경우를 그대로 넘김
                findFeatureStartPoints(currentLocation, featureIndex)
            }

            if (sourceCourses.isEmpty()) return AuthResult.Fail("주변에 이용 가능한 코스가 없습니다.")

            // 각 코스의 여러 갈래(A[1,2,3], B[1,2]...)를 받아옴
            val recommendedResult = generatePathsFromSources(sourceCourses, courseDistance.toDouble())

            if (recommendedResult.isEmpty()) {
                AuthResult.Fail("조건에 맞는 코스를 생성할 수 없습니다.")
            } else {
                AuthResult.Success(recommendedResult)
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 탐색 중 오류 발생: ${e.message}")
        }
    }

    // 사용자 위치와 가장 가까운 시작점 찾기
    private suspend fun findNearestStartPoints(userLoc: GeoPoint): List<Pair<Course, GeoPoint>> {
        val maxRadius = 0.005 // 최대 500m
        val docs = firestore.collection("Course")
            .whereGreaterThanOrEqualTo("maxLat", userLoc.latitude - maxRadius)
            .whereLessThanOrEqualTo("minLat", userLoc.latitude + maxRadius)
            .get().await()

        val allCandidates = docs.toObjects(Course::class.java).filter {
            it.maxLng >= userLoc.longitude - maxRadius && it.minLng <= userLoc.longitude + maxRadius
        }

        //코스, 시작좌표, 내위치에서 시작점까지의 거리를 담는 리스트
        val startPoints = mutableListOf<Triple<Course, GeoPoint, Double>>()

        allCandidates.forEach { course ->
            course.locationPoints.forEach { node ->
                val dist = calculateDistance(userLoc, node.locationPoint)
                if (dist <= 500.0) { // 500m 이내인 모든 좌표 후보군
                    startPoints.add(Triple(course, node.locationPoint, dist))
                }
            }
        }

        // 거리순 정렬 후, 동일 코스 내 중복 시작점 제거(가장 가까운 것만) 및 최대 5개 추출
        return startPoints.sortedBy { it.third }
            .distinctBy { it.first.id } // 한 코스당 가장 가까운 지점 1개만 선택
            .take(5)
            .map { it.first to it.second }
    }

    /**
     * 특징(점수) 순으로 500m 이내의 코스와 시작점을 찾아 반환합니다.
     * @param userLoc 사용자 위치
     * @param featureIndex 1: 밝기(Bright), 2: 혼잡도(Crowded), 3: 난이도(Hard) 순 정렬
     */
    private suspend fun findFeatureStartPoints(
        userLoc: GeoPoint,
        featureIndex: Int
    ): List<Pair<Course, GeoPoint>> {
        val maxRadius = 0.005 // 최대 500m 범위
        val docs = firestore.collection("Course")
            .whereGreaterThanOrEqualTo("maxLat", userLoc.latitude - maxRadius)
            .whereLessThanOrEqualTo("minLat", userLoc.latitude + maxRadius)
            .get().await()

        val allCandidates = docs.toObjects(Course::class.java).filter {
            it.maxLng >= userLoc.longitude - maxRadius && it.minLng <= userLoc.longitude + maxRadius
        }

        // 코스, 시작점, 특징점수를 한꺼번에 담을 리스트
        val featurePoints = mutableListOf<Triple<Course, GeoPoint, Double>>()

        allCandidates.forEach { course ->
            course.locationPoints.forEach { node ->
                val dist = calculateDistance(userLoc, node.locationPoint)
                if (dist <= 500.0) { // 500m 이내인 코스만 대상으로 함
                    // 인덱스에 따라 정렬 기준 점수(score)를 Triple의 세 번째 인자로 설정
                    val targetScore = when (featureIndex) {
                        1 -> course.scores.brightScore
                        2 -> course.scores.crowdedScore
                        3 -> course.scores.hardScore
                        else -> 0.0
                    }
                    featurePoints.add(Triple(course, node.locationPoint, targetScore))
                }
            }
        }

        // 점수가 높은 순(descending)으로 정렬 후 중복 제거 및 최대 5개 추출
        return featurePoints.sortedByDescending { it.third } // 점수가 높은 순
            .distinctBy { it.first.id } // 동일 코스 내 중복 시작점 제거
            .take(5)
            .map { it.first to it.second }
    }


    // 공통 모듈: 여러 소스(Course)로부터 목표 거리에 맞는 경로들을 "그룹별로" 생성
    private suspend fun generatePathsFromSources(
        sources: List<Pair<Course, GeoPoint>>,
        targetDist: Double
    ): List<List<Pair<Int, List<GeoPoint>>>> { //[A_course[1,2,3],B_course[1,2],C_course[1,2,3]]
        val allGroupsResult = mutableListOf<List<Pair<Int, List<GeoPoint>>>>()

        sources.forEachIndexed { groupIndex, (course, startPoint) ->
            val courseResults = mutableListOf<Pair<Double, List<GeoPoint>>>()
            val visited = mutableSetOf<Pair<Double, Double>>()
            visited.add(startPoint.latitude to startPoint.longitude)

            // 각 코스(소스)마다 DFS 탐색 수행 (최대 3개)
            searchRecursive(
                currentPath = mutableListOf(startPoint),
                currentDist = 0.0,
                targetDist = targetDist,
                pointsPool = course.locationPoints,
                visited = visited,
                results = courseResults,
                maxPerSource = 3
            )

            // 탐색된 결과가 있다면 해당 코스의 그룹(SubGroup)으로 묶음
            if (courseResults.isNotEmpty()) {
                val subGroup = courseResults.mapIndexed { pathIndex, (dist, path) ->
                    val distanceInt = dist.toInt()

                    // 저장 로직을 여기서 처리하여 그룹/경로 인덱스를 정확히 기록
                    saveToTestCollection(
                        distance = distanceInt,
                        path = path,
                        originId = course.id,
                        groupOrder = groupIndex, // A=0, B=1, C=2...
                        subIndex = pathIndex + 1  // 1, 2, 3
                    )

                    distanceInt to path
                }
                allGroupsResult.add(subGroup)
            }
        }

        return allGroupsResult
    }

    // --- 내부 알고리즘 함수 ---

    private fun searchRecursive(
        currentPath: MutableList<GeoPoint>, // 현재 위치
        currentDist: Double, // 현재까지의 누적 거리
        targetDist: Double, // 사용자가 원하는 거리
        pointsPool: List<Node>, // 해당 코스에 포함되는 모든 좌표
        visited: MutableSet<Pair<Double, Double>>, // 방문한 좌표
        results: MutableList<Pair<Double, List<GeoPoint>>>, // Double(거리) 와 좌표 목록 을 리스트로 담음
        maxPerSource: Int // 추가된 인자
    ) {
        // 해당 코스에서 이미 충분한 갈래(예: 3개)를 찾았다면 중단
        if (results.size >= maxPerSource) return

        if (currentDist >= targetDist) {
            // 도달 시점의 누적 거리(currentDist)를 경로와 함께 저장
            results.add(currentDist to currentPath.toList())
            return
        }

        val lastPt = currentPath.last() // 현재 위치
        val parentPt = if (currentPath.size >= 2) currentPath[currentPath.size - 2] else null // 부모 위치

        // 주변 이웃 찾기
        val allNeighbors = pointsPool.filter { pt ->
            val key = pt.locationPoint.latitude to pt.locationPoint.longitude
            if (key in visited) return@filter false
            val d = calculateDistance(lastPt, pt.locationPoint)
            d in 1.0..7.9 // 1미터에서 7.9미터 사이의 이웃 좌표들 탐색
        }.map { it to calculateDistance(lastPt, it.locationPoint) }

        if (allNeighbors.isEmpty()) {
            if (currentDist > targetDist * 0.95) { // 더 이상 주변 좌표가 없을 경우, 현재까지의 거리가 목표거리의 95퍼센트가 넘으면 코스로 인정
                results.add(currentDist to currentPath.toList())
            }
            return
        }

        // 필터링 및 정렬
        val scoredNeighbors = mutableListOf<Triple<GeoPoint, Double, Int>>()
        if (allNeighbors.size == 1) { // 이웃이 한개밖에 없으면 우선순위 1
            scoredNeighbors.add(Triple(allNeighbors[0].first.locationPoint, allNeighbors[0].second, 0)) // 좌표, 거리, 우선순위
        } else { // 이웃 좌표가 2개 이상 있는 경우
            for ((pt, d) in allNeighbors) {
                val angleToNext = if (parentPt != null) calculateAngleDiff(parentPt, lastPt, pt.locationPoint) else 0.0
                var isValid = false
                var priority = 1

                if (angleToNext < 20.0) { //부모 좌표와 나의 좌표, 나의 좌표와 다음 이웃 좌표의 각도차이가 20도 미만(거의 직선) 이라면
                    isValid = true
                    priority = 0 // 우선순위 0
                } else { // 20도를 벗어나는 경우에는 그 다음 좌표까지 확인
                    val hasContinuingPath = pointsPool.any { nNext ->
                        val nNextKey = nNext.locationPoint.latitude to nNext.locationPoint.longitude
                        if (nNextKey in visited || nNextKey == (lastPt.latitude to lastPt.longitude)) false
                        else {
                            val dNext = calculateDistance(pt.locationPoint, nNext.locationPoint)
                            dNext in 1.0..7.9 && calculateAngleDiff(lastPt, pt.locationPoint, nNext.locationPoint) < 20.0
                        }
                    }
                    if (hasContinuingPath) {
                        isValid = true
                        priority = 1 // 직선 이웃좌표보다는 우선순위 낮게 설정
                    }
                }
                if (isValid) scoredNeighbors.add(Triple(pt.locationPoint, d, priority))
            }
        }
        // 우선순위 먼저 확인 후, 거리 짧은 순 정렬
        val sortedNeighbors = scoredNeighbors.sortedWith(compareBy({ it.third }, { it.second }))

        // 탐색 진행
        for ((nextPt, d, _) in sortedNeighbors) {
            if (results.size >= maxPerSource) break // 코스를 5개 이상 찾으면 종료
            visited.add(nextPt.latitude to nextPt.longitude)
            currentPath.add(nextPt)
            // 누적 거리(currentDist + d)를 넘겨줌
            searchRecursive(currentPath, currentDist + d, targetDist, pointsPool, visited, results, maxPerSource)
            currentPath.removeAt(currentPath.size - 1)
        }
    }

    // --- 유틸리티 함수 ---

    // [수정] 파이어베이스 저장 함수
    private suspend fun saveToTestCollection(
        distance: Int,
        path: List<GeoPoint>,
        originId: String,
        groupOrder: Int,
        subIndex: Int
    ) {
        val testData = hashMapOf(
            "distance" to distance,
            "locationPoints" to path,
            "originCourseId" to originId,
            "groupOrder" to groupOrder, // 정렬용 필드
            "subPathName" to "Group${groupOrder}_${originId}_Path${subIndex}", // 이름에 그룹 순서 명시
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // 문서 ID를 직접 지정해서 저장하면 콘솔에서 정렬된 상태로 보기 편합니다.
        val docName = "test_G${groupOrder}_P${subIndex}_${System.currentTimeMillis()}"
        firestore.collection("test").document(docName).set(testData).await()
    }
    // 두 좌표의 거리 계산 (지구 반지름 기반)
    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
        return 2 * atan2(sqrt(a), sqrt(1 - a)) * r
    }
    // 3좌표의 각도 차이 계산
    private fun calculateAngleDiff(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint): Double {
        val v1 = Pair(p1.latitude - p2.latitude, p1.longitude - p2.longitude)
        val v2 = Pair(p3.latitude - p2.latitude, p3.longitude - p2.longitude)
        val dot = v1.first * v2.first + v1.second * v2.second
        val mag1 = sqrt(v1.first.pow(2) + v1.second.pow(2))
        val mag2 = sqrt(v2.first.pow(2) + v2.second.pow(2))
        if (mag1 == 0.0 || mag2 == 0.0) return 180.0
        return 180.0 - Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)))
    }


    override suspend fun startRealtimeSync() {
        Log.d("RUNUP_SYNC", "🚀 Firestore 실시간 동기화 시작...")

        // 로컬에는 있는데 Firestore 에는 없는 유령 데이터 삭제
        firestore.collection("Course").get().addOnSuccessListener { snapshot ->
            val remoteIds = snapshot.documents.map { it.id }.toSet()

            // 로컬 ObjectBox에 저장된 모든 데이터 가져오기
            val localEntities = courseBox.all

            // 로컬에는 있는데 Firestore(remoteIds)에는 없는 데이터 찾아내기
            val toDelete = localEntities.filter { it.firebaseId !in remoteIds }

            if (toDelete.isNotEmpty()) {
                courseBox.remove(toDelete) // 로컬 DB에서 한 번에 삭제
                Log.d("RUNUP_SYNC", "🗑️ 유령 데이터 ${toDelete.size}건 삭제 완료: ${toDelete.map { it.firebaseId }}")
            } else {
                Log.d("RUNUP_SYNC", "✅ 로컬 데이터가 최신 상태입니다. 삭제할 유령 데이터 없음.")
            }
        }.addOnFailureListener {
            Log.e("RUNUP_SYNC", "❌ 전체 동기화 체크 실패: ${it.message}")
        }

        firestore.collection("Course")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("RUNUP_SYNC", "❌ 동기화 중 에러 발생: ${error.message}")
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { dc ->
                    val doc = dc.document

                    try {
                        // Firestore 데이터를 기존 Course 모델로 변환
                        val courseModel = doc.toObject(Course::class.java)

                        // 벡터 데이터 추출 (Double List -> FloatArray)
                        val vectorList = doc.get("vector") as? List<Double>
                        val floatVector = vectorList?.map { it.toFloat() }?.toFloatArray()

                        // ObjectBox 엔티티 생성
                        val entity = CourseEntity(
                            firebaseId = doc.id,
                            courseDataJson = gson.toJson(courseModel),
                            embeddingText = doc.get("embedding_text") as? String,
                            vector = floatVector
                        )

                        when (dc.type) {
                            // 데이터 추가 또는 수정
                            DocumentChange.Type.ADDED -> {
                                courseBox.put(entity)
                                Log.d("RUNUP_SYNC", "✅ 새 코스 추가됨: ${courseModel.id}")
                            }
                            DocumentChange.Type.MODIFIED -> {
                                courseBox.put(entity)
                                Log.d("RUNUP_SYNC", "🔄 코스 정보 수정됨: ${courseModel.id}")
                            }
                            // 데이터 삭제
                            DocumentChange.Type.REMOVED -> {
                                // firebaseId로 로컬 데이터를 찾아서 삭제
                                val target = courseBox.query(CourseEntity_.firebaseId.equal(doc.id)).build().findFirst()
                                target?.let { courseBox.remove(it.id) }
                                Log.d("RUNUP_SYNC", "🗑️ 코스 삭제됨: ${doc.id}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RUNUP_SYNC", "⚠️ 데이터 변환 중 오류: ${e.message}")
                    }
                }

                Log.d("RUNUP_SYNC", "현재 로컬 DB 코스 총 개수: ${courseBox.count()}")
            }
    }


    //벡터 유사도 검색
    override suspend fun getSearchResult(queryText: String): List<Course> {
        // 문장을 384차원 벡터로 변환
        val userQueryVector = embeddingHelper.getEmbedding(queryText)

        // 점수와 함께 검색
        val resultsWithScores = courseBox.query {
            nearestNeighbors(CourseEntity_.vector, userQueryVector, 5) // 넉넉히 5개까지 확인
        }.findWithScores()

        Log.d("RUNUP_SEARCH_DEBUG", "--------------------------------------")
        Log.d("RUNUP_SEARCH_DEBUG", "📊 DB 내 총 코스 개수: ${courseBox.count()}")
        Log.d("RUNUP_SEARCH_DEBUG", "🔎 검색 결과 개수: ${resultsWithScores.size}")
        Log.d("RUNUP_SEARCH_DEBUG", "🎯 검색어: '$queryText'")
        Log.d("RUNUP_SEARCH_DEBUG", "--------------------------------------")

        // 점수 확인 및 코스 번호만 로그 출력
        resultsWithScores.forEachIndexed { index, scoreObject ->
            val entity = scoreObject.get() // 실제 데이터(CourseEntity)
            val score = scoreObject.score  // ⭐ 유사도 점수 (0에 가까울수록 일치)

            Log.d("RUNUP_SEARCH_DEBUG", "   [$index] 순위 | 코스ID: ${entity.firebaseId} | 유사도 점수: $score")
            Log.d("RUNUP_SEARCH_DEBUG", "   텍스트: ${entity.embeddingText?.take(128)}")
        }
        Log.d("RUNUP_SEARCH_DEBUG", "--------------------------------------")

        // UI에는 검색 결과 순서대로 Course 객체 리스트 반환
        return resultsWithScores.map { scoreObject ->
            val entity = scoreObject.get()
            gson.fromJson(entity.courseDataJson, Course::class.java)
        }
    }
}