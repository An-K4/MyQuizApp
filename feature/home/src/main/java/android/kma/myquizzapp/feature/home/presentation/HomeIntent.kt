package android.kma.myquizzapp.feature.home.presentation

/**
 * User intents for Home screen (MVI pattern).
 *
 * Home screen focuses on browsing sections of quizzes via scroll. Search
 * functionality is in a separate SearchScreen. "Của tôi" đã chuyển hẳn vào
 * màn Profile (không còn liên quan tới Home nữa) — xem feature:quiz-manage
 * QuizManageListScreen và app-level ProfileScreen.
 */
sealed interface HomeIntent {
    /** Load home content (sections) */
    data object LoadHome : HomeIntent

    /** User clicked search icon → navigate to SearchScreen */
    data object NavigateToSearch : HomeIntent

    /** User clicked a quiz card */
    data class QuizCardClicked(val quizId: Long) : HomeIntent

    /** Retry after error */
    data object Retry : HomeIntent
}

// N19.6: đã xóa `CheckAuthState`. Nó tồn tại chỉ vì Home phải tự đi hỏi lại
// `GET /users/me` mỗi lần resume để bắt kịp việc người dùng đăng nhập/đăng xuất
// ở màn khác. Giờ trạng thái phiên được đẩy tức thời qua SessionRepository, nên
// một intent "đi hỏi lại" vừa dư vừa là một request mạng mỗi lần đổi tab.
