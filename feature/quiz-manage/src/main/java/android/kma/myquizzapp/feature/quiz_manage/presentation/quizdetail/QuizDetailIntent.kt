package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

/**
 * User intents for Quiz Detail screen (MVI pattern).
 *
 * Navigation (back, tạo phòng, sửa quiz) được xử lý qua callback truyền trực tiếp
 * vào QuizDetailScreen, không đi qua intent - giống pattern onNavigateToXxx của HomeScreen.
 */
sealed interface QuizDetailIntent {
    /** Load/reload quiz detail */
    data object LoadQuizDetail : QuizDetailIntent

    /** Retry sau khi lỗi */
    data object Retry : QuizDetailIntent

    // ---- N16: xóa quiz (hard delete) — 3 intent điều khiển dialog xác nhận ----
    /** Mở dialog xác nhận xóa */
    data object DeleteQuizClicked : QuizDetailIntent

    /** Xác nhận xóa trong dialog */
    data object DeleteQuizConfirmed : QuizDetailIntent

    /** Đóng dialog không xóa (bị chặn khi đang xóa dở) */
    data object DeleteQuizDismissed : QuizDetailIntent
}
