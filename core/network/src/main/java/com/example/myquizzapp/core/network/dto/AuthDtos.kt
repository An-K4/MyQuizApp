package com.example.myquizzapp.core.network.dto

import com.example.myquizzapp.core.common.model.User
import kotlinx.serialization.Serializable

// Request bodies — khớp auth.schema.ts (zod)
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullname: String,
    // ⚠️ zod .optional() KHÔNG chấp nhận null → phải bỏ hẳn key khi rỗng.
    // kotlinx.serialization bỏ key khi giá trị = default, miễn Json không bật explicitNulls.
    val phone: String? = null,
)

@Serializable
data class GoogleOneTapRequest(val credential: String)

/**
 * login / register / one-tap / users/me đều trả envelope data = { user }.
 * KHÔNG có token trong body — token nằm trong Set-Cookie HttpOnly, CookieJar tự lo.
 */
@Serializable
data class AuthDataDto(val user: UserDto)

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val fullname: String,
    val phone: String? = null,
    val avatar: String? = null,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        fullname = fullname,
        phone = phone,
        avatar = avatar,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}