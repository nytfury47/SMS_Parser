package com.tan.smsparser.data.remote

import com.tan.smsparser.Constants

import okhttp3.RequestBody
import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST(Constants.API_ENDPOINT)
    suspend fun createSMSList(@Body requestBody: RequestBody): Response<ResponseBody>
}