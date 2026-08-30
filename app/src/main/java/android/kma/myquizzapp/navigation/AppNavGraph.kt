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
import android.kma.myquizzapp.presentation.profile.ProfileScreen
import android.kma.myquizzapp.feature.home.presentation.HomeScreen
import android.kma.myquizzapp.feature.home.presentation.search.SearchScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.createroom.CreateRoomScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz.CreateQuizScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz.EditQuizScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail.QuizDetailScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist.QuizManageListScreen
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.HostLobbyScreen
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
                    // N16.5: OTP đã verify ở màn trước → màn Reset nhận ticket.
                    onNavigateToResetPassword = { ticket, email ->
                        navController.navigate(
                            Route.ResetPassword(
                                ticket = ticket,
                                email = email
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
                    },
                    onNavigateToProfile = {
                        navController.navigate(Route.Profile)
                    }
                )
            }
            
            composable<Route.MyQuizzes> {
                // N13-14: danh sách "Quiz của tôi" — yêu cầu đăng nhập (cookie auth ở QuizApiService.getMyQuizzes).
                QuizManageListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateQuiz = { navController.navigate(Route.CreateQuiz) },
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
                // N13.5: màn Profile — chứa "Quiz của tôi" (trước đây là tab ở Home) và Đăng xuất.
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMyQuizzes = { navController.navigate(Route.MyQuizzes) },
                    onLoggedOut = { navController.popBackStack() }
                )
            }
            
            composable<Route.CreateQuiz> {
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQuizCreated = { quizId ->
                        // Thay CreateQuiz bằng QuizDetail trong back stack — quay lại sẽ về MyQuizzes.
                        navController.navigate(Route.QuizDetail(quizId)) {
                            popUpTo<Route.CreateQuiz> { inclusive = true }
                        }
                    }
                )
            }
            
            composable<Route.EditQuiz> {
                // N16: sửa quiz. quizId lấy từ route argument qua SavedStateHandle ở
                // EditQuizViewModel. Lưu xong → popBackStack về QuizDetail (màn detail
                // tự reload khi ON_RESUME để hiển thị bản mới).
                EditQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQuizUpdated = { navController.popBackStack() }
                )
            }
            
            composable<Route.QuizDetail> {
                QuizDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateRoom = { quizId ->
                        navController.navigate(Route.CreateRoom(quizId))
                    },
                    onNavigateToEditQuiz = { quizId ->
                        navController.navigate(Route.EditQuiz(quizId))
                    }
                )
            }
            
            composable<Route.CreateRoom> {
                CreateRoomScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHostLobby = { gameId, socketToken, sessionCode ->
                        navController.navigate(Route.HostLobby(gameId, socketToken, sessionCode)) {
                            popUpTo<Route.CreateRoom> { inclusive = true }
                        }
                    },
                    onRequireAuthentication = {
                        navController.navigate(Route.AuthGraph)
                    }
                )
            }
            
            // ----- GAMEPLAY ROUTES (Host vs Player ViewModels) -----
            composable<Route.PlayerLobby> {
                // TODO: PlayerLobbyScreen() - guest hoặc authenticated
                androidx.compose.material3.Text("Player Lobby - Coming Soon")
            }
            
            composable<Route.HostLobby> {
                // N18: màn lobby host thật ở feature:lobby (thay HostLobbyPlaceholder).
                // gameId / socketToken / sessionCode do HostLobbyViewModel đọc từ
                // SavedStateHandle nên ở đây không cần toRoute nữa.
                HostLobbyScreen(
                    // message != null là trường hợp bị buộc rời phòng (token sai, phòng
                    // không còn). TODO N19: hiển thị message này ở màn đích sau khi pop.
                    onExit = { navController.popBackStack() }
                )
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