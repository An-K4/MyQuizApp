package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.User

/**
 * UI state for Home screen (MVI pattern).
 *
 * Home screen focuses on browsing sections of quizzes via scroll. "Của tôi"
 * (N13-14) không còn là tab của Home — nó đã chuyển vào bên trong màn
 * Profile (mục "Xem quiz của tôi"), vì để nguyên như 1 tab-nav-trigger là
 * sai ngữ nghĩa UI (tab phải đổi content tại chỗ, không phải điều hướng đi
 * màn khác).
 *
 * [currentUser] dùng để quyết định hiện nút "Đăng nhập" (null — guest) hay
 * avatar (không null — đã đăng nhập) cạnh nút tìm kiếm.
 */
data class HomeUiState(
    val homeSections: List<HomeSection> = emptyList(),
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    val currentUser: User? = null
)
