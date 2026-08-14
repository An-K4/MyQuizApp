package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import android.kma.myquizzapp.core.network.dto.GoogleOneTapRequest
import android.kma.myquizzapp.core.network.dto.LoginRequest
import android.kma.myquizzapp.core.network.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login") // path tương đối, không dấu / đầu — BASE_URL đã là .../v1/
    suspend fun login(@Body body: LoginRequest): Result<AuthDataDto>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Result<AuthDataDto>

    @POST("auth/google/one-tap")
    suspend fun loginWithGoogle(@Body body: GoogleOneTapRequest): Result<AuthDataDto>

    @POST("auth/logout")
    suspend fun logout(): Result<Unit>
    @POST("auth/refresh")
    suspend fun refresh(): Result<Unit>
}