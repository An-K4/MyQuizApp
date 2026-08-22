package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.model.Quiz

/**
 * UI state for Quiz Detail screen (MVI pattern).
 */
data class QuizDetailUiState(
    val quiz: Quiz? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
