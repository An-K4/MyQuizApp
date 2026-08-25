package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.model.Quiz

/**
 * UI state for Quiz Detail screen (MVI pattern).
 */
data class QuizDetailUiState(
    val quiz: Quiz? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // N16: true khi user hiện tại là chủ quiz — mới hiện nút Chỉnh sửa/Xóa.
    // Guest xem quiz public: getCurrentUser lỗi 401 → isOwner = false.
    val isOwner: Boolean = false,
    // N16: trạng thái dialog xóa quiz.
    val isConfirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
    // Lỗi xóa hiện NGAY trong dialog (dialog giữ mở) — pattern QuizDetailPage web.
    val deleteError: String? = null
)
