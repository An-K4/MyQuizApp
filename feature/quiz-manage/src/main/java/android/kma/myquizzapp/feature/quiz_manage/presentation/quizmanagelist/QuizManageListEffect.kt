package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

sealed interface QuizManageListEffect {
    data class ShowMessage(val message: String) : QuizManageListEffect
}
