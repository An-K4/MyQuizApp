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

    /**
     * Re-check login state (gọi lại getCurrentUser). Dùng khi Home resume
     * (ví dụ: quay lại từ màn Đăng nhập hoặc màn Profile sau khi đăng xuất)
     * vì AuthRepository chỉ expose suspend fun một lần, không có Flow phản
     * ứng theo thời gian thực.
     */
    data object CheckAuthState : HomeIntent
}
