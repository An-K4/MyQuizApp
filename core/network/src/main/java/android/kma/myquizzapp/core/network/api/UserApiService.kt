package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import retrofit2.http.GET

interface UserApiService {
    @GET("users/me")
    suspend fun getMe(): Result<AuthDataDto> // data cũng là { user }
}