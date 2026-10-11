package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility

/** Only the identity/title needed for the hard-delete confirmation. */
data class QuizDeleteTarget(val id: Long, val title: String)

/** Paging data is exposed separately; filters and mutation state belong to this state. */
data class QuizManageListUiState(
    val visibility: MyQuizzesVisibility = MyQuizzesVisibility.ALL,
    val sort: MyQuizzesSort = MyQuizzesSort.RECENTLY_UPDATED,
    val keyword: String = "",
    val deleteTarget: QuizDeleteTarget? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
)
