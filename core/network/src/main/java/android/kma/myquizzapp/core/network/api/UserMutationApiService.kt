package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import android.kma.myquizzapp.core.network.dto.AvatarRequestDto
import android.kma.myquizzapp.core.network.dto.AvatarResponseDto
import android.kma.myquizzapp.core.network.dto.UserProfilePatchDto
import retrofit2.http.Body
import retrofit2.http.PATCH

/** PreserveCaseRetrofit: fileUrl/avatarUrl are camelCase, nested user is snake_case. */
interface UserMutationApiService {
    @PATCH("users/me")
    suspend fun updateProfile(@Body patch: UserProfilePatchDto): Result<AuthDataDto>

    @PATCH("users/me/avatar")
    suspend fun updateAvatar(@Body request: AvatarRequestDto): Result<AvatarResponseDto>
}
