package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

/**
 * Hiệu ứng một lần của màn lobby host.
 *
 * [ExitLobby] dùng cho cả hai tình huống "phải rời phòng": người dùng tự bấm
 * thoát (message = null) và lỗi không thể phục hồi (message != null, VD token sai
 * hoặc phòng không còn). Gộp làm một vì đối với tầng navigation chúng giống
 * nhau: pop khỏi backstack; chỉ khác có kèm thông báo hay không.
 */
sealed interface HostLobbyEffect {
    data class ExitLobby(val message: String? = null) : HostLobbyEffect
}
