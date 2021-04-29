package com.tan.smsparser.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST("/api/v1/create")
    suspend fun createEmployee(@Body requestBody: RequestBody): Response<ResponseBody>
}