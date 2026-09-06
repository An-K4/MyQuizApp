package android.kma.myquizzapp.core.common.model

/**
 * Trạng thái phiên đăng nhập — NGUỒN DUY NHẤT cho toàn app (N19.6).
 *
 * Trước N19.6 mỗi màn tự gọi `GetCurrentUserUseCase()` một lần trong `init`
 * rồi giữ bản sao riêng trong UiState của mình. Hệ quả là sau khi đăng xuất,
 * màn Hồ sơ vào lại vẫn hiện account cũ: ViewModel không bị tạo lại nên `init`
 * không chạy lần hai, và không có ai thông báo cho nó rằng phiên đã mất. Sửa
 * kiểu "gọi lại API mỗi lần ON_RESUME" chỉ che được triệu chứng; gốc vấn đề là
 * có N bản sao trạng thái, không có bản nào là chuẩn.
 *
 * Vì sao có [Unknown] tách khỏi [Guest]: hai cái này nhìn giống nhau (đều
 * "không có user") nhưng phải xử lý khác nhau. [Unknown] = chưa hỏi backend lần
 * nào, tuyệt đối KHÔNG được dùng để chặn người dùng — nếu gác đăng nhập mà coi
 * [Unknown] như chưa đăng nhập thì user đã login sẽ bị đá ra màn Login trong
 * mấy trăm ms đầu sau khi mở app. [Guest] = đã hỏi và backend trả 401, lúc đó
 * mới được chặn.
 *
 * Đây là cách DUY NHẤT để biết "đã đăng nhập chưa". Enum `AuthState` cũ (2 giá
 * trị, không phân biệt được "chưa biết", không mang theo [User]) cùng
 * `CheckAuthStateUseCase` đã hết chỗ dùng sau lượt 3 của N19.6 — đọc phiên luôn
 * qua `SessionRepository` (hoặc `ObserveSessionUseCase` ở tầng UI), ĐỪNG dựng
 * lại một nguồn sự thật thứ hai.
 */
sealed interface SessionState {

    /** Chưa gọi `GET /users/me` lần nào. KHÔNG phải "chưa đăng nhập". */
    data object Unknown : SessionState

    /** Đã xác định là không có phiên hợp lệ (backend trả 401, hoặc vừa logout). */
    data object Guest : SessionState

    /** Đã xác định có phiên hợp lệ, kèm thông tin user mới nhất. */
    data class LoggedIn(val user: User) : SessionState
}

/** User hiện tại, hoặc null nếu là [SessionState.Unknown]/[SessionState.Guest]. */
val SessionState.userOrNull: User?
    get() = (this as? SessionState.LoggedIn)?.user

/**
 * Chỉ `true` khi CHẮC CHẮN đã đăng nhập. Dùng cho việc hiển thị (avatar, tên).
 * Đừng dùng riêng cái này để quyết định chặn/không chặn — xem [isConfirmedGuest].
 */
val SessionState.isLoggedIn: Boolean
    get() = this is SessionState.LoggedIn

/**
 * Chỉ `true` khi ĐÃ HỎI backend và biết chắc là chưa đăng nhập. Đây mới là điều
 * kiện đúng để bật hộp thoại yêu cầu đăng nhập; [SessionState.Unknown] phải chờ
 * chứ không được chặn.
 */
val SessionState.isConfirmedGuest: Boolean
    get() = this is SessionState.Guest
