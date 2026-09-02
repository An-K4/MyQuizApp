package android.kma.myquizzapp.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import android.kma.myquizzapp.feature.auth.presentation.forgot.ForgotPasswordScreen
import android.kma.myquizzapp.feature.auth.presentation.login.LoginScreen
import android.kma.myquizzapp.feature.auth.presentation.otp.OtpVerificationScreen
import android.kma.myquizzapp.feature.auth.presentation.register.RegisterScreen
import android.kma.myquizzapp.feature.auth.presentation.reset.ResetPasswordScreen

/**
 * Authentication navigation graph.
 * Contains all auth-related routes: Login, Register, ForgotPassword, OtpVerification, ResetPassword.
 */
fun NavGraphBuilder.authGraph(navController: NavHostController) {
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
                // OTP đã verify ở màn trước → màn Reset nhận ticket.
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
}
