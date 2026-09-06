package android.kma.myquizzapp.feature.lobby.presentation.joinroom

/**
 * MVI contract của thẻ nhập mã phòng.
 *
 * Thẻ này CHỈ có ô mã phòng — không có ô nhập tên. Lý do: người đã đăng nhập
 * không được đặt tên khác (server lấy fullname từ tài khoản và bỏ qua body), còn
 * khách thì phải qua màn nhập tên riêng — và chỉ khi phòng cho phép khách.
 */
data class JoinRoomUiState(
    val sessionCode: String = "",
    val isSubmitting: Boolean = false,
    /** Lỗi gắn trực tiếp dưới ô mã (sai mã, phòng đầy, trận đã bắt đầu). */
    val codeError: String? = null,
    /** Lỗi chung hiển thị riêng trong thẻ (mất mạng, lỗi server). */
    val errorMessage: String? = null,
    /** Phòng không nhận khách — hiện dialog mời đăng nhập. */
    val guestBlocked: Boolean = false
) {
    val canSubmit: Boolean get() = sessionCode.length == SESSION_CODE_LENGTH && !isSubmitting
}

/**
 * Độ dài mã phòng — CỐ ĐỊNH 6 ký tự, không phải "từ 6 trở lên".
 *
 * Backend sinh mã bằng `generateSessionCode(len = 6)` từ bảng chữ
 * `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` — bỏ I, O, 0, 1 cho khỏi nhầm khi đọc mã
 * cho nhau. Mã dài 7 ký tự không tồn tại, nên cho bấm "Vào phòng" ở độ dài đó
 * chỉ đổi một lượt gọi mạng thành 404.
 */
const val SESSION_CODE_LENGTH = 6

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
