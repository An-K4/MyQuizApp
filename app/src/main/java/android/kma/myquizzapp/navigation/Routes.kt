package android.kma.myquizzapp.navigation

import kotlinx.serialization.Serializable

/** Toàn bộ route của app. data object = không tham số; data class = có tham số. */
sealed interface Route {
    @Serializable data object Splash : Route

    // ---- Auth graph ----
    @Serializable data object AuthGraph : Route
    @Serializable data object Login : Route
    @Serializable data object Register : Route

    // ---- Player graph (không cần tài khoản) — Tuần 4+ ----
    @Serializable data object PlayerGraph : Route
    @Serializable data object JoinRoom : Route
    @Serializable data class PlayerLobby(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class GamePlay(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class FinalResult(val gameId: Long) : Route

    // ---- Host graph (yêu cầu đăng nhập) ----
    @Serializable data object HostGraph : Route
    @Serializable data object HostHome : Route
    @Serializable data object QuizManage : Route
    @Serializable data class CreateRoom(val quizId: Long) : Route
    @Serializable data class HostLobby(val gameId: Long) : Route
    @Serializable data class HostGame(val gameId: Long, val socketToken: String) : Route
}