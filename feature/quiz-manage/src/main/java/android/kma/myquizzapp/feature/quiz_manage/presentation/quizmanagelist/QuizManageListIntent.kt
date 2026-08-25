package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility

sealed interface QuizManageListIntent {
    data class VisibilityChanged(val visibility: MyQuizzesVisibility) : QuizManageListIntent
    data class SortChanged(val sort: MyQuizzesSort) : QuizManageListIntent
    data class KeywordChanged(val keyword: String) : QuizManageListIntent

    /** N16: yêu cầu tạo Pager mới + load lại từ mạng (Screen gọi khi ON_RESUME). */
    data object Refresh : QuizManageListIntent
}
