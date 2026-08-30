package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hiện thực kênh socket cho vai HOST.
 *
 * @Singleton nhưng giữ [GameSocketClient] riêng (client không phải singleton) nên
 * kết nối của host độc lập với của player.
 *
 * Các lệnh điều khiển trận (start/next/pause/resume/end) là emit không payload,
 * không ack — kết quả quay về qua broadcast (`game:started`, `question:started`...)
 * hoặc qua event `error`. Đã hiện thực sẵn ở N18 vì chúng đúng contract và chỉ một
 * dòng; UI console host sẽ dùng ở N20+.
 */
@Singleton
class HostGameSocketRepositoryImpl @Inject constructor(
    private val client: GameSocketClient
) : HostGameSocketRepository {

    override fun events(socketToken: String): Flow<GameEvent> = client.events(socketToken)

    override suspend fun joinLobby() = client.emit(GameSocketEvents.LOBBY_JOIN)

    override suspend fun disconnect() = client.disconnect()

    override suspend fun startGame() = client.emit(GameSocketEvents.GAME_START)

    override suspend fun nextQuestion() = client.emit(GameSocketEvents.GAME_NEXT)

    override suspend fun pauseGame() = client.emit(GameSocketEvents.GAME_PAUSE)

    override suspend fun resumeGame() = client.emit(GameSocketEvents.GAME_RESUME)

    override suspend fun endGame() = client.emit(GameSocketEvents.GAME_END)
}
