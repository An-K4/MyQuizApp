package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

sealed interface EditQuizEffect {
    /** Lưu thành công — Screen quay về màn Chi tiết (tự reload khi ON_RESUME). */
    data class QuizUpdated(val quizId: Long) : EditQuizEffect
}
