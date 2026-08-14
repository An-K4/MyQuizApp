package android.kma.myquizzapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import android.kma.myquizzapp.auth.presentation.host.HostHomePlaceholder
import android.kma.myquizzapp.auth.presentation.login.LoginScreen
import android.kma.myquizzapp.auth.presentation.register.RegisterScreen
import android.kma.myquizzapp.presentation.splash.SplashScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Splash) {

        composable<Route.Splash> {
            SplashScreen(
                onLoggedIn = {
                    navController.navigate(Route.HostGraph) {
                        // Xóa Splash khỏi back stack: từ HostHome bấm Back là thoát app,
                        // không quay lại màn loading.
                        popUpTo<Route.Splash> { inclusive = true }
                    }
                },
                onLoggedOut = {
                    navController.navigate(Route.AuthGraph) {
                        popUpTo<Route.Splash> { inclusive = true }
                    }
                },
            )
        }

        navigation<Route.AuthGraph>(startDestination = Route.Login) {
            composable<Route.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Route.HostGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true } // Back không về Login nữa
                        }
                    },
                    onGoToRegister = { navController.navigate(Route.Register) },
                    onPlayAsGuest = {
                        navController.navigate(Route.PlayerGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Register> {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Route.HostGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() },
                )
            }
        }

        navigation<Route.PlayerGraph>(startDestination = Route.JoinRoom) {
            composable<Route.JoinRoom> { /* N19 — placeholder */ }
        }

        navigation<Route.HostGraph>(startDestination = Route.HostHome) {
            composable<Route.HostHome> {
                // Placeholder phục vụ M2: chứng minh cookie auth hoạt động (hiện tên user
                // từ /users/me) + nút logout. HostHome thật làm ở Tuần 3 (feature:home).
                HostHomePlaceholder(
                    onLoggedOut = {
                        navController.navigate(Route.AuthGraph) {
                            popUpTo<Route.HostGraph> { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}