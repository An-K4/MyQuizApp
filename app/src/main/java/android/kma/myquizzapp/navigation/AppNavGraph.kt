package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.presentation.splash.SplashScreen
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    initialDeepLinkToken: String? = null
) {
    // Handle deep link navigation to password reset
    LaunchedEffect(initialDeepLinkToken) {
        initialDeepLinkToken?.let { token ->
            navController.navigate(Route.ResetPassword(token = token))
        }
    }

    // Bottom nav (N19.5): chỉ hiện khi đang đứng ở 1 trong 5 tab cấp cao nhất
    // (xem TopLevelTab). Màn con (Search, QuizDetail, editor) và toàn bộ màn game
    // đều tự động ẩn bar, nên không cần logic ẩn/hiện riêng ở tứng graph.
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        currentDestination?.hasRoute(tab.route::class) == true
    }

    // Avatar cho tab "Hồ sơ". ViewModel scope theo Activity (xem CurrentUserViewModel)
    // vì bottom bar nằm ngoài NavHost, không thuộc back stack entry nào.
    val currentUserViewModel: CurrentUserViewModel = hiltViewModel()
    val avatarUrl by currentUserViewModel.avatarUrl.collectAsStateWithLifecycle()

    // Nạp lần đầu, và nạp lại mỗi khi vừa RỜI luồng auth (đăng nhập/đăng ký xong).
    // Key theo inAuthGraph nên effect chỉ chạy ở đúng lằn ranh auth, KHÔNG chạy
    // theo mỗi lần đổi tab — /users/me không có cache nên mỗi lần gọi là 1 request.
    val inAuthGraph = currentDestination?.hierarchy?.any {
        it.hasRoute(Route.AuthGraph::class)
    } == true
    LaunchedEffect(inAuthGraph) {
        if (!inAuthGraph) currentUserViewModel.refresh()
    }

    // App quay lại foreground: user có thể đã đổi avatar ở web trong lúc đó.
    // Lời gọi trùng lúc khởi động được refresh() tự bỏ qua.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentUserViewModel.refresh()
    }

    Scaffold(
        // Inset hệ thống để từng màn con tự xử lý (chúng đều có Scaffold riêng);
        // Scaffold ngoài này chỉ làm một việc: chừa chỗ cho bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedTab != null) {
                MainBottomBar(
                    selected = selectedTab.route,
                    onSelect = { route -> navController.navigateToTab(route) },
                    avatarUrl = avatarUrl
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Splash,
            modifier = Modifier
                .padding(innerPadding)
                // NavigationBar đã gồm inset navigation bar của hệ thống; consume để
                // màn con không cộng dồn lần hai. Khi ẩn bar thì innerPadding = 0 nên
                // các màn con (game, editor) vẫn tự áp inset y như trước N19.5.
                .consumeWindowInsets(innerPadding)
        ) {
            // ===== SPLASH SCREEN =====
            composable<Route.Splash> {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Route.MainGraph) {
                            popUpTo<Route.Splash> { inclusive = true }
                        }
                    },
                )
            }

            // ===== AUTH GRAPH =====
            authGraph(navController)

            // ===== MAIN GRAPH (Unified - Guest + Authenticated) =====
            navigation<Route.MainGraph>(startDestination = Route.Home) {
                mainGraph(navController, onCurrentUserChanged = currentUserViewModel::refresh)
                quizManageGraph(navController)
                gameGraph(navController)
            }
        }
    }
}
