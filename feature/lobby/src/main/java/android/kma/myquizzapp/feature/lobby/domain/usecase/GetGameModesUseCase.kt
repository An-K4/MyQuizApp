package android.kma.myquizzapp.feature.lobby.domain.usecase

import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Lấy đặc tả cấu hình của tất cả chế độ chơi (`GET /game-modes`).
 *
 * Đây là nguồn DUY NHẤT biết field nào `editable`, field nào `locked` và ràng
 * buộc min/max của từng field — `lobby:updated` và `GET /:code` chỉ trả giá trị
 * config, không trả quyền sửa. Không có endpoint nào gộp cả hai.
 *
 * Bản sao của use case cùng tên ở feature:quiz-manage là cố ý: quy ước kiến
 * trúc cấm feature phụ thuộc feature. Cả hai chỉ là wrapper mỏng một dòng quanh
 * repository ở core:common nên trùng lặp này rẻ hơn việc phá ranh giới module.
 */
class GetGameModesUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(): Result<List<GameModeDescriptor>> = repository.getGameModes()
}
