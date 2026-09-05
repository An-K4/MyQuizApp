package android.kma.myquizzapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import android.kma.myquizzapp.feature.home.presentation.HomeScreen
import android.kma.myquizzapp.feature.home.presentation.search.SearchScreen
import android.kma.myquizzapp.feature.lobby.presentation.joinroom.JoinRoomScreen
import android.kma.myquizzapp.presentation.profile.ProfileScreen

/**
 * Main application navigation graph.
 * Contains public routes (Home, Search, Discover, JoinRoom) and protected routes (Library, Profile).
 */
fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    // ----- PUBLIC ROUTES (accessible to everyone) -----
    composable<Route.Home> {
        HomeScreen(
            onNavigateToSearch = { navController.navigate(Route.Search) },
            onNavigateToAuth = {
                navController.navigate(Route.AuthGraph) {
                    popUpTo<Route.MainGraph> { inclusive = false }
                }
            },
            onNavigateToQuizDetail = { quizId ->
                navController.navigate(Route.QuizDetail(quizId))
            },
            onNavigateToProfile = {
                navController.navigate(Route.Profile)
            },
            // Tạm thời cho tới khi có Bottom Navigation (N19.5).
            onNavigateToJoinRoom = { navController.navigate(Route.JoinRoom) }
        )
    }

    composable<Route.Search> {
        SearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuizDetail = { quizId ->
                navController.navigate(Route.QuizDetail(quizId))
            }
        )
    }

    composable<Route.Discover> {
        // TODO: DiscoverScreen() - gọi GET /quizzes/search
        // Placeholder
        Text("Discover - Coming Soon")
    }

    composable<Route.JoinRoom> { entry ->
        // Màn này còn sống sau khi lobby bị pop nên nó nhận và hiển thị lý do
        // bị buộc rời phòng (xem KEY_LOBBY_EXIT_MESSAGE ở GameNavGraph).
        val exitMessage by entry.savedStateHandle
            .getStateFlow<String?>(KEY_LOBBY_EXIT_MESSAGE, null)
            .collectAsState()

        JoinRoomScreen(
            onNavigateToPlayerLobby = { gameId, playerId, socketToken ->
                navController.navigate(Route.PlayerLobby(gameId, playerId, socketToken))
            },
            onNavigateToGuestNickname = { sessionCode ->
                navController.navigate(Route.GuestNickname(sessionCode))
            },
            onNavigateToLogin = {
                navController.navigate(Route.AuthGraph) {
                    popUpTo<Route.MainGraph> { inclusive = false }
                }
            },
            onBack = { navController.popBackStack() },
            exitMessage = exitMessage,
            onExitMessageShown = { entry.savedStateHandle[KEY_LOBBY_EXIT_MESSAGE] = null }
        )
    }

    // ----- PROTECTED ROUTES (require auth) -----
    composable<Route.Library> {
        // TODO: RequireAuth { LibraryScreen() }
        // Placeholder
        Text("Library - Requires Auth")
    }

    composable<Route.Profile> {
        // Màn Profile — chứa "Quiz của tôi" và Đăng xuất.
        ProfileScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToMyQuizzes = { navController.navigate(Route.MyQuizzes) },
            onLoggedOut = { navController.popBackStack() }
        )
    }
}
