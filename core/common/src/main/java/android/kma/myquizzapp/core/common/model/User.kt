package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val fullname: String,
    val email: String,
    val phone: String? = null,
    val role: UserRole = UserRole.USER,
    val avatar: String? = null,
    val description: String? = null,
    val authProvider: AuthProvider = AuthProvider.LOCAL,
    val createdAt: String,
    val updatedAt: String
    // password, google_id, deleted_at: không bao giờ lộ ra client
)

@Serializable
enum class UserRole {
    @SerialName("admin") ADMIN,
    @SerialName("moderator") MODERATOR,
    @SerialName("user") USER
}

@Serializable
enum class AuthProvider {
    @SerialName("local") LOCAL,
    @SerialName("google") GOOGLE
}