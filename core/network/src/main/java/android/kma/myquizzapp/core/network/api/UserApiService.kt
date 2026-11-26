package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import retrofit2.http.GET
import retrofit2.http.Tag
import android.kma.myquizzapp.core.network.cookie.CredentialEpoch

interface UserApiService {
    @GET("users/me")
    suspend fun getMe(@Tag epoch: CredentialEpoch? = null): Result<AuthDataDto> // data cũng là { user }
}