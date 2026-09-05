package android.kma.myquizzapp.feature.lobby.presentation.guestnickname

/**
 * MVI contract của màn nhập tên hiển thị cho KHÁCH.
 *
 * Màn này chỉ xuất hiện khi đã biết chắc: chưa đăng nhập VÀ phòng cho phép
 * khách. Người đã đăng nhập không bao giờ đi qua đây.
 */
data class GuestNicknameUiState(
    val sessionCode: String = "",
    val nickname: String = "",
    val nicknameError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean get() = nickname.isNotBlank() && !isSubmitting
}

sealed interface GuestNicknameIntent {
    data class NicknameChanged(val value: String) : GuestNicknameIntent
    data object Submit : GuestNicknameIntent
    data object ErrorShown : GuestNicknameIntent
}

sealed interface GuestNicknameEffect {
    data class NavigateToPlayerLobby(
        val gameId: Long,
        val playerId: Long,
        val socketToken: String
    ) : GuestNicknameEffect

    /** Phòng đổi ý (khóa khách, đầy, đã bắt đầu) — quay lại màn nhập mã. */
    data class ExitWithMessage(val message: String) : GuestNicknameEffect
}
