package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfilePatchDto(
    val fullname: String? = null,
    val phone: String? = null,
    val description: String? = null
)

@Serializable
data class AvatarRequestDto(val fileUrl: String)

@Serializable
data class AvatarResponseDto(val avatarUrl: String)
