package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Giữ trạng thái đăng nhập của app ở MỘT chỗ duy nhất (N19.6).
 *
 * Khác [AuthRepository] thế nào: [AuthRepository] là tầng gọi API — mỗi method
 * là một request, không nhớ gì. [SessionRepository] là tầng trạng thái — chỉ
 * `@Singleton` này biết "hiện đang là ai", và mọi ViewModel đọc chung một
 * [StateFlow] thay vì tự gọi API rồi giữ bản sao riêng.
 *
 * Quy tắc bắt buộc: KHÔNG ViewModel nào được gọi thẳng
 * `AuthRepository.getCurrentUser()` để biết trạng thái đăng nhập nữa. Gọi thẳng
 * là tự tạo thêm một bản sao, và bản sao đó sẽ lệch ngay lần logout kế tiếp.
 */
interface SessionRepository {

    /**
     * Trạng thái hiện tại. Bắt đầu ở [SessionState.Unknown] cho tới lần
     * [refresh] đầu tiên hoàn tất. Mọi màn hình collect flow này.
     */
    val state: StateFlow<SessionState>

    /**
     * Đọc lại `GET /users/me` và cập nhật [state].
     *
     * Tự gộp lời gọi trùng: nhiều nơi gọi cùng lúc (dựng thanh nav + Home +
     * Hồ sơ lúc khởi động) chỉ tốn 1 request, các nơi còn lại chờ chung kết
     * quả. Vì vậy nơi gọi không cần tự chống trùng nữa.
     *
     * Chỉ gọi ở các mốc dữ liệu thực sự có thể đổi: mở app, app trở lại
     * foreground, vừa xong luồng auth. Đừng gọi theo mỗi lần đổi tab —
     * endpoint này không có cache.
     */
    suspend fun refresh()

    /**
     * Ghi [state] = đã đăng nhập ngay sau khi login/register/Google thành công,
     * dùng luôn [User] mà API vừa trả về — không phát sinh request thứ hai.
     */
    fun onAuthenticated(user: User)

    /**
     * Ghi [state] = [SessionState.Guest] ngay khi logout. Hàm này đồng bộ và
     * phải chạy kể cả khi API logout lỗi: trạng thái "đã đăng xuất" ở client là
     * điều quan trọng nhất, và UI phải đổi trong cùng frame để không loé lên
     * thông tin của người vừa rời máy.
     */
    fun onSignedOut()
}
