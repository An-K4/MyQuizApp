package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.kma.myquizzapp.core.common.model.QuestionType
import android.net.Uri

/**
 * User intents cho màn Sửa quiz (N16) — mirror CreateQuizIntent, thêm nhóm load.
 * Navigation (back, thoát khi chưa lưu) xử lý ở Screen qua callback + dialog.
 */
sealed interface EditQuizIntent {
    data object LoadQuiz : EditQuizIntent
    data object Retry : EditQuizIntent

    data class QuizNameChanged(val value: String) : EditQuizIntent
    data class QuizDescriptionChanged(val value: String) : EditQuizIntent
    data class QuizLanguageChanged(val value: String) : EditQuizIntent
    data class QuizCategoryChanged(val value: String) : EditQuizIntent
    data class IsPublicChanged(val value: Boolean) : EditQuizIntent

    data class PickCoverImage(val uri: Uri) : EditQuizIntent
    data object RemoveCoverImage : EditQuizIntent

    data object AddQuestion : EditQuizIntent
    data class RemoveQuestion(val localId: String) : EditQuizIntent
    data class PickQuestionImage(val localId: String, val uri: Uri) : EditQuizIntent
    data class RemoveQuestionImage(val localId: String) : EditQuizIntent
    data class QuestionTypeChanged(val localId: String, val type: QuestionType) : EditQuizIntent
    data class QuestionTextChanged(val localId: String, val value: String) : EditQuizIntent
    data class TimeLimitChanged(val localId: String, val value: String) : EditQuizIntent
    data class HintChanged(val localId: String, val value: String) : EditQuizIntent
    data class ExplanationChanged(val localId: String, val value: String) : EditQuizIntent

    data class OptionChanged(val localId: String, val index: Int, val value: String) : EditQuizIntent
    data class AddOption(val localId: String) : EditQuizIntent
    data class RemoveOption(val localId: String, val index: Int) : EditQuizIntent
    data class ToggleCorrectIndex(val localId: String, val index: Int) : EditQuizIntent
    data class CorrectTextChanged(val localId: String, val value: String) : EditQuizIntent

    data object Submit : EditQuizIntent
    data object ErrorShown : EditQuizIntent
}
