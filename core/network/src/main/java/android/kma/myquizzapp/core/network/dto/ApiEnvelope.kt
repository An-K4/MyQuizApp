package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorBody? = null,
    val meta: Meta? = null
)

@Serializable
data class ApiErrorBody(
    val message: String,
    val details: JsonElement? = null   // backend gửi details kiểu gì cũng có — JsonElement cho an toàn
)

@Serializable
data class Meta(val timestamp: String? = null)