package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
import android.net.Uri

/**
 * UI state cho màn Sửa quiz (N16).
 */
data class EditQuizUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val quizName: String = "",
    val quizDescription: String = "",
    val quizLanguage: String = "vi",
    val quizCategory: String = "",
    val isPublic: Boolean = true,
    // URL ảnh cover đã lưu trên server (null nếu quiz không có cover).
    val existingCoverUrl: String? = null,
    // Ảnh cover MỚI chọn (local, chưa upload) — chỉ upload thật lúc bấm "Lưu thay đổi".
    val coverImageUri: Uri? = null,
    // User xóa cover hiện có mà chưa chọn ảnh mới. Backend PATCH không thể clear field
    // (field vắng mặt = giữ nguyên), nên cover cũ vẫn được gửi lại — xem ghi chú ở submit().
    val coverRemoved: Boolean = false,
    val questions: List<QuestionDraft> = emptyList(),
    val isSubmitting: Boolean = false,
    // true khi có thay đổi chưa lưu — dùng cho dialog xác nhận thoát (pattern EditQuizPage web).
    val isDirty: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: List<String> = emptyList()
)
