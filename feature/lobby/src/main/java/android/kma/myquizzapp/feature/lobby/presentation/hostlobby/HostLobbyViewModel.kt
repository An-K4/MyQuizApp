package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.lobby.domain.usecase.RefreshHostTokenUseCase
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
 * ViewModel màn lobby của HOST — chứa toàn bộ luồng realtime đầu tiên của app.
 *
 * Ba quyết định quan trọng:
 *
 * 1. `lobby:join` được gọi sau MỌI lần [GameEvent.Connected], không phải một lần
 *    trong init. Socket.io tự reconnect nhưng KHÔNG tự join lại room — sau khi mạng
 *    trở lại, nếu không join lại thì socket vẫn "connected" mà không bao giờ nhận
 *    được `lobby:updated` nữa. Đây là cái bẫy chính của toàn bộ việc N18.
 *
 * 2. Phân biệt lỗi tạm thời và lỗi chết. Với 4 code trong [FATAL_CODES], kết nối
 *    lại bằng cùng token sẽ fail y nguyên, nên phải điều hướng ra ngoài thay vì
 *    quay vòng vô tận.
 *
 * 3. Token socket có TTL riêng, ngắn hơn phiên đăng nhập. Khi app bị treo lâu ở
 *    background rồi quay lại, token cũ có thể đã hết hạn — thứ nhận được sẽ là
 *    GAME_TOKEN_INVALID. Khi đó thử lấy token mới qua REST ĐÚNG MỘT LẦN
 *    ([tokenRefreshAttempted]); nếu vẫn fail thì thoát, tránh vòng lặp refresh.
 */
@HiltViewModel
class HostLobbyViewModel @Inject constructor(
    private val socketRepository: HostGameSocketRepository,
    private val refreshHostToken: RefreshHostTokenUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val sessionCode: String = checkNotNull(savedStateHandle["sessionCode"])

    /** Token đang dùng — có thể bị thay khi refresh, nên không phải val. */
    private var socketToken: String = checkNotNull(savedStateHandle["socketToken"])

    private var eventJob: Job? = null
    private var tokenRefreshAttempted = false

    private val _uiState = MutableStateFlow(HostLobbyUiState(sessionCode = sessionCode))
    val uiState: StateFlow<HostLobbyUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HostLobbyEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        connect()
    }

    fun onIntent(intent: HostLobbyIntent) {
        when (intent) {
            HostLobbyIntent.Retry -> connect()
            HostLobbyIntent.LeaveRoom -> leaveRoom()
            HostLobbyIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    /**
     * Mở (hoặc mở lại) kết nối.
     *
     * Hủy [eventJob] cũ trước khi tạo job mới: việc hủy kích hoạt awaitClose trong
     * GameSocketClient nên socket cũ đóng hẳn, không để hai socket cùng sống và
     * cùng bắn event vào một UI.
     */
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
                // Join lại sau mọi lần connect — xem ghi chú (1) ở đầu class.
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
                // Server chủ động đá: socket.io sẽ không tự reconnect, chờ là vô vọng.
                DisconnectReason.SERVER_DISCONNECT ->
                    exit("Máy chủ đã đóng kết nối tới phòng này.")
                // Do chính ta gọi disconnect() — không báo gì thêm.
                DisconnectReason.CLIENT -> Unit
                // Mất mạng: giữ nguyên dự liệu cũ, để socket.io tự thử lại.
                DisconnectReason.TRANSPORT ->
                    _uiState.update { it.copy(connection = ConnectionStatus.RECONNECTING) }
            }

            is GameEvent.Failed -> onFailure(event)

            // Các event của giai đoạn chơi (question:*, leaderboard:*...) chưa dùng ở
            // N18. Bỏ qua có ý thức, đã có log ở tầng client.
            is GameEvent.Unhandled -> Unit
        }
    }

    private suspend fun onFailure(event: GameEvent.Failed) {
        val message = AppError.Api(event.code).toUserMessage()
        when {
            event.code == CODE_TOKEN_INVALID && !tokenRefreshAttempted -> {
                tokenRefreshAttempted = true
                when (val result = refreshHostToken(gameId)) {
                    is Result.Success -> {
                        socketToken = result.data
                        connect()
                    }
                    is Result.Error -> exit(result.error.toUserMessage())
                }
            }

            event.code in FATAL_CODES -> exit(message)

            else -> _uiState.update { it.copy(errorMessage = message) }
        }
    }

    private fun leaveRoom() {
        viewModelScope.launch {
            exit(message = null)
        }
    }

    /** Đóng socket và yêu cầu tầng navigation rời khỏi màn này. */
    private suspend fun exit(message: String?) {
        socketRepository.disconnect()
        eventJob?.cancel()
        _effect.send(HostLobbyEffect.ExitLobby(message))
    }

    private companion object {
        const val CODE_TOKEN_INVALID = "GAME_TOKEN_INVALID"

        /**
         * Code mà việc thử lại chắc chắn vô ích: token thuộc phòng khác, phòng không
         * còn tồn tại, hoặc bản ghi người chơi đã bị xóa. GAME_TOKEN_INVALID KHÔNG
         * nằm ở đây vì còn cơ hội làm mới token một lần.
         */
        val FATAL_CODES = setOf(
            "GAME_TOKEN_INVALID",
            "GAME_TOKEN_WRONG_ROOM",
            "GAME_ROOM_NOT_FOUND",
            "GAME_PLAYER_NOT_FOUND"
        )
    }
}
