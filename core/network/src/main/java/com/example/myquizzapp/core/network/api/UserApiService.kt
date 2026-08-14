package com.example.myquizzapp.core.network.api

import com.example.myquizzapp.core.common.result.Result
import com.example.myquizzapp.core.network.dto.AuthDataDto
import retrofit2.http.GET

interface UserApiService {
    @GET("users/me")
    suspend fun getMe(): Result<AuthDataDto> // data cũng là { user }
}