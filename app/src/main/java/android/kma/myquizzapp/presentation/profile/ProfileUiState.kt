package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull

/**
 * UI state cho màn Profile.
 *
 * N19.6 — state này không còn giữ bản sao [User] riêng nữa, chỉ mang
 * [SessionState] đọc từ nguồn chung. Đây chính là chỗ sửa bug "đăng xuất rồi
 * vào lại Hồ sơ vẫn thấy account cũ": trước đây `user` được ghi một lần trong
 * `init` của ViewModel, mà ViewModel không bị tạo lại khi đổi tab — nên bản sao
 * đó sống dừng dưng qua cả lần đăng xuất.
 *
 * [isLoading] là giá trị suy ra, không phải trường độc lập: "đang tải" và "chưa
 * biết là ai" là cùng một sự việc, giữ hai biến riêng thì sớm muộn cũng lệch
 * nhau.
 */
data class ProfileUiState(
    val session: SessionState = SessionState.Unknown,
) {
    val user: User? get() = session.userOrNull

    val isLoading: Boolean get() = session is SessionState.Unknown

    /** Đã xác định là khách — N19.6 lượt 2 sẽ dùng để hiện empty state đăng nhập. */
    val isConfirmedGuest: Boolean get() = session is SessionState.Guest
}
