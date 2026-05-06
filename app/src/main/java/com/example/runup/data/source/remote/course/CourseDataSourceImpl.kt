package com.example.runup.data.source.remote.course

import android.util.Log
import com.example.runup.data.source.local.objectbox.entity.CourseEntity
import com.example.runup.data.source.local.objectbox.entity.CourseEntity_
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.Course
import com.example.runup.domain.model.CoursePathGroup
import com.example.runup.domain.model.Node
import com.example.runup.domain.model.Path
import com.example.runup.domain.model.Scores
import com.example.runup.domain.model.SortDirection
import com.example.runup.domain.model.SortType
import com.example.runup.service.GeminiHelper
import com.example.runup.ui.util.calculateDistance
import com.example.runup.ui.util.mapper.CourseMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import io.objectbox.Box
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CourseDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val courseBox: Box<CourseEntity>,
    private val geminiHelper: GeminiHelper,
    private val courseMapper: CourseMapper
) : CourseDataSource {
    // [코스 병합 및 저장하는 함수]
    override suspend fun saveCourse(course: Course): AuthResult<Boolean> {
        return try {
            mergeAndSaveCourse(course)
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail("러닝 기록 저장 실패", e)
        }
    }

    private suspend fun mergeAndSaveCourse(newCourse: Course) {
        val eps = 0.00008 // 8m
        val minSamples = 2

        // 빈 코스 방어 코드 (앱 크래시 방지)
        if (newCourse.locationPoints.isEmpty()) {
            Log.e("CourseSave", "좌표가 없는 코스는 저장할 수 없습니다.")
            return
        }

        // 1. 새 코스의 Bounding Box 및 넓이 계산
        val newMinLat = newCourse.locationPoints.minOf { it.locationPoint.latitude }
        val newMaxLat = newCourse.locationPoints.maxOf { it.locationPoint.latitude }
        val newMinLng = newCourse.locationPoints.minOf { it.locationPoint.longitude }
        val newMaxLng = newCourse.locationPoints.maxOf { it.locationPoint.longitude }

        val newArea = maxOf((newMaxLat - newMinLat) * (newMaxLng - newMinLng), 0.00000001)

        // 🌟 [원상 복구] 복합 색인이 설정되어 있으므로 DB 단에서 완벽하게 위도를 걸러냅니다! (최고의 속도)
        val candidates = firestore.collection("Course")
            .whereGreaterThanOrEqualTo("maxLat", newMinLat)
            .whereLessThanOrEqualTo("minLat", newMaxLat)
            .get().await().toObjects(Course::class.java)

        // 3. [핵심 변경] 경도 및 Bounding Box "겹치는 비율(Overlap Ratio)" 검사
        val targetClusters = candidates.filter { existing ->

            // ── [1단계 필터] 경도가 아예 안 겹치는 코스 컷오프 ──
            if (existing.maxLng < newMinLng || existing.minLng > newMaxLng) {
                return@filter false
            }

            // ── [2단계 필터: Broad-Phase] Bounding Box 넓이 겹침 검사 ──
            val overlapMinLat = maxOf(newMinLat, existing.minLat)
            val overlapMaxLat = minOf(newMaxLat, existing.maxLat)
            val overlapMinLng = maxOf(newMinLng, existing.minLng)
            val overlapMaxLng = minOf(newMaxLng, existing.maxLng)

            val overlapArea = if (overlapMinLat < overlapMaxLat && overlapMinLng < overlapMaxLng) {
                (overlapMaxLat - overlapMinLat) * (overlapMaxLng - overlapMinLng)
            } else {
                0.0
            }

            val existingArea = maxOf((existing.maxLat - existing.minLat) * (existing.maxLng - existing.minLng), 0.00000001)
            val smallerArea = minOf(newArea, existingArea)
            val bboxOverlapRatio = overlapArea / smallerArea

            if (bboxOverlapRatio < 0.6) {
                return@filter false
            }

            // ── [3단계 필터: Narrow-Phase] 대각선 함정 방어용 "실제 좌표 정밀 매칭" ──
            val thresholdMeters = 15.0

            val (smallerCourse, largerCourse) = if (newCourse.locationPoints.size <= existing.locationPoints.size) {
                newCourse.locationPoints to existing.locationPoints
            } else {
                existing.locationPoints to newCourse.locationPoints
            }

            // 💨 [유지] 성능 누수 방지: 반복문 '밖'에서 샘플링을 미리 완료
            val sampledSmallerCourse = smallerCourse.filterIndexed { index, _ -> index % 3 == 0 }
            val sampledLargerCourse = largerCourse.filterIndexed { index, _ -> index % 2 == 0 }

            var matchCount = 0
            for (smallPt in sampledSmallerCourse) {
                // 미리 만들어둔 sampledLargerCourse에서 바로 탐색 (연산 속도 극대화)
                val isMatched = sampledLargerCourse.any { largePt ->
                    calculateDistance(smallPt.locationPoint, largePt.locationPoint) <= thresholdMeters
                }
                if (isMatched) matchCount++
            }

            val overlapRatio = matchCount.toDouble() / sampledSmallerCourse.size

            overlapRatio >= 0.70
        }

        // 4. 병합 대상들만 모아서 DBSCAN 실행
        val allNodes = mutableListOf<Node>().apply {
            addAll(newCourse.locationPoints)
            targetClusters.forEach { addAll(it.locationPoints) }
        }

        val clusters = performDBSCAN(allNodes, eps, minSamples)

        // 5. Firestore 업데이트
        firestore.runTransaction { transaction ->
            val metaRef = firestore.collection("Metadata").document("courseInfo")
            val metaSnap = transaction.get(metaRef)
            var lastNum = if (metaSnap.exists()) metaSnap.getLong("lastCourseNumber") ?: 0L else 0L

            targetClusters.forEach { oldCourse ->
                transaction.delete(firestore.collection("Course").document(oldCourse.id))
            }

            clusters.forEach { clusterPoints ->
                if (clusterPoints.isNotEmpty()) {
                    lastNum++
                    val customId = "course$lastNum"

                    val finalSimplifiedPoints = gridSimplify(clusterPoints, 0.00005)
                    val newDocRef = firestore.collection("Course").document(customId)

                    val finalCourse = createCourseFromPoints(customId, finalSimplifiedPoints)
                    transaction.set(newDocRef, finalCourse)
                }
            }
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
        if (nodes.isEmpty()) return Course(id = id, locationPoints = emptyList())

        // 1. [순서 정렬] 가장 가까운 점을 찾아가며 경로 순서 재구성
        val sortedPoints = mutableListOf<Node>()
        val remaining = nodes.toMutableList()
        var current = remaining.removeAt(0)
        sortedPoints.add(current)

        var totalDistance = 0.0

        // 2. 🌟 원래 방식대로 단순 평균 적용
        val avgBright = nodes.map { it.score.brightScore }.average()
        val avgCrowded = nodes.map { it.score.crowdedScore }.average()
        val avgHard = nodes.map { it.score.hardScore }.average()

        val courseScores = Scores(avgBright, avgCrowded, avgHard)

        // 3. 거리 계산 및 좌표 잇기
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { p ->
                val dLat = Math.toRadians(p.locationPoint.latitude - current.locationPoint.latitude)
                val dLon = Math.toRadians(p.locationPoint.longitude - current.locationPoint.longitude)
                Math.pow(dLat, 2.0) + Math.pow(dLon, 2.0)
            }!!

            totalDistance += calculateDistance(current.locationPoint, next.locationPoint)
            remaining.remove(next)
            sortedPoints.add(next)
            current = next
        }

        return Course(
            id = id,
            locationPoints = sortedPoints,
            minLat = sortedPoints.minOf { it.locationPoint.latitude },
            maxLat = sortedPoints.maxOf { it.locationPoint.latitude},
            minLng = sortedPoints.minOf { it.locationPoint.longitude },
            maxLng = sortedPoints.maxOf { it.locationPoint.longitude },
            distance = Math.round(totalDistance).toInt(),
            scores = courseScores
        )
    }

    // #2. [조건에 맞게 코스 가져오는 함수]
    override suspend fun getCourse(
        courseDistance: Int,
        currentLocation: GeoPoint,
        isLoop: Boolean,
        sortType: SortType,
        maxSearchDistance: Int, // (단위: km)
        sortDirection: SortDirection // 📍 추가 (ASC, DESC)
    ): AuthResult<List<CoursePathGroup>> {
        return try {
            val targetDist = if(isLoop) courseDistance/2.0 else courseDistance.toDouble()

            val sourceCourses = if (sortType == SortType.DISTANCE) { // 방법 선택
                findNearestStartPoints(currentLocation, maxSearchDistance, targetDist)
            } else {
                // featureIndex가 1, 2, 3인 경우를 그대로 넘김
                findFeatureStartPoints(currentLocation, sortType, maxSearchDistance, sortDirection, targetDist)
            }

            if (sourceCourses.isEmpty()) return AuthResult.Fail("주변에 이용 가능한 코스가 없습니다.")

            // 2. Pair를 Triple로 변환 (추천 사유 추가)
            val sourcesWithReason = sourceCourses.map { (course, startPoint) ->
                // 특징 인덱스에 따라 적절한 기본 문구 설정
                val defaultReason = when (sortType) {
                    SortType.DISTANCE -> "현재 위치에서 가장 가까운 추천 코스입니다."
                    SortType.BRIGHT -> "야간에도 밝고 안전한 코스입니다."
                    SortType.PEOPLE -> "사람들이 많이 찾는 활기찬 코스입니다."
                    SortType.DIFFICULTY -> "운동 효과가 좋은 난이도 있는 코스입니다."
                    else -> "사용자 취향에 맞는 추천 코스입니다."
                }
                Triple(course, startPoint, defaultReason)
            }

            // 각 코스의 여러 갈래(A[1,2,3], B[1,2]...)를 받아옴
            val recommendedResult = generatePathsFromSources(currentLocation, sourcesWithReason, targetDist)

            if (recommendedResult.isEmpty()) {
                AuthResult.Fail("근처에 조건에 맞는 코스가 없습니다...")
            } else {
                AuthResult.Success(recommendedResult)
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 탐색 중 오류 발생: ${e.message}")
        }
    }

    override suspend fun getCourseFromAI(
        courseDistance: Int, // 몇 m 뛸껀지
        currentLocation: GeoPoint, //
        currentAddress : String,
        isLoop: Boolean,
        userPrompt: String,
        maxSearchDistance: Int
    ): AuthResult<List<CoursePathGroup>>{
        return try {
            val targetDist = if(isLoop) courseDistance/2.0 else courseDistance.toDouble()

            val aiRecommendations = geminiHelper.performAiSearch(userPrompt, currentAddress, currentLocation, maxSearchDistance, targetDist)

            val sources = aiRecommendations.mapNotNull { (course, reason) ->
                val startPoint = findStartPointForAI(currentLocation, course)
                if (startPoint != null) {
                    // (코스, 시작점, 추천이유)를 묶어서 전달
                    Triple(course, startPoint.second, reason)
                } else null
            }

            if (sources.isEmpty()) {
                return AuthResult.Fail("근처에 조건에 맞는 코스가 없습니다...")
            }

            // 각 코스의 여러 갈래(A[1,2,3], B[1,2]...)를 받아옴
            val recommendedResult = generatePathsFromSources(currentLocation, sources, targetDist)

            if (recommendedResult.isEmpty()) {
                AuthResult.Fail("근처에 조건에 맞는 코스가 없습니다...")
            } else {
                AuthResult.Success(recommendedResult)
            }
        } catch (e: Exception) {
            AuthResult.Fail("코스 탐색 중 오류 발생: ${e.message}")
        }
    }

    // 사용자 위치와 가장 가까운 시작점 찾기
    private suspend fun findNearestStartPoints(
        userLoc: GeoPoint,
        maxSearchDistance: Int,
        targetDist: Double): List<Pair<Course, GeoPoint>> {
        val maxRadius = maxSearchDistance * 0.00001 // 약 500m 내외의 위경도 오차 범위

        // ObjectBox 쿼리: 지리적 영역(Bounding Box) 필터링
        val query = courseBox.query()
            .greater(CourseEntity_.maxLat, userLoc.latitude - maxRadius)
            .less(CourseEntity_.minLat, userLoc.latitude + maxRadius)
            .greater(CourseEntity_.maxLng, userLoc.longitude - maxRadius)
            .less(CourseEntity_.minLng, userLoc.longitude + maxRadius)
            .build()

        val candidates = query.find()
        query.close()
        //코스, 시작좌표, 내위치에서 시작점까지의 거리를 담는 리스트
        val startPoints = mutableListOf<Triple<Course, GeoPoint, Double>>()

        candidates.forEach { entity ->
            val course = courseMapper.toDomain(entity) // 매퍼 사용하여 복원

            if (course.distance < targetDist) { // 코스 전체 길이가 목표 거리보다 짧으면 제외
                return@forEach
            }

            course.locationPoints.forEach { node ->
                val dist = calculateDistance(userLoc, node.locationPoint)
                if (dist <= maxSearchDistance.toDouble()) { // 500m 이내인 모든 좌표 후보군
                    startPoints.add(Triple(course, node.locationPoint, dist))
                }
            }
        }

        // 거리순 정렬 후, 동일 코스 내 중복 시작점 제거(가장 가까운 것만) 및 최대 5개 추출
        return startPoints.sortedBy { it.third }
            .distinctBy { it.first.id }
            .take(3)
            .map { it.first to it.second }
    }

    /**
     * 특징(점수) 순으로 500m 이내의 코스와 시작점을 찾아 반환합니다.
     * @param userLoc 사용자 위치
     * @param featureIndex 1: 밝기(Bright), 2: 혼잡도(Crowded), 3: 난이도(Hard) 순 정렬
     */
    private suspend fun findFeatureStartPoints(
        userLoc: GeoPoint,
        sortType: SortType,
        maxSearchDistance: Int,
        sortDirection: SortDirection,
        targetDist: Double
    ): List<Pair<Course, GeoPoint>> {
        val maxRadius = maxSearchDistance * 0.00001 // 약 500m 내외의 위경도 오차 범위

        // 1. ObjectBox 쿼리: 사용자 주변 영역(Bounding Box)에 걸쳐 있는 코스들 1차 필터링
        val query = courseBox.query()
            .greater(CourseEntity_.maxLat, userLoc.latitude - maxRadius)
            .less(CourseEntity_.minLat, userLoc.latitude + maxRadius)
            .greater(CourseEntity_.maxLng, userLoc.longitude - maxRadius)
            .less(CourseEntity_.minLng, userLoc.longitude + maxRadius)
            .build()

        val candidates = query.find()
        query.close()

        // 코스, 시작점, 특징점수를 한꺼번에 담을 리스트
        val featurePoints = mutableListOf<Triple<Course, GeoPoint, Double>>()

        candidates.forEach { entity ->
            val course = courseMapper.toDomain(entity)
            if (course.distance < targetDist) return@forEach

            // 1. 해당 코스의 점들 중 사용자와 가장 가까운 지점(Best Entry Point)을 찾습니다.
            val nearestNode = course.locationPoints
                .map { node -> node to calculateDistance(userLoc, node.locationPoint) }
                .filter { it.second <= maxSearchDistance.toDouble() }
                .minByOrNull { it.second } // 내 위치와 가장 가까운 지점 선택

            if (nearestNode != null) {
                val targetScore = when (sortType) {
                    SortType.BRIGHT -> course.scores.brightScore
                    SortType.PEOPLE -> course.scores.crowdedScore
                    SortType.DIFFICULTY -> course.scores.hardScore
                    else -> 0.0
                }
                // 코스당 딱 하나의 '최적 진입점'만 담깁니다.
                featurePoints.add(Triple(course, nearestNode.first.locationPoint, targetScore))
            }
        }

        val sortedList = if (sortDirection == SortDirection.DESCENDING) {
            featurePoints.sortedBy { it.third }
        } else {
            featurePoints.sortedByDescending { it.third }
        }

        return sortedList.distinctBy { it.first.id }.take(3).map { it.first to it.second }
    }

    private fun findStartPointForAI(
        userLoc: GeoPoint,
        course: Course
    ): Pair<Course, GeoPoint>? {
        // 1. 해당 코스의 모든 points 중 사용자 위치와 가장 가까운 점(GeoPoint)을 찾음
        val nearestPoint = course.locationPoints
            .map { it.locationPoint }
            .minByOrNull { calculateDistance(userLoc, it) }

        // 2. 가장 가까운 점이 있다면 Course와 묶어서 반환, 없으면 null 반환
        return nearestPoint?.let {
            course to it
        }
    }




    // 공통 모듈: 여러 소스(Course)로부터 목표 거리에 맞는 경로들을 "그룹별로" 생성
    private suspend fun generatePathsFromSources(
        currentLocation: GeoPoint,
        sources: List<Triple<Course, GeoPoint, String>>,
        targetDist: Double
    ): List<CoursePathGroup> {
        val allGroupsResult = mutableListOf<CoursePathGroup>()

        sources.forEachIndexed { index, (course, startPoint, reason) ->

            val courseResults = mutableListOf<Pair<Double, List<GeoPoint>>>()
            val visited = mutableSetOf<Pair<Double, Double>>()
            visited.add(startPoint.latitude to startPoint.longitude)

            // ── 🔹 탐색 전 체크 📍
            if (course.locationPoints.isEmpty()) {
                Log.e("RUNUP_DFS", "   ⚠️ 에러: 코스에 좌표 데이터가 없어 탐색을 건너뜁니다.")
            }

            searchRecursive( // 각 코스(소스)마다 DFS 탐색 수행
                currentPath = mutableListOf(startPoint),
                currentDist = 0.0,
                targetDist = targetDist,
                pointsPool = course.locationPoints,
                visited = visited,
                results = courseResults,
                maxPerSource = 3
            )

            if (courseResults.isNotEmpty()) {
                val subGroup = courseResults.mapIndexed { pathIndex, (dist, path) ->
                    val distanceInt = dist.toInt()
                    val centerPoint = calculateCenterPoint(path, currentLocation)

                    Path(
                        distance = distanceInt,
                        points = path,
                        centerPoint = centerPoint
                    )
                }
                allGroupsResult.add(CoursePathGroup(course, reason, subGroup))
            } else {
                Log.w("RUNUP_DFS", "   ❌ 해당 소스에서는 조건을 만족하는 경로를 찾지 못함 (거리 미달 혹은 끊김)")
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
        if (results.size >= maxPerSource) return // 해당 코스에서 이미 충분한 갈래(예: 3개)를 찾았다면 중단

        if (currentDist >= targetDist) {
            // 도달 시점의 누적 거리(currentDist)를 경로와 함께 저장
            results.add(currentDist to currentPath.toList())
            return
        }

        val lastPt = currentPath.last() // 현재 위치
        val parentPt = if (currentPath.size >= 2) currentPath[currentPath.size - 2] else null // 부모 위치

        val allNeighbors = pointsPool.filter { pt ->
            val key = pt.locationPoint.latitude to pt.locationPoint.longitude
            if (key in visited) return@filter false
            val d = calculateDistance(lastPt, pt.locationPoint)
            d in 1.0..10.0// 1미터에서 10미터 사이의 이웃 좌표들 탐색
        }.map { it to calculateDistance(lastPt, it.locationPoint) }

        if (allNeighbors.isEmpty()) {
            if (currentDist > targetDist * 0.95) { // 더 이상 주변 좌표가 없을 경우, 현재까지의 거리가 목표거리의 95퍼센트가 넘으면 코스로 인정
                results.add(currentDist to currentPath.toList())
            }
            return
        }

        // 1. 후보군을 모으는 임시 리스트 (좌표, 거리, 우선순위, 각도차이)
        data class NeighborCandidate(
            val point: GeoPoint,
            val distance: Double,
            val priority: Int,
            val angle: Double
        )

        val candidates = mutableListOf<NeighborCandidate>()

        if (allNeighbors.size == 1) {
            candidates.add(NeighborCandidate(allNeighbors[0].first.locationPoint, allNeighbors[0].second, 0, 0.0))
        } else {
            for ((pt, d) in allNeighbors) {
                val angleToNext = if (parentPt != null) calculateAngleDiff(parentPt, lastPt, pt.locationPoint) else 0.0
                var isValid = false
                var priority = 1

                // 1-1. 직선 우선순위 판단
                if (angleToNext < 20.0) {
                    isValid = true
                    priority = 0
                } else {
                    // 1-2. 곡선/꺾임 시 후속 경로 존재 여부 판단
                    val hasContinuingPath = pointsPool.any { nNext ->
                        val nNextKey = nNext.locationPoint.latitude to nNext.locationPoint.longitude
                        if (nNextKey in visited || nNextKey == (lastPt.latitude to lastPt.longitude)) false
                        else {
                            val dNext = calculateDistance(pt.locationPoint, nNext.locationPoint)
                            dNext in 1.0..10.0 && calculateAngleDiff(lastPt, pt.locationPoint, nNext.locationPoint) < 30.0
                        }
                    }
                    if (hasContinuingPath) {
                        isValid = true
                        priority = 1
                    }
                }

                if (isValid) {
                    candidates.add(NeighborCandidate(pt.locationPoint, d, priority, angleToNext))
                }
            }
        }

        val filteredNeighbors = mutableListOf<NeighborCandidate>() // 비슷한 각도(±20도) 내에서 가장 가까운 점 하나만 남기기

        // 각도가 작은 순서(직선에 가까운 순)로 정렬해서 비교하거나, 그룹화 처리
        candidates.sortBy { it.distance } // 일단 거리순 정렬

        for (candidate in candidates) {
            // 이미 결과 리스트에 비슷한 각도(±20도)를 가진 더 짧은 거리의 점이 있는지 확인
            val isDuplicateDirection = filteredNeighbors.any { existing ->
                Math.abs(existing.angle - candidate.angle) <= 40.0
            }

            if (!isDuplicateDirection) {
                filteredNeighbors.add(candidate)
            }
        }

        // 3. 최종 정렬 (우선순위 -> 거리 순)
        val sortedNeighbors = filteredNeighbors.sortedWith(compareBy({ it.priority }, { it.distance }))

        // 4. 탐색 진행 (기존과 동일)
        for (neighbor in sortedNeighbors) {
            if (results.size >= maxPerSource) break

            val nextPt = neighbor.point
            val d = neighbor.distance

            visited.add(nextPt.latitude to nextPt.longitude)
            currentPath.add(nextPt)
            searchRecursive(currentPath, currentDist + d, targetDist, pointsPool, visited, results, maxPerSource)
            currentPath.removeAt(currentPath.size - 1)
            visited.remove(nextPt.latitude to nextPt.longitude)
        }
    }

/*
    // [수정] 파이어베이스 저장 함수
    private suspend fun saveToTestCollection(
        distance: Int,
        path: List<GeoPoint>,
        originId: String,
        subIndex: Int
    ) {
        val testData = hashMapOf(
            "distance" to distance,
            "locationPoints" to path,
            "originCourseId" to originId,
            "subPathName" to "${originId}_Path${subIndex}", // 이름에 그룹 순서 명시
        )

        // 문서 ID를 직접 지정해서 저장하면 콘솔에서 정렬된 상태로 보기 편합니다.
        val docName = "test_${originId}_P${subIndex}"
        firestore.collection("test").document(docName).set(testData).await()
    }
*/

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

    // 좌표 목록에서 중심 좌표 반환
    private fun calculateCenterPoint(path: List<GeoPoint>, currentLocation: GeoPoint): GeoPoint {
        if (path.isEmpty()) return currentLocation

        // 1. 경로(path)의 경계값 계산
        val minLat = path.minOf { it.latitude }
        val maxLat = path.maxOf { it.latitude }
        val minLng = path.minOf { it.longitude }
        val maxLng = path.maxOf { it.longitude }

        // 2. 경로의 중심점 계산
        val pathCenterLat = (minLat + maxLat) / 2.0
        val pathCenterLng = (minLng + maxLng) / 2.0

        // 3. 경로 중심점과 사용자 위치(currentLocation)의 중점 계산 및 반환
        return GeoPoint(
            pathCenterLat,
            pathCenterLng
        )
    }
}