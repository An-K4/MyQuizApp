package android.kma.myquizzapp.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import android.kma.myquizzapp.feature.home.presentation.HomeScreen
import android.kma.myquizzapp.feature.home.presentation.search.SearchScreen
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
            }
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

    composable<Route.JoinRoom> {
        // TODO: JoinRoomScreen() - guest có thể join bằng nickname
        // Placeholder
        Text("Join Room - Coming Soon")
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
