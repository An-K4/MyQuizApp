package android.kma.myquizzapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import android.kma.myquizzapp.feature.home.presentation.HomeScreen
import android.kma.myquizzapp.feature.home.presentation.search.SearchScreen
import android.kma.myquizzapp.feature.lobby.presentation.joinroom.JoinRoomCard
import android.kma.myquizzapp.presentation.activity.ActivityScreen
import android.kma.myquizzapp.presentation.profile.ProfileScreen

/**
 * Main application navigation graph.
 * Contains public routes (Home, Search, Discover) and protected routes (Activity, Profile).
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
    composable<Route.Home> { entry ->
        // Trang chủ là màn còn sống sau khi lobby bị pop, nên từ N19.6 nó nhận và
        // hiển thị lý do bị buộc rời phòng (xem KEY_LOBBY_EXIT_MESSAGE ở
        // GameNavGraph) — trước đây việc này do màn Join đảm nhiệm.
        val exitMessage by entry.savedStateHandle
            .getStateFlow<String?>(KEY_LOBBY_EXIT_MESSAGE, null)
            .collectAsState()

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
            onNavigateToDiscover = { sectionKey ->
                navController.navigate(Route.Discover(sectionKey))
            },
            // Ô nhập mã phòng — trước N19.6 là cả một màn riêng (Route.JoinRoom).
            // Tầng navigation nối thế của feature:lobby vào Home để hai feature
            // không phải biết nhau.
            roomCodeCard = {
                JoinRoomCard(
                    onNavigateToPlayerLobby = { gameId, playerId, socketToken ->
                        navController.navigate(
                            Route.PlayerLobby(gameId, playerId, socketToken)
                        )
                    },
                    onNavigateToGuestNickname = { sessionCode ->
                        navController.navigate(Route.GuestNickname(sessionCode))
                    },
                    onNavigateToLogin = {
                        navController.navigate(Route.AuthGraph) {
                            popUpTo<Route.MainGraph> { inclusive = false }
                        }
                    },
                    exitMessage = exitMessage,
                    onExitMessageShown = {
                        entry.savedStateHandle[KEY_LOBBY_EXIT_MESSAGE] = null
                    }
                )
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

    composable<Route.Discover> { entry ->
        // TODO: DiscoverScreen() — gọi GET /quizzes/search và GET /quizzes/feed.
        // Màn thật còn là một mốc riêng trong kế hoạch; placeholder này hiện
        // sẵn sectionKey để kiểm tra nút "Xem thêm" ở Trang chủ truyền đúng.
        val sectionKey = entry.toRoute<Route.Discover>().sectionKey
        Text("Discover - Coming Soon (section: ${sectionKey ?: "tất cả"})")
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
            // Khách vẫn vào được tab này — không gác bằng hộp thoại, vì một tab của
            // bottom nav bị chặn ngay khi bấm thì không còn là tab nữa. Màn tự hiện
            // empty state có nút đăng nhập (N19.6).
            onNavigateToAuth = {
                navController.navigate(Route.AuthGraph) {
                    popUpTo<Route.MainGraph> { inclusive = false }
                }
            },
            onLoggedOut = {
                // Xoá avatar khỏi bottom nav: LogoutUseCase đã clear cookie nên
                // /users/me sẽ trả 401 → tab Hồ sơ về lại icon mặc định.
                onCurrentUserChanged()
                navController.navigateToTab(Route.Home)
            }
        )
    }
}
