package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.Serializable

/**
 * Public identity của quiz author, dùng cho listing cards và detail views.
 * 
 * Owner có thể null khi author bị soft-deleted - client nên hiển thị
 * neutral label thay vì tên bị hỏng.
 */
@Serializable
data class QuizOwner(
    val id: Long,
    val fullname: String,
    val avatar: String? = null
)
