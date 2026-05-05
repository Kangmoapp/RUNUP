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
        val eps = 0.0008 // 10m
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

                    val simplifiedPoints = gridSimplify(clusterPoints, 0.00002) // 2미터 정사각형 격자 안에 들어온 점들은 평균내서 하나의 점으로 합침
                    val finalSimplifiedPoints = gridSimplify(simplifiedPoints, 0.00005) // 5미터 정사각형 격자 안에 들어온 점들은 평균내서 하나의 점으로 합침
                    val newDocRef = firestore.collection("Course").document(customId)

                    // finalCourse 객체 생성 시 id도 customId로 전달
                    val finalCourse = createCourseFromPoints(customId, finalSimplifiedPoints)
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

        // 2. 모든 노드의 각 점수들을 합산한 뒤 평균을 내어 코스의 대표 점수로 설정합니다.
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
            totalDistance += calculateDistance(current.locationPoint, next.locationPoint)

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
                return AuthResult.Fail("추천된 코스의 시작점을 찾을 수 없습니다.")
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
            // CourseMapper를 사용하여 DB 엔티티를 도메인 모델(Course)로 복원
            val course = courseMapper.toDomain(entity)

            if (course.distance < targetDist) {
                return@forEach
            }

            val targetScore = when (sortType) { // 해당 코스의 점수 확인 (SortType에 따라 선택)
                SortType.BRIGHT -> course.scores.brightScore
                SortType.PEOPLE -> course.scores.crowdedScore
                SortType.DIFFICULTY -> course.scores.hardScore
                else -> 0.0
            }

            // 코스 내의 모든 포인트를 확인하여 사용자 위치와 500m 이내인 것들 수집
            course.locationPoints.forEach { node ->
                val dist = calculateDistance(userLoc, node.locationPoint)
                if (dist <= maxSearchDistance.toDouble()) {
                    featurePoints.add(Triple(course, node.locationPoint, targetScore))
                }
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

        Log.d("RUNUP_DFS", "─── 🔍 경로 생성 시작 ───")
        Log.d("RUNUP_DFS", "목표 거리: ${targetDist}m | 소스(코스) 개수: ${sources.size}")

        sources.forEachIndexed { index, (course, startPoint, reason) ->
            Log.d("RUNUP_DFS", "[소스 $index] 코스ID: ${course.id} | 시작점: ${startPoint.latitude}, ${startPoint.longitude}")
            Log.d("RUNUP_DFS", "   -> 보유한 좌표(pointsPool) 개수: ${course.locationPoints.size}")

            val courseResults = mutableListOf<Pair<Double, List<GeoPoint>>>()
            val visited = mutableSetOf<Pair<Double, Double>>()
            visited.add(startPoint.latitude to startPoint.longitude)

            // ── 🔹 탐색 전 체크 📍
            if (course.locationPoints.isEmpty()) {
                Log.e("RUNUP_DFS", "   ⚠️ 에러: 코스에 좌표 데이터가 없어 탐색을 건너뜁니다.")
            }

            // 각 코스(소스)마다 DFS 탐색 수행
            searchRecursive(
                currentPath = mutableListOf(startPoint),
                currentDist = 0.0,
                targetDist = targetDist,
                pointsPool = course.locationPoints,
                visited = visited,
                results = courseResults,
                maxPerSource = 3
            )

            // ── 🔹 탐색 결과 분석 📍
            Log.d("RUNUP_DFS", "   -> 탐색 종료: 발견된 경로 ${courseResults.size}개")

            if (courseResults.isNotEmpty()) {
                val subGroup = courseResults.mapIndexed { pathIndex, (dist, path) ->
                    val distanceInt = dist.toInt()
                    val centerPoint = calculateCenterPoint(path, currentLocation)

                    Log.d("RUNUP_DFS", "      [$pathIndex] 생성된 거리: ${distanceInt}m | 좌표수: ${path.size}")

                    saveToTestCollection(
                        distance = distanceInt,
                        path = path,
                        originId = course.id,
                        subIndex = pathIndex + 1
                    )

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

        Log.d("RUNUP_DFS", "─── ✅ 최종 생성된 그룹 수: ${allGroupsResult.size} ───")
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

        Log.v("RUNUP_DFS", "현재거리: ${currentDist.roundToInt()}m | 경로수: ${currentPath.size}")

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
            d in 1.0..10.0// 1미터에서 8미터 사이의 이웃 좌표들 탐색
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
                            dNext in 1.0..7.9 && calculateAngleDiff(lastPt, pt.locationPoint, nNext.locationPoint) < 30.0
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

        // 비슷한 각도(±10도) 내에서 가장 가까운 점 하나만 남기기
        val filteredNeighbors = mutableListOf<NeighborCandidate>()

        // 각도가 작은 순서(직선에 가까운 순)로 정렬해서 비교하거나, 그룹화 처리
        candidates.sortBy { it.distance } // 일단 거리순 정렬

        for (candidate in candidates) {
            // 이미 결과 리스트에 비슷한 각도(±10도)를 가진 더 짧은 거리의 점이 있는지 확인
            val isDuplicateDirection = filteredNeighbors.any { existing ->
                Math.abs(existing.angle - candidate.angle) <= 20.0
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

    // --- 유틸리티 함수 ---

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