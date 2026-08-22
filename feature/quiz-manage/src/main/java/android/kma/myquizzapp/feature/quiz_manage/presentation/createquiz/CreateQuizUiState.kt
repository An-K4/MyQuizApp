package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

data class CreateQuizUiState(
    val quizName: String = "",
    val quizDescription: String = "",
    val quizLanguage: String = "vi",
    val quizCategory: String = "",
    val isPublic: Boolean = true,
    val questions: List<QuestionDraft> = listOf(QuestionDraft()),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: List<String> = emptyList()
)
