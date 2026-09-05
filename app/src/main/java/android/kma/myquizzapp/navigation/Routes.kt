package android.kma.myquizzapp.navigation

import kotlinx.serialization.Serializable

/** Toàn bộ route của app. data object = không tham số; data class = có tham số. */
sealed interface Route {
    @Serializable data object Splash : Route

    // ---- Auth graph (modal flow) ----
    @Serializable data object AuthGraph : Route
    @Serializable data object Login : Route
    @Serializable data object Register : Route
    @Serializable data object ForgotPassword : Route
    @Serializable data class OtpVerification(val email: String) : Route
    // N16.5: màn Reset làm việc với TICKET (từ màn OTP, đã verify) hoặc TOKEN
    // (deep link email → verify đổi lấy ticket). Không còn truyền otp qua nav.
    @Serializable data class ResetPassword(val ticket: String? = null, val token: String? = null, val email: String? = null) : Route

    // ---- Main graph (unified for all users - guest + authenticated) ----
    @Serializable data object MainGraph : Route
    
    // Bottom nav routes
    @Serializable data object Home : Route
    @Serializable data object Discover : Route
    @Serializable data object JoinRoom : Route
    @Serializable data object Library : Route
    @Serializable data object Profile : Route
    
    // Search (full-screen modal from Home)
    @Serializable data object Search : Route
    
    // Quiz routes
    @Serializable data class QuizDetail(val quizId: Long) : Route
    @Serializable data object MyQuizzes : Route
    @Serializable data object CreateQuiz : Route
    @Serializable data class EditQuiz(val quizId: Long) : Route
    
    // Room creation
    @Serializable data class CreateRoom(val quizId: Long) : Route
    
    // Nhập tên hiển thị cho KHÁCH — chỉ nằm giữa JoinRoom và PlayerLobby.
    // Người đã đăng nhập không đi qua route này (backend lấy tên từ tài khoản).
    @Serializable data class GuestNickname(val sessionCode: String) : Route

    // Gameplay routes - truyền socketToken qua argument
    @Serializable data class PlayerLobby(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class HostLobby(
        val gameId: Long,
        val socketToken: String,
        val sessionCode: String
    ) : Route
    @Serializable data class GamePlay(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class HostGame(val gameId: Long, val socketToken: String) : Route
    @Serializable data class FinalResult(val gameId: Long) : Route
}