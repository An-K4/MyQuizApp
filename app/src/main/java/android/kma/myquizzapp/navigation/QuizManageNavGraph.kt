package android.kma.myquizzapp.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import android.kma.myquizzapp.feature.quiz_manage.presentation.createroom.CreateRoomScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz.CreateQuizScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz.EditQuizScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail.QuizDetailScreen
import android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist.QuizManageListScreen

/**
 * Quiz management navigation graph.
 * Contains quiz CRUD routes: MyQuizzes, CreateQuiz, EditQuiz, QuizDetail, CreateRoom.
 */
fun NavGraphBuilder.quizManageGraph(navController: NavHostController) {
    composable<Route.MyQuizzes> {
        // Danh sách "Quiz của tôi" — yêu cầu đăng nhập (cookie auth ở QuizApiService.getMyQuizzes).
        QuizManageListScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCreateQuiz = { navController.navigate(Route.CreateQuiz) },
            onNavigateToQuizDetail = { quizId ->
                navController.navigate(Route.QuizDetail(quizId))
            }
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
        // Sửa quiz. quizId lấy từ route argument qua SavedStateHandle ở
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
}
