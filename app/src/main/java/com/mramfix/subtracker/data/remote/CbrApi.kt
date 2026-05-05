package com.mramfix.subtracker.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface CbrApi {
    @GET("daily_json.js")
    suspend fun dailyRates(): Response<ResponseBody>
}
