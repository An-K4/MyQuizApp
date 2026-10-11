package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.validator.QuizEditorErrors
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

/** State creation is called only by Screen/Preview wrappers, not the stateless editor. */
@Composable
fun rememberQuestionScrollStates(questions: List<QuestionDraft>): Map<String, ScrollState> =
    questions.associate { question ->
        question.localId to key(question.localId) { rememberScrollState() }
    }

/** Scroll to the first invalid lazy item. Indices follow QuizEditorContent's fixed header. */
@Composable
fun ScrollToEditorError(errors: QuizEditorErrors, attempt: Int, questions: List<QuestionDraft>, listState: LazyListState) {
    LaunchedEffect(errors, attempt) {
        val target = when {
            errors.name != null -> 1
            errors.description != null -> 2
            errors.language != null || errors.category != null -> 3
            errors.general.isNotEmpty() -> 5
            errors.questions.isNotEmpty() -> {
                val index = questions.indexOfFirst { it.localId in errors.questions }
                if (index < 0) null else 6 + index
            }
            else -> null
        }
        if (target != null) listState.animateScrollToItem(target)
    }
}
