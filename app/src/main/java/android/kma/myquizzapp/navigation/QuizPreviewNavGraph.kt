package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.feature.quiz_preview.QuizPreviewScreen
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

fun NavGraphBuilder.quizPreviewGraph(navController: NavHostController) {
    composable<Route.QuizPreview> {
        QuizPreviewScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEditQuiz = { quizId ->
                navController.navigate(Route.EditQuiz(quizId)) {
                    popUpTo<Route.QuizPreview> { inclusive = true }
                }
            }
        )
    }
}
