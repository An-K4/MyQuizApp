package android.kma.myquizzapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import android.kma.myquizzapp.presentation.splash.SplashScreen

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

        // ===== AUTH GRAPH =====
        authGraph(navController)

        // ===== MAIN GRAPH (Unified - Guest + Authenticated) =====
        navigation<Route.MainGraph>(startDestination = Route.Home) {
            mainGraph(navController)
            quizManageGraph(navController)
            gameGraph(navController)
        }
    }
}