package com.example.runup.domain.model

import com.google.gson.annotations.SerializedName

// LocationResponse.kt
// 🔹 1. 최상위 루트 태그 매칭

// 🔹 1. XML/JSON의 최상위 루트 이름인 'StanReginCd'와 내부 리스트 'row' 매칭


// 🔹 1. JSON 최상위 구조 (StanReginCd가 리스트임!)
data class DistrictResponse(
    @SerializedName("StanReginCd")
    val stanReginCd: List<StanReginCdItem>?
)

// 🔹 2. 리스트 내부의 객체들 (첫 번째는 head, 두 번째는 row가 들어있음)
data class StanReginCdItem(
    @SerializedName("head")
    val head: List<Map<String, Any>>?, // head 부분은 무시해도 됨

    @SerializedName("row") // 🔹 실제 우리가 필요한 지역 데이터 리스트
    val row: List<AdmVO>?
)

// 🔹 3. 실제 지역 정보 (가져오신 JSON의 키값과 1:1 매칭)
data class AdmVO(
    @SerializedName("region_cd")
    val admCode: String,

    @SerializedName("locallow_nm")
    val lowestAdmName: String,

    @SerializedName("locathigh_cd")
    val locathighCd: String,

    @SerializedName("locatadd_nm")
    val fullAddress: String // 🔹 "대구광역시 서구" 전체 이름 필요시 사용
)