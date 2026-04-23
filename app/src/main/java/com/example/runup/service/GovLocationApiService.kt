package com.example.runup.service

import com.example.runup.domain.model.DistrictResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GovLocationApiService {
    @GET("1741000/StanReginCd/getStanReginCdList")
    suspend fun getLocations(
        @Query("ServiceKey") key: String,
        @Query("type") type: String = "json",
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("pageNo") pageNo: Int = 1,
        @Query("locatadd_nm") locationName: String? = null,
        @Query("locathigh_cd") parentCode: String? = null
    ): DistrictResponse
}