package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility

/**
 * UI state cho danh sách "Quiz của tôi" (N13-14).
 *
 * Dữ liệu list thật được expose riêng qua Flow<PagingData<QuizSummary>> ở
 * ViewModel (Paging 3 không hợp để nhét chung vào 1 StateFlow data class),
 * state này chỉ giữ các filter/sort để dẫn xuất MyQuizzesParams.
 */
data class QuizManageListUiState(
    val visibility: MyQuizzesVisibility = MyQuizzesVisibility.ALL,
    val sort: MyQuizzesSort = MyQuizzesSort.RECENTLY_UPDATED,
    val keyword: String = ""
)
