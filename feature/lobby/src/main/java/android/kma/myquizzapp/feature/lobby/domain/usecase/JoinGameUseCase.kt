package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import javax.inject.Inject

/**
 * Vào phòng bằng mã — gồm toàn bộ phần "ta là ai" ở một chỗ.
 *
 * Ba việc được gom vào đây vì chúng luôn đi cùng nhau và không phải việc của UI:
 *
 * 1. Xác định đã đăng nhập hay là khách ([SessionRepository]).
 * 2. Người đã đăng nhập: KHÔNG gửi gì trong body. Backend tự dựng player từ
 *    `req.user` (fullname + avatar) và bỏ qua body — có gửi tên lên cũng bị ghi đè.
 * 3. Khách: bắt buộc có nickname + uuid thiết bị lấy từ [GuestIdentityStore]
 *    (sinh lần đầu ở đúng lần join đầu tiên, sau đó dùng lại mãi mãi).
 *
 * KHÔNG kiểm tra `allowGuests` ở đây: thế nhập mã đã tra phòng trước nên biết
 * sớm hơn và chặn trước khi hỏi tên; ngoài ra server vẫn chặn lần cuối bằng
 * GAME_GUESTS_NOT_ALLOWED nên không có lỗ hỏng.
 *
 * N19.6: chuyển từ `CheckAuthStateUseCase` sang [SessionRepository]. Cái cũ gọi
 * `GET /users/me` MỎI LẦN được gọi và còn ghi cờ guest vào DataStore; cái mới
 * đọc trạng thái đã có trong bộ nhớ và chỉ gọi mạng khi thật sự chưa biết.
 */
class JoinGameUseCase @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
    private val session: SessionRepository,
    private val guestIdentityStore: GuestIdentityStore
) {

    /**
     * @param sessionCode mã phòng người dùng nhập.
     * @param nickname chỉ dùng khi là khách. Người đã đăng nhập truyền null.
     */
    suspend operator fun invoke(
        sessionCode: String,
        nickname: String? = null
    ): Result<JoinRoomResult> = when (resolveSession()) {
        is SessionState.LoggedIn -> gameSessionRepository.joinRoom(sessionCode)

        is SessionState.Guest -> {
            val guestName = requireNotNull(nickname?.trim()?.takeIf { it.isNotEmpty() }) {
                "Khách phải có nickname trước khi join — UI cần validate bằng NicknameValidator"
            }
            gameSessionRepository.joinRoom(
                sessionCode = sessionCode,
                playerName = guestName,
                guestId = guestIdentityStore.getOrCreateGuestId()
            )
        }

        // Không đoán là khách ở đây: join sai tư cách sẽ tạo một bản ghi player rác
        // mang tên sai trong phòng, không đảo lại được. Để UI báo lỗi mạng cho người
        // dùng thử lại. Trên thực tế nhánh này gần như không xảy ra vì thế nhập mã
        // đã chốt phiên trước đó rồi.
        is SessionState.Unknown -> Result.Error(AppError.Network)
    }

    private suspend fun resolveSession(): SessionState {
        val current = session.state.value
        if (current !is SessionState.Unknown) return current
        session.refresh()
        return session.state.value
    }
}
