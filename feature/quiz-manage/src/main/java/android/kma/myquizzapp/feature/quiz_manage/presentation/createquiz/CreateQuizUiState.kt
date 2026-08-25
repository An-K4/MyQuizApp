package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
import android.net.Uri

data class CreateQuizUiState(
    val quizName: String = "",
    val quizDescription: String = "",
    val quizLanguage: String = "vi",
    val quizCategory: String = "",
    val isPublic: Boolean = true,
    // Ảnh cover đã chọn (local, chưa upload) — N15. Chỉ upload thật lúc bấm "Tạo quiz".
    val coverImageUri: Uri? = null,
    val questions: List<QuestionDraft> = listOf(QuestionDraft()),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: List<String> = emptyList()
)
