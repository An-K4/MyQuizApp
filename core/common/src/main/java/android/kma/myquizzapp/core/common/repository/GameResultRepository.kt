package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.GameEnded

/** Kết quả tạm thời chuyển từ socket gameplay sang màn Final Result. */
data class StoredGameResult(
    val gameId: Long,
    val playerId: Long,
    val result: GameEnded
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
