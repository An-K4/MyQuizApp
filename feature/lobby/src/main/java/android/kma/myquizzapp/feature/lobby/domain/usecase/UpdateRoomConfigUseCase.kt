package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Gửi patch cấu hình phòng qua socket (`lobby:config-update`).
 *
 * Patch RỖNG là trách nhiệm của người gọi: use case này không tự "thành công giả"
 * được vì [ConfigUpdateAck.config] là non-null, không có gì hợp lý để điền vào khi
 * chưa hỏi server. Biến một patch rỗng thành lợt gọi socket cũng sai: backend sẽ
 * trả `changed = false` và UI hiển thị "không có gì thay đổi" — tốn một vòng
 * mạng cho một câu trả lời ta đã biết trước. ViewModel chặn trước khi gọi.
 *
 * Không kiểm tra trạng thái phiên ở đây: use case không thấy `sessionStatus`.
 * Việc chặn sửa config sau khi trận đã bắt đầu nằm ở ViewModel (lớp chặn sừm)
 * và ở backend `writeConfig` (409 `GAME_LOBBY_ONLY`, lớp chặn thật).
 */
class UpdateRoomConfigUseCase @Inject constructor(
    private val socketRepository: HostGameSocketRepository
) {
    suspend operator fun invoke(
        patch: Map<GameConfigKey, GameConfigValue>
    ): Result<ConfigUpdateAck> {
        require(patch.isNotEmpty()) { "patch must not be empty" }
        return socketRepository.updateConfig(patch)
    }
}
