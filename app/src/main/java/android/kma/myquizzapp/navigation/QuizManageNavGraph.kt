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
fun NavGraphBuilder.quizManageGraph(
    navController: NavHostController,
    /**
     * Chốt gác đăng nhập do AppNavGraph sở hữu: (lời nhắn, việc cần làm nếu đã
     * đăng nhập). Graph không tự quyết định vì hộp thoại phải sống ngoài NavHost
     * — gác là chặn TRƯỚC khi điều hướng, không phải điều hướng rồi chặn.
     */
    requireAuth: (String, () -> Unit) -> Unit,
) {
    composable<Route.MyQuizzes> {
        // Tab "Thư viện" ở bottom nav (N19.5) — vẫn là danh sách "Quiz của tôi",
        // yêu cầu đăng nhập (cookie auth ở QuizApiService.getMyQuizzes).
        // Là tab cấp cao nhất nên KHÔNG truyền onNavigateBack → TopAppBar bỏ mũi tên back.
        // FAB trong màn đi thẳng vào editor (Route.CreateQuiz), chưa làm màn chọn
        // cách tạo như web — để dành tới khi có luồng import.
        QuizManageListScreen(
            onNavigateToCreateQuiz = {
                // Trên lý thuyết là dư (FAB chỉ tồn tại trong Thư viện, mà Thư viện
                // đã gác ở thanh nav) nhưng vẫn gác: editor chỉ POST khi bấm lưu,
                // nên nếu lọt qua thì người dùng mất cả bài vừa soạn lúc ăn 401.
                requireAuth("Đăng nhập để tạo quiz mới.") {
                    navController.navigate(Route.CreateQuiz)
                }
            },
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
                // Gác NGAY ở đây, không để tới màn cấu hình phòng: khách vẫn xem
                // được quiz detail từ Trang chủ, mà nếu để họ đặt tên phòng, chọn
                // chế độ xong mới chặn thì toàn bộ cấu hình vừa nhập sẽ mất.
                requireAuth("Đăng nhập để tạo phòng chơi từ quiz này.") {
                    navController.navigate(Route.CreateRoom(quizId))
                }
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
