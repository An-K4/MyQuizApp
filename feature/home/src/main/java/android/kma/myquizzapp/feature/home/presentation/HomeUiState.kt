package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.isConfirmedGuest

/**
 * UI state for Home screen (MVI pattern).
 *
 * Home screen focuses on browsing sections of quizzes via scroll. "Của tôi"
 * (N13-14) không còn là tab của Home — nó đã chuyển thành tab "Thư viện" ở
 * bottom nav (N19.5).
 *
 * N19.6 — [session] thay cho `currentUser: User?` cũ. Lý do không phải để cho
 * đụng kiểu mới: `User?` không phân biệt được "chưa biết là ai" với "biết chắc
 * là khách", nên top bar buộc phải coi cả hai là khách và nháy nút
 * "Đăng ký/Đăng nhập" trong mấy trăm ms đầu với user đã đăng nhập.
 */
data class HomeUiState(
    val homeSections: List<HomeSection> = emptyList(),
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    val session: SessionState = SessionState.Unknown,
) {
    /**
     * Chỉ hiện lối đăng nhập ở top bar khi ĐÃ xác định là khách — không hiện
     * trong lúc còn [SessionState.Unknown].
     */
    val showSignInAction: Boolean get() = session.isConfirmedGuest

    /**
     * "Tiếp tục chơi" — ghim lên đầu trang, CỐ TÌNH ghi đè `position` của
     * backend.
     *
     * Đây là trạng thái riêng của người dùng (ván đang dở), không phải nội
     * dung khám phá, nên nó phải nằm sát ô nhập mã phòng ở trên thay vì xếp
     * lẫn vào giữa các section gợi ý. Backend đã lọc section rỗng nên khách
     * đơn giản là không nhận được section này — không cần empty state.
     */
    val continueSection: HomeSection? get() =
        homeSections.firstOrNull { it.sectionType == SECTION_TYPE_CONTINUE }

    /** Các section gợi ý, giữ đúng thứ tự `position` do backend sắp xếp. */
    val discoverySections: List<HomeSection> get() =
        homeSections.filterNot { it.sectionType == SECTION_TYPE_CONTINUE }
}

private const val SECTION_TYPE_CONTINUE = "continue"

/**
 * Các loại section có endpoint phân trang tương ứng ở backend, tức là xem tiếp
 * được thật:
 * - `trending`: `GET /quizzes/feed` (trang 1 của feed chính là section này)
 * - `category`: `GET /quizzes/feed?topic=...`
 * - `newest`: `GET /quizzes/search?sort=newest`
 *
 * `featured` KHÔNG có trong danh sách: nó lọc theo `is_featured` mà chưa endpoint
 * nào nhận tham số đó, nên nếu hiện nút thì bấm vào sẽ ra một danh sách KHÁC
 * với cái đang xem. `continue` cũng không: nó sắp theo `last_played_at` của
 * riêng user.
 */
private val PAGINABLE_SECTION_TYPES = setOf("trending", "category", "newest")

/**
 * Có hiện nút "Xem thêm" cho section này hay không.
 *
 * Nhận diện bằng `sectionType`, TUYỆT ĐỐI không bằng `title`: title nằm trong
 * bảng `home_sections` và người vận hành sửa được bằng SQL bất kỳ lúc nào.
 */
val HomeSection.hasSeeMore: Boolean
    get() = sectionType in PAGINABLE_SECTION_TYPES
