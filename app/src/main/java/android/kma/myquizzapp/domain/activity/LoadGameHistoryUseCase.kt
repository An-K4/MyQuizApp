package android.kma.myquizzapp.domain.activity

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.PageInfo
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import javax.inject.Inject

/**
 * Nạp một trang lịch sử theo đúng danh tính hiện tại.
 *
 * Guest chưa từng join chưa có UUID: trả trang rỗng ngay, không tạo định danh chỉ
 * vì người dùng mở tab Hoạt động. User đăng nhập không gửi guest id vì cookie luôn
 * phải thắng ở backend.
 */
class LoadGameHistoryUseCase @Inject constructor(
    private val repository: GameSessionRepository,
    private val guestIdentityStore: GuestIdentityStore
) {
    suspend operator fun invoke(
        role: GameHistoryRole,
        cursor: String?,
        session: SessionState,
        limit: Int = 20
    ): Result<List<GameHistoryItem>> {
        val guestId = when (session) {
            is SessionState.LoggedIn -> null
            SessionState.Guest -> guestIdentityStore.getGuestIdOrNull()
                ?: return Result.Success(emptyList(), PageInfo(nextCursor = null, hasMore = false))
            SessionState.Unknown -> return Result.Error(
                AppError.Unknown(IllegalStateException("Session is not resolved"))
            )
        }
        return repository.getGameHistory(
            role = role,
            cursor = cursor,
            limit = limit,
            guestId = guestId
        )
    }
}
