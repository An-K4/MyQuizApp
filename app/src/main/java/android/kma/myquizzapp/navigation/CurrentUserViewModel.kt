package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.feature.auth.domain.usecase.ObserveSessionUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.RefreshSessionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Giữ avatar của user hiện tại cho bottom nav (tab "Hồ sơ" hiển thị avatar thay
 * cho icon tĩnh — N19.5).
 *
 * Vì sao phải có ViewModel riêng thay vì dùng state của Home/Profile: bottom bar
 * sống ở Scaffold NGOÀI NavHost nên không thuộc back stack entry nào, không thể
 * lấy ViewModel scope theo destination. ViewModel này được scope theo Activity
 * (hiltViewModel() gọi trong AppNavGraph) nên sống đúng bằng vòng đời thanh nav.
 *
 * N19.6 — không còn tự gọi API và tự giữ bản sao user nữa: avatar giờ là một
 * phép chiếu (`map`) từ [ObserveSessionUseCase]. Hệ quả trực tiếp: đăng xuất ở
 * màn Hồ sơ là avatar ở thanh nav biến mất trong cùng frame, không cần ai nhắc
 * nó đi đọc lại.
 *
 * [refresh] giữ nguyên tên và ý nghĩa (AppNavGraph đang gọi ở lúc dụng thanh nav
 * và khi app trở lại foreground), nhưng giờ chỉ ủy quyền cho repository — nơi
 * duy nhất biết có cần gọi mạng hay không, và tự gộp các lời gọi trùng.
 */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val refreshSession: RefreshSessionUseCase,
) : ViewModel() {

    /**
     * Trạng thái phiên cho các chốt gác đăng nhập ở AppNavGraph (N19.6).
     *
     * Đặt ở chính ViewModel này vì nó đã là chỗ duy nhất trong phạm vi Activity
     * quan sát phiên; thêm một ViewModel riêng để gác chỉ tạo thêm một chỗ
     * collect nữa mà không thêm thông tin gì.
     */
    val session: StateFlow<SessionState> = observeSession()

    /** null = chưa đăng nhập, chưa xác định, hoặc user không có avatar. */
    val avatarUrl: StateFlow<String?> = session
        .map { it.userOrNull?.avatar }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun refresh() {
        viewModelScope.launch { refreshSession() }
    }
}
