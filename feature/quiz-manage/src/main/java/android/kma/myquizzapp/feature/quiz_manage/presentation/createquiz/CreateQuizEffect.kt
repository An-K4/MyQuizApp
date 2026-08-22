package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

sealed interface CreateQuizEffect {
    data class QuizCreated(val quizId: Long) : CreateQuizEffect
}
