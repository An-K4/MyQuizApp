package com.example.myquizzapp.core.network.api

import com.example.myquizzapp.core.common.result.Result
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/refresh")
    suspend fun refresh(): Result<Unit>
}