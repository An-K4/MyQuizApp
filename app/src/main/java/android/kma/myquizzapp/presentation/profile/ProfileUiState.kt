package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.core.common.model.User

/**
 * UI state cho màn Profile.
 *
 * Màn này yêu cằu đăng nhập (chỉ đạt tới đây qua avatar ở Home khi đã
 * đăng nhập), nên [user] = null chỉ xảy ra trong lúc đang tải hoặc nếu
 * session vừa hết hạn.
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
