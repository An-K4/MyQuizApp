package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.onSuccess
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/*
 * N19.6 — Mọi luồng làm đổi danh tính (login, register, Google, logout) ĐỀU phải
 * ghi vào [SessionRepository] ngay tại đây.
 *
 * Vì sao đặt ở tầng use-case chứ không ở ViewModel: nếu để ViewModel tự nhờo
 * `session.onAuthenticated(...)` sau khi login thành công thì chỉ cần thêm một
 * màn đăng nhập mới (VD Google ở màn khác) mà quên dòng đó là trạng thái lệch
 * trở lại — và sẽ lệch Êm, không crash, rất khó thấy. Ở tầng này thì không ai
 * qua được cửa.
 */

class LoginUseCase @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(email: String, password: String) =
        repo.login(email, password).onSuccess(session::onAuthenticated)
}

class RegisterUseCase @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionRepository,
) {
    // auto-login đã nằm TRONG AuthRepositoryImpl.register — UseCase không cần biết,
    // chỉ cần biết User trả về ở đây đã là user đã có cookie hợp lệ.
    suspend operator fun invoke(email: String, password: String, fullname: String, phone: String?) =
        repo.register(email, password, fullname, phone).onSuccess(session::onAuthenticated)
}

class LoginWithGoogleUseCase @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(idToken: String) =
        repo.loginWithGoogle(idToken).onSuccess(session::onAuthenticated)
}

class LogoutUseCase @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = repo.logout()
        // Gọi KỂ CẢ khi API lỗi, và đặt NGOÀI nhánh thành công cho rõ ý:
        // AuthRepositoryImpl.logout() đã xóa cookie cục bộ bất kể kết quả mạng, nên
        // cookie đã mất mà state vẫn LoggedIn thì UI đang nói dối người dùng.
        session.onSignedOut()
        return result
    }
}

/**
 * Đọc trạng thái đăng nhập — cách DUY NHẤT để một màn hình biết "đang là ai".
 *
 * Trả [StateFlow] nên màn hình collect một lần là tự đổi theo login/logout ở bất
 * kỳ đâu, không cần gọi lại mỗi lần ON_RESUME như trước N19.6.
 */
class ObserveSessionUseCase @Inject constructor(
    private val session: SessionRepository,
) {
    operator fun invoke(): StateFlow<SessionState> = session.state
}

/**
 * Đọc lại phiên từ backend. Chỉ gọi ở các mốc dữ liệu thực sự có thể đổi (mở
 * app, app trở lại foreground). Tự gộp lời gọi trùng ở tầng repository.
 */
class RefreshSessionUseCase @Inject constructor(
    private val session: SessionRepository,
) {
    suspend operator fun invoke() = session.refresh()
}

// ĐÃ XÓA `GetCurrentUserUseCase` (N19.6). Nó trả một Result một-lần, và chính vì
// vậy mỗi nơi gọi đều phải tự giữ một bản sao user trong UiState riêng — 3 bản
// sao (Home, Hồ sơ, thanh nav) và không bản nào biết khi bản khác đổi. Đó là gốc
// của bug "đăng xuất rồi vào lại Hồ sơ vẫn thấy account cũ". Dùng
// [ObserveSessionUseCase] (đọc) + [RefreshSessionUseCase] (làm mới) thay thế.
