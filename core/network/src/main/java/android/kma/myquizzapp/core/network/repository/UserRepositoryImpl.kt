package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.UserProfilePatch
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.UserApiService
import android.kma.myquizzapp.core.network.api.UserMutationApiService
import android.kma.myquizzapp.core.network.dto.AvatarRequestDto
import android.kma.myquizzapp.core.network.dto.UserProfilePatchDto
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val mutationApi: UserMutationApiService,
    private val userApi: UserApiService
) : UserRepository {
    override suspend fun updateProfile(patch: UserProfilePatch): Result<User> =
        mutationApi.updateProfile(UserProfilePatchDto(patch.fullname, patch.phone, patch.description))
            .map { it.user.toDomain() }

    override suspend fun updateAvatar(fileUrl: String): Result<String> =
        mutationApi.updateAvatar(AvatarRequestDto(fileUrl)).map { it.avatarUrl }

    override suspend fun getCurrentUser(): Result<User> = userApi.getMe().map { it.user.toDomain() }
}
