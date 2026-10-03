package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode

/** Kết quả tạm thời chuyển từ socket gameplay sang màn Final Result. */
data class StoredGameResult(
    val gameId: Long,
    val playerId: Long,
    val result: GameEnded,
    val mode: GameMode?,
    /** Chỉ giữ trong RAM để gọi REST review; không đưa token vào route, Bundle hay log. */
    val socketToken: String
)

/**
 * Store theo vòng đời process; tránh nhét leaderboard lớn vào navigation Bundle.
 * Nếu process bị kill, màn kết quả phải hiện fallback thay vì tự dựng dữ liệu giả.
 */
interface GameResultRepository {
    fun save(value: StoredGameResult)
    fun get(gameId: Long): StoredGameResult?
    fun clear(gameId: Long)
}
