package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Tra phòng theo mã trước khi join.
 *
 * Vì sao tách hẳn một bước tra cứu thay vì join luôn rồi đọc lỗi: nếu phòng
 * không cho guest vào, ta cần biết ĐIỀU ĐÓ TRƯỚC khi hỏi người dùng nhập tên —
 * bắt họ nhập tên rồi mới báo "phòng không nhận khách" là trải nghiệm tệ.
 * Endpoint này public và không tạo dữ liệu nên gọi trước là vô hại.
 */
class LookupRoomUseCase @Inject constructor(
    private val gameSessionRepository: GameSessionRepository
) {
    suspend operator fun invoke(sessionCode: String): Result<RoomLookup> =
        gameSessionRepository.lookupRoom(sessionCode)
}
