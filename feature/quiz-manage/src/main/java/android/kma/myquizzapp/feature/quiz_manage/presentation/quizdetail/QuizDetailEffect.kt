package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

sealed interface QuizDetailEffect {
    /**
     * Quiz đã xóa xong (N16) — màn detail không còn gì để hiển thị, Screen
     * điều hướng ra ngoài (popBackStack về danh sách, list tự refresh khi resume).
     */
    data object QuizDeleted : QuizDetailEffect
}
