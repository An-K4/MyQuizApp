package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
class ChangePasswordDto(val oldPassword: String, val newPassword: String) {
    override fun toString() = "ChangePasswordDto([REDACTED])"
}

@Serializable
class DeactivateAccountDto(val password: String) {
    override fun toString() = "DeactivateAccountDto([REDACTED])"
}

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
