package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.core.common.model.QuestionType

sealed interface CreateQuizIntent {
    data class QuizNameChanged(val value: String) : CreateQuizIntent
    data class QuizDescriptionChanged(val value: String) : CreateQuizIntent
    data class QuizLanguageChanged(val value: String) : CreateQuizIntent
    data class QuizCategoryChanged(val value: String) : CreateQuizIntent
    data class IsPublicChanged(val value: Boolean) : CreateQuizIntent

    data object AddQuestion : CreateQuizIntent
    data class RemoveQuestion(val localId: String) : CreateQuizIntent
    data class QuestionTypeChanged(val localId: String, val type: QuestionType) : CreateQuizIntent
    data class QuestionTextChanged(val localId: String, val value: String) : CreateQuizIntent
    data class TimeLimitChanged(val localId: String, val value: Int) : CreateQuizIntent
    data class HintChanged(val localId: String, val value: String) : CreateQuizIntent
    data class ExplanationChanged(val localId: String, val value: String) : CreateQuizIntent

    data class OptionChanged(val localId: String, val index: Int, val value: String) : CreateQuizIntent
    data class AddOption(val localId: String) : CreateQuizIntent
    data class RemoveOption(val localId: String, val index: Int) : CreateQuizIntent
    data class ToggleCorrectIndex(val localId: String, val index: Int) : CreateQuizIntent
    data class CorrectTextChanged(val localId: String, val value: String) : CreateQuizIntent

    data object Submit : CreateQuizIntent
    data object ErrorShown : CreateQuizIntent
}
