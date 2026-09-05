package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.model.AuthState
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import android.kma.myquizzapp.core.datastore.usecase.CheckAuthStateUseCase
import javax.inject.Inject

/**
 * Vào phòng bằng mã — gồm toàn bộ phần "ta là ai" ở một chỗ.
 *
 * Ba việc được gom vào đây vì chúng luôn đi cùng nhau và không phải việc của UI:
 *
 * 1. Xác định đã đăng nhập hay là khách ([CheckAuthStateUseCase]).
 * 2. Người đã đăng nhập: KHÔNG gửi gì trong body. Backend tự dựng player từ
 *    `req.user` (fullname + avatar) và bỏ qua body — có gửi tên lên cũng bị ghi đè.
 * 3. Khách: bắt buộc có nickname + uuid thiết bị lấy từ [GuestIdentityStore]
 *    (sinh lưỡi ở đúng lần join đầu tiên, sau đó dùng lại mãi mãi).
 *
 * KHÔNG kiểm tra `allowGuests` ở đây: màn nhập mã đã tra phòng trước nên biết
 * sớm hơn và chặn trước khi hỏi tên; ngoài ra server vẫn chặn lần cuối bằng
 * GAME_GUESTS_NOT_ALLOWED nên không có lỗ hỏng.
 */
class JoinGameUseCase @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
    private val checkAuthState: CheckAuthStateUseCase,
    private val guestIdentityStore: GuestIdentityStore
) {

    /**
     * @param sessionCode mã phòng người dùng nhập.
     * @param nickname chỉ dùng khi là khách. Người đã đăng nhập truyền null.
     */
    suspend operator fun invoke(
        sessionCode: String,
        nickname: String? = null
    ): Result<JoinRoomResult> = when (checkAuthState()) {
        AuthState.AUTHENTICATED -> gameSessionRepository.joinRoom(sessionCode)

        AuthState.GUEST -> {
            val guestName = requireNotNull(nickname?.trim()?.takeIf { it.isNotEmpty() }) {
                "Khách phải có nickname trước khi join — UI cần validate bằng NicknameValidator"
            }
            gameSessionRepository.joinRoom(
                sessionCode = sessionCode,
                playerName = guestName,
                guestId = guestIdentityStore.getOrCreateGuestId()
            )
        }
    }
}
