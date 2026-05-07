package com.runit.runup.domain.model

// API 응답을 받아올 DTO (Data Transfer Object)
data class GeocodingResponse(
    val status: Status,
    val results: List<ResultItem>
)

data class Status(val code: Int, val name: String)
data class ResultItem(
    val region: Region,
    val land: Land?
)

data class Region(
    val area1: Area, // 시/도 (대구광역시)
    val area2: Area, // 구/군 (북구)
    val area3: Area, // 동/읍/면 (대현동)
    val area4: Area
)
data class Area(val name: String)
data class Land(val number1: String, val number2: String)

// 2. 앱 내에서 실제로 사용할 가공된 데이터 모델
data class AddressModel(
    val fullAddress: String,    // 전체 주소
    val displayAddress: String, // 화면 표시용 (예: 북구 대현동)
    val city: String,          // 대구광역시
    val district: String,      // 북구
    val dong: String           // 대현동
)