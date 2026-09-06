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
import android.kma.myquizzapp.presentation.activity.ActivityScreen
import android.kma.myquizzapp.presentation.profile.ProfileScreen

/**
 * Main application navigation graph.
 * Contains public routes (Home, Search, Discover, JoinRoom) and protected routes (Activity, Profile).
 *
 * @param onCurrentUserChanged gọi khi phiên đăng nhập vừa đổi (hiện tại: đăng xuất)
 *   để bottom nav nạp lại avatar. Bottom bar sống NGOÀI NavHost nên không tự biết
 *   những thay đổi xảy ra bên trong graph.
 */
fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    onCurrentUserChanged: () -> Unit
) {
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
            }
            // Avatar ở top bar đã bỏ — lối vào Hồ sơ giờ là tab cuối bottom nav,
            // nơi chính avatar đó được dùng làm icon.
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
    composable<Route.Activity> {
        // Tab Hoạt động — N19.5 chỉ dựng placeholder. Hợp đồng backend đã audit
        // được ghi trong KDoc của ActivityScreen.
        ActivityScreen()
    }

    composable<Route.Profile> {
        // Tab Hồ sơ — từ N19.5 chỉ còn thông tin người dùng + Đăng xuất; lối vào
        // "Quiz của tôi" đã thành tab Thư viện. Là tab nên không có nút back.
        // Đăng xuất xong quay về tab Trang chủ (trước đây popBackStack sẽ thoát app).
        ProfileScreen(
            onLoggedOut = {
                // Xoá avatar khỏi bottom nav: LogoutUseCase đã clear cookie nên
                // /users/me sẽ trả 401 → tab Hồ sơ về lại icon mặc định.
                onCurrentUserChanged()
                navController.navigateToTab(Route.Home)
            }
        )
    }
}
