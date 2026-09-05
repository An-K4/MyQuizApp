package android.kma.myquizzapp.feature.lobby.presentation.joinroom

/**
 * MVI contract của màn nhập mã phòng.
 *
 * Màn này CHỈ có ô mã phòng — không có ô nhập tên. Lý do: người đã đăng nhập
 * không được đặt tên khác (server lấy fullname từ tài khoản và bỏ qua body), còn
 * khách thì phải qua màn nhập tên riêng — và chỉ khi phòng cho phép khách.
 */
data class JoinRoomUiState(
    val sessionCode: String = "",
    val isSubmitting: Boolean = false,
    /** Lỗi gắn trực tiếp dưới ô mã (sai mã, phòng đầy, trận đã bắt đầu). */
    val codeError: String? = null,
    /** Lỗi chung hiển thị bằng snackbar (mất mạng, lỗi server). */
    val errorMessage: String? = null,
    /** Phòng không nhận khách — hiện dialog mời đăng nhập. */
    val guestBlocked: Boolean = false
) {
    val canSubmit: Boolean get() = sessionCode.isNotBlank() && !isSubmitting
}

sealed interface JoinRoomIntent {
    data class CodeChanged(val value: String) : JoinRoomIntent
    data object Submit : JoinRoomIntent
    data object GuestBlockedDismissed : JoinRoomIntent
    data object GuestBlockedLoginClicked : JoinRoomIntent
    data object ErrorShown : JoinRoomIntent
}

sealed interface JoinRoomEffect {
    /** Đã đăng nhập và join xong — vào thẳng phòng chờ. */
    data class NavigateToPlayerLobby(
        val gameId: Long,
        val playerId: Long,
        val socketToken: String
    ) : JoinRoomEffect

    /** Là khách và phòng cho phép khách — sang màn nhập tên hiển thị. */
    data class NavigateToGuestNickname(val sessionCode: String) : JoinRoomEffect

    data object NavigateToLogin : JoinRoomEffect
}
