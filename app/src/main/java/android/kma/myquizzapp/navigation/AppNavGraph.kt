package android.kma.myquizzapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import android.kma.myquizzapp.feature.auth.presentation.forgot.ForgotPasswordScreen
import android.kma.myquizzapp.feature.auth.presentation.login.LoginScreen
import android.kma.myquizzapp.feature.auth.presentation.otp.OtpVerificationScreen
import android.kma.myquizzapp.feature.auth.presentation.register.RegisterScreen
import android.kma.myquizzapp.feature.auth.presentation.reset.ResetPasswordScreen
import android.kma.myquizzapp.presentation.splash.SplashScreen
import android.kma.myquizzapp.feature.home.presentation.HomeScreen
import android.kma.myquizzapp.feature.home.presentation.search.SearchScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail.QuizDetailScreen
import androidx.navigation.toRoute

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
    
    NavHost(navController = navController, startDestination = Route.Splash) {

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

        // ===== AUTH GRAPH (Modal) =====
        navigation<Route.AuthGraph>(startDestination = Route.Login) {
            composable<Route.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Route.MainGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true }
                        }
                    },
                    onGoToRegister = { navController.navigate(Route.Register) },
                    onGoToForgotPassword = { navController.navigate(Route.ForgotPassword) },
                    onPlayAsGuest = {
                        navController.navigate(Route.MainGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Register> {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Route.MainGraph) {
                            popUpTo<Route.AuthGraph> { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() },
                )
            }
            
            composable<Route.ForgotPassword> {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOtpVerification = { email ->
                        navController.navigate(Route.OtpVerification(email = email))
                    }
                )
            }
            
            composable<Route.OtpVerification> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.OtpVerification>()
                OtpVerificationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResetPassword = { email, otp ->
                        navController.navigate(
                            Route.ResetPassword(
                                token = null,
                                email = email,
                                otp = otp
                            )
                        )
                    }
                )
            }
            
            composable<Route.ResetPassword> { backStackEntry ->
                val args = backStackEntry.toRoute<Route.ResetPassword>()
                ResetPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { 
                        navController.navigate(Route.Login) {
                            popUpTo<Route.AuthGraph> { inclusive = false }
                        }
                    }
                )
            }
        }

        // ===== MAIN GRAPH (Unified - Guest + Authenticated) =====
        navigation<Route.MainGraph>(startDestination = Route.Home) {
            
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
                androidx.compose.material3.Text("Discover - Coming Soon")
            }
            
            composable<Route.JoinRoom> {
                // TODO: JoinRoomScreen() - guest có thể join bằng nickname
                // Placeholder
                androidx.compose.material3.Text("Join Room - Coming Soon")
            }
            
            // ----- PROTECTED ROUTES (require auth) -----
            composable<Route.Library> {
                // TODO: RequireAuth { LibraryScreen() }
                // Placeholder
                androidx.compose.material3.Text("Library - Requires Auth")
            }
            
            composable<Route.Profile> {
                // TODO: RequireAuth { ProfileScreen() }
                // Placeholder
                androidx.compose.material3.Text("Profile - Requires Auth")
            }
            
            composable<Route.CreateQuiz> {
                // TODO: RequireAuth { CreateQuizScreen() }
                // Placeholder
                androidx.compose.material3.Text("Create Quiz - Requires Auth")
            }
            
            composable<Route.EditQuiz> {
                // TODO: RequireAuth { EditQuizScreen() }
                // Placeholder
                androidx.compose.material3.Text("Edit Quiz - Requires Auth")
            }
            
            composable<Route.QuizDetail> {
                QuizDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateRoom = { quizId ->
                        navController.navigate(Route.CreateRoom(quizId))
                    }
                )
            }
            
            composable<Route.CreateRoom> {
                // TODO: RequireAuth { CreateRoomScreen() }
                // Placeholder
                androidx.compose.material3.Text("Create Room - Requires Auth")
            }
            
            // ----- GAMEPLAY ROUTES (Host vs Player ViewModels) -----
            composable<Route.PlayerLobby> {
                // TODO: PlayerLobbyScreen() - guest hoặc authenticated
                androidx.compose.material3.Text("Player Lobby - Coming Soon")
            }
            
            composable<Route.HostLobby> {
                // TODO: HostLobbyScreen() - cần authenticated
                androidx.compose.material3.Text("Host Lobby - Coming Soon")
            }
            
            composable<Route.GamePlay> {
                // TODO: GamePlayScreen() - dùng GameViewModel
                androidx.compose.material3.Text("Game Play - Coming Soon")
            }
            
            composable<Route.HostGame> {
                // TODO: HostGameScreen() - dùng HostGameViewModel riêng
                androidx.compose.material3.Text("Host Game - Coming Soon")
            }
            
            composable<Route.FinalResult> {
                // TODO: FinalResultScreen()
                androidx.compose.material3.Text("Final Result - Coming Soon")
            }
        }
    }
}