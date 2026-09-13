package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerGameSocketRepositoryImpl @Inject constructor(
    private val client: GameSocketClient,
    private val ackMapper: SocketAckMapper
) : PlayerGameSocketRepository {
    override fun events(socketToken: String): Flow<GameEvent> = client.events(socketToken)
    override suspend fun joinLobby() = client.emit(GameSocketEvents.LOBBY_JOIN)
    override suspend fun disconnect() = client.disconnect()
    override suspend fun leaveLobby() = client.emit(GameSocketEvents.LOBBY_LEAVE)
    override suspend fun requestNextQuestion() = client.emit(GameSocketEvents.QUESTION_NEXT)
    override suspend fun sync() = client.emit(GameSocketEvents.PLAYER_SYNC)

    override suspend fun submitAnswer(answer: PlayerAnswer) = ackMapper.toAnswerAck(
        client.emitWithAck(
            event = GameSocketEvents.QUESTION_ANSWER,
            payload = JSONObject().put("answer", answer.toWireValue())
        )
    )

    private fun PlayerAnswer.toWireValue(): Any = when (this) {
        is PlayerAnswer.SingleChoice -> optionId
        is PlayerAnswer.MultipleSelect -> JSONArray(optionIds)
        is PlayerAnswer.Text -> value
    }
}
