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
    @Serializable data class ResetPassword(val token: String? = null, val email: String? = null, val otp: String? = null) : Route

    // ---- Main graph (unified for all users - guest + authenticated) ----
    @Serializable data object MainGraph : Route
    
    // Bottom nav routes
    @Serializable data object Home : Route
    @Serializable data object Discover : Route
    @Serializable data object JoinRoom : Route
    @Serializable data object Library : Route
    @Serializable data object Profile : Route
    
    // Quiz routes
    @Serializable data class QuizDetail(val quizId: Long) : Route
    @Serializable data object CreateQuiz : Route
    @Serializable data class EditQuiz(val quizId: Long) : Route
    
    // Room creation
    @Serializable data class CreateRoom(val quizId: Long) : Route
    
    // Gameplay routes - truyền socketToken qua argument
    @Serializable data class PlayerLobby(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class HostLobby(val gameId: Long, val socketToken: String) : Route
    @Serializable data class GamePlay(val gameId: Long, val playerId: Long, val socketToken: String) : Route
    @Serializable data class HostGame(val gameId: Long, val socketToken: String) : Route
    @Serializable data class FinalResult(val gameId: Long) : Route
}