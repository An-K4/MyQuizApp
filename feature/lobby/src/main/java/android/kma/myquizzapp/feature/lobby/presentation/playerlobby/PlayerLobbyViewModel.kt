package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel phòng chờ của NGƯỜI CHƠI.
 *
 * Giống HostLobby ở xương sống — và cố tình giống để hai màn cùng hành vi khi
 * mạng chập chờn: `lobby:join` được gọi lại sau MỌI [GameEvent.Connected] (socket.io
 * tự reconnect nhưng KHÔNG tự vào lại room), và huỷ job cũ trước khi mở kết nối
 * mới để không có hai socket cùng sống.
 *
 * Khác một điểm quan trọng: KHÔNG có cơ chế làm mới token. Backend chỉ có
 * `POST /games/{id}/host-token` cho chủ phòng; người chơi muốn có token mới thì
 * phải join lại bằng mã phòng. Vì vậy GAME_TOKEN_INVALID ở đây là lỗi chết: thoát
 * ra kèm thông báo thay vì quay vòng thử lại vô ích.
 *
 * Khi rời phòng chủ động phải gửi `lobby:leave` TRƯỚC khi disconnect — disconnect
 * suông chỉ làm server đánh dấu mất kết nối (vẫn chờm chỗ trong danh sách), còn
 * `lobby:leave` mới là rời hẳn để các máy khác thấy biến mất ngay.
 */
@HiltViewModel
class PlayerLobbyViewModel @Inject constructor(
    private val socketRepository: PlayerGameSocketRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playerId: Long = checkNotNull(savedStateHandle["playerId"])
    private val socketToken: String = checkNotNull(savedStateHandle["socketToken"])

    private var eventJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerLobbyUiState(myPlayerId = playerId))
    val uiState: StateFlow<PlayerLobbyUiState> = _uiState.asStateFlow()

    private val _effect = Channel<PlayerLobbyEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        connect()
    }

    fun onIntent(intent: PlayerLobbyIntent) {
        when (intent) {
            PlayerLobbyIntent.Retry -> connect()
            PlayerLobbyIntent.LeaveRoom -> leaveRoom()
            PlayerLobbyIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun connect() {
        eventJob?.cancel()
        _uiState.update {
            it.copy(
                connection = if (it.hasLobbySnapshot) {
                    ConnectionStatus.RECONNECTING
                } else {
                    ConnectionStatus.CONNECTING
                },
                errorMessage = null
            )
        }
        eventJob = viewModelScope.launch {
            socketRepository.events(socketToken).collect { event -> onEvent(event) }
        }
    }

    private suspend fun onEvent(event: GameEvent) {
        when (event) {
            GameEvent.Connected -> {
                _uiState.update { it.copy(connection = ConnectionStatus.CONNECTED) }
                socketRepository.joinLobby()
            }

            is GameEvent.LobbyUpdated -> _uiState.update {
                it.copy(
                    players = event.lobby.players,
                    sessionStatus = event.lobby.sessionStatus,
                    connection = ConnectionStatus.CONNECTED
                )
            }

            is GameEvent.Disconnected -> when (event.reason) {
                DisconnectReason.SERVER_DISCONNECT ->
                    exit("Máy chủ đã đóng kết nối tới phòng này.")
                DisconnectReason.CLIENT -> Unit
                DisconnectReason.TRANSPORT ->
                    _uiState.update { it.copy(connection = ConnectionStatus.RECONNECTING) }
            }

            is GameEvent.Failed -> onFailure(event)

            // game:started và các event chơi sẽ được xử lý từ N21 (màn chơi). N19 chỉ
            // dừng ở phòng chờ nên bỏ qua có ý thức, đã có log ở tầng client.
            is GameEvent.Unhandled -> Unit
        }
    }

    private suspend fun onFailure(event: GameEvent.Failed) {
        val message = AppError.Api(event.code).toUserMessage()
        if (event.code in FATAL_CODES) exit(message)
        else _uiState.update { it.copy(errorMessage = message) }
    }

    private fun leaveRoom() {
        viewModelScope.launch {
            // Chỉ báo rời phòng khi đang thực sự kết nối; đang mất mạng mà gọi thì
            // lệnh rơi vào hư vô, để server tự dọn theo timeout.
            if (_uiState.value.connection == ConnectionStatus.CONNECTED) {
                socketRepository.leaveLobby()
            }
            exit(message = null)
        }
    }

    private suspend fun exit(message: String?) {
        socketRepository.disconnect()
        eventJob?.cancel()
        _effect.send(PlayerLobbyEffect.ExitLobby(message))
    }

    private companion object {
        /**
         * Những code mà thử lại chắc chắn vô ích. Khác HostLobby, GAME_TOKEN_INVALID
         * nằm trong danh sách này vì người chơi không có endpoint làm mới token.
         */
        val FATAL_CODES = setOf(
            "GAME_TOKEN_INVALID",
            "GAME_TOKEN_WRONG_ROOM",
            "GAME_ROOM_NOT_FOUND",
            "GAME_PLAYER_NOT_FOUND"
        )
    }
}
