package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.auth.domain.usecase.GetCurrentUserUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * CHI PHÍ MẠNG: GetCurrentUserUseCase gọi thẳng GET /users/me, KHÔNG có cache
 * (xem AuthRepositoryImpl.getCurrentUser). Vì vậy tuyệt đối không gọi [refresh]
 * theo mỗi lần đổi tab. Chỉ gọi ở các mốc dữ liệu thực sự có thể đổi:
 *   1. lần đầu dựng thanh nav,
 *   2. app quay lại foreground (ON_RESUME của Activity),
 *   3. vừa rời luồng auth (đăng nhập/đăng ký xong),
 *   4. vừa đăng xuất.
 * [refresh] tự bỏ qua lời gọi trùng khi request trước còn bay, nên các mốc trên
 * chồng nhau lúc khởi động cũng chỉ tốn 1 request.
 */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _avatarUrl = MutableStateFlow<String?>(null)

    /** null = chưa đăng nhập, chưa tải xong, hoặc user không có avatar. */
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private var inFlight: Job? = null

    fun refresh() {
        if (inFlight?.isActive == true) return
        inFlight = viewModelScope.launch {
            _avatarUrl.value = when (val result = getCurrentUserUseCase()) {
                is Result.Success -> result.data.avatar
                // Guest (401) hoặc mất mạng: về icon mặc định, KHÔNG báo lỗi — avatar
                // ở thanh nav là chi tiết trang trí, không đáng chặn UI.
                is Result.Error -> null
            }
        }
    }
}
