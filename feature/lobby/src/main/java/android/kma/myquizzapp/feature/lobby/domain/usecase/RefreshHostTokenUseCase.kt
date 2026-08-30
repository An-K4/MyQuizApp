package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Lấy lại socket token của host cho một phiên đã tồn tại.
 *
 * Dùng khi socket báo GAME_TOKEN_INVALID — tính huống thường gặp nhất là app nằm
 * lâu ở background, token socket (TTL ngắn) hết hạn trong khi cookie đăng nhập
 * vẫn còn sống. Gọi lại endpoint host-token là đủ để vào lại phòng, không cần
 * tạo phòng mới.
 *
 * Lưu ý: đây KHÔNG phải lệnh tạo phòng — endpoint này idempotent, gọi nhiều lần
 * chỉ sinh token mới cho cùng phiên.
 */
class RefreshHostTokenUseCase @Inject constructor(
    private val gameSessionRepository: GameSessionRepository
) {
    suspend operator fun invoke(gameId: Long): Result<String> =
        gameSessionRepository.getHostToken(gameId)
}
