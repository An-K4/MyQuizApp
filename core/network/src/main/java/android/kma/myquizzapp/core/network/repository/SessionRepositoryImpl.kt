package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hiện thực [SessionRepository] — tầng trạng thái đứng trên [AuthRepository].
 *
 * Bắt buộc `@Singleton`: nếu Hilt dụng nhiều instance thì có nhiều [StateFlow],
 * và ta quay về đúng cái bệnh nhiều bản sao mà N19.6 đang đi sửa.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
) : SessionRepository {

    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * Scope riêng của repository, KHÔNG dùng scope của người gọi.
     *
     * Lý do: request được chia sẻ cho nhiều người chờ. Nếu nó chạy trong
     * `viewModelScope` của người gọi đầu tiên thì chỉ cần ViewModel đó bị hủy
     * (đổi tab, xoay máy) là cả nhóm chờ chung bị hủy theo, rồi state mắc
     * lại ở [SessionState.Unknown] vĩnh viễn. Singleton này sống bằng đời
     * app nên scope không cần cancel.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val inFlightLock = Mutex()
    private var inFlight: Deferred<Unit>? = null

    override suspend fun refresh() {
        val job = inFlightLock.withLock {
            inFlight?.takeIf { it.isActive }
                ?: scope.async { fetchAndApply() }.also { inFlight = it }
        }
        try {
            job.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // fetchAndApply() đã tự nuốt lỗi qua Result nên tới đây là chuyện
            // bất thường thật. Không để nó nổ lên UI: biết "là ai" là việc phụ,
            // không đáng làm sập màn hình người dùng đang xem.
            Timber.e(e, "SessionRepository.refresh() thất bại ngoài dự kiến")
        }
    }

    private suspend fun fetchAndApply() {
        when (val result = authRepository.getCurrentUser()) {
            is Result.Success -> _state.value = SessionState.LoggedIn(result.data)
            is Result.Error -> when (result.error) {
                // Chỉ 401 mới được kết luận "chưa đăng nhập".
                is AppError.Unauthorized -> _state.value = SessionState.Guest

                // ⚠️ MẤT MẠNG KHÔNG PHẢI LÀ ĐĂNG XUẤT. Nếu ở đây ghi
                // [SessionState.Guest] thì chỉ cần rất ủ mạng một nhịp lúc mở app
                // là người đã đăng nhập bị hạ xuống guest, mất avatar, và bị hộp
                // thoại đăng nhập chặn ở Thư viện dù phiên vẫn còn sống. Giữ
                // nguyên state cũ và để lần [refresh] sau phán quyết.
                else -> Timber.w(
                    "Không xác định được phiên (${result.error}) — giữ nguyên ${_state.value}"
                )
            }
        }
    }

    override fun onAuthenticated(user: User) {
        _state.value = SessionState.LoggedIn(user)
    }

    override fun onSignedOut() {
        _state.value = SessionState.Guest
    }
}
