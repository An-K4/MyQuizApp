package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.ui.gameconfig.RoomConfigForm
import android.kma.myquizzapp.core.ui.gameconfig.baselineFor
import android.kma.myquizzapp.core.ui.gameconfig.buildGameConfigPatch
import android.kma.myquizzapp.feature.lobby.domain.usecase.GetGameModesUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.LookupRoomUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.RefreshHostTokenUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.UpdateRoomConfigUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
 * Ba quyết định quan trọng về kết nối:
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
 *
 * Ba quyết định về việc sửa cấu hình phòng (N20):
 *
 * 4. Descriptor (editable/locked) được tải LAZY, chỉ khi host mở bảng sửa, và cần
 *    HAI request song song: `GET /:code` cho biết phòng thuộc mode nào, `GET
 *    /game-modes` cho biết mode đó cho sửa những gì. Không có endpoint nào trả
 *    cả hai, và `lobby:updated` chỉ trả giá trị config chứ không trả quyền sửa.
 *    Tải lazy vì phần lớn phiên host không bấm vào đây.
 *
 * 5. Form KHÔNG bị dựng lại mỗi lần `lobby:updated` khi bảng đang mở. Event này
 *    bắn mỗi lần có người vào/ra phòng; dựng lại form sẽ xóa sạch những gì host
 *    đang gõ giữa chừng.
 *
 * 6. Sau khi lưu, form được dựng lại từ [ConfigUpdateAck.config] chứ không giữ giá
 *    trị host vừa chọn. `normalizeConfig` ở backend có thể sửa cả field host không
 *    chạm tới (bật reviewMode tự bật showCorrectAnswer, marathon bị ép
 *    autoAdvance...) nên UI lạc quan sẽ hiển thị sai so với phòng thật.
 */
@HiltViewModel
class HostLobbyViewModel @Inject constructor(
    private val socketRepository: HostGameSocketRepository,
    private val refreshHostToken: RefreshHostTokenUseCase,
    private val lookupRoom: LookupRoomUseCase,
    private val getGameModes: GetGameModesUseCase,
    private val updateRoomConfig: UpdateRoomConfigUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val sessionCode: String = checkNotNull(savedStateHandle["sessionCode"])

    /** Token đang dùng — có thể bị thay khi refresh, nên không phải val. */
    private var socketToken: String = checkNotNull(savedStateHandle["socketToken"])

    private var eventJob: Job? = null
    private var specJob: Job? = null
    private var startTimeoutJob: Job? = null
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
            HostLobbyIntent.OpenConfigSheet -> openConfigSheet()
            HostLobbyIntent.DismissConfigSheet -> dismissConfigSheet()
            HostLobbyIntent.RetryLoadSpec -> loadModeSpec()
            is HostLobbyIntent.ToggleChanged -> updateBoolean(intent.key, intent.checked)
            is HostLobbyIntent.NumberChanged -> updateNumber(intent.key, intent.value)
            is HostLobbyIntent.ChoiceChanged -> updateChoice(intent.key, intent.value)
            HostLobbyIntent.SaveConfig -> saveConfig()
            HostLobbyIntent.StartGame -> startGame()
            HostLobbyIntent.ConfigNoticeShown -> _uiState.update { it.copy(configNotice = null) }
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

            is GameEvent.LobbyUpdated -> onLobbyUpdated(event)

            is GameEvent.Disconnected -> when (event.reason) {
                // Server chủ động đá: socket.io sẽ không tự reconnect, chờ là vô vọng.
                DisconnectReason.SERVER_DISCONNECT ->
                    exit("Máy chủ đã đóng kết nối tới phòng này.")
                // Do chính ta gọi disconnect() — không báo gì thêm.
                DisconnectReason.CLIENT -> Unit
                // Mất mạng: giữ nguyên dữ liệu cũ, để socket.io tự thử lại.
                DisconnectReason.TRANSPORT ->
                    _uiState.update { it.copy(connection = ConnectionStatus.RECONNECTING) }
            }

            is GameEvent.Failed -> onFailure(event)

            // Xác nhận trận đã bắt đầu. `game:start` KHÔNG có ack nên đây là tín hiệu
            // thành công DUY NHẤT: việc điều hướng phải neo vào event này, không
            // phải vào lúc bấm nút. Event cũng đến ngay cả khi trận do máy khác
            // của cùng host bắt đầu, nên không kiểm tra [HostLobbyUiState.isStarting].
            is GameEvent.GameStarted -> onGameStarted()

            // Các event của giai đoạn chơi (question:*, leaderboard:*...) chưa dùng ở
            // N18. Bỏ qua có ý thức, đã có log ở tầng client.
            // Dùng `else` thay vì liệt kê từng nhánh: màn lobby sẽ không bao giờ xử
            // lý event gameplay, nên mỗi lần GameEvent mọc thêm nhánh mới không có
            // lý do gì để file này vỡ build.
            else -> Unit
        }
    }

    /**
     * Áp snapshot lobby mới.
     *
     * Form chỉ được dựng lại khi bảng sửa ĐANG ĐÓNG — xem ghi chú (5) ở đầu class.
     * Ngoài ra nếu trận vừa bắt đầu thì đóng bảng lại: từ lúc đó mọi patch đều bị
     * backend trả 409 `GAME_LOBBY_ONLY`.
     */
    private fun onLobbyUpdated(event: GameEvent.LobbyUpdated) {
        _uiState.update { state ->
            val config = event.lobby.config
            val stillInLobby = event.lobby.sessionStatus == android.kma.myquizzapp.core.common.model.SessionStatus.LOBBY
            val keepSheetOpen = state.isConfigSheetOpen && stillInLobby
            state.copy(
                players = event.lobby.players,
                sessionStatus = event.lobby.sessionStatus,
                connection = ConnectionStatus.CONNECTED,
                config = config,
                isConfigSheetOpen = keepSheetOpen,
                configForm = if (state.isConfigSheetOpen) {
                    state.configForm
                } else {
                    state.descriptor?.let { RoomConfigForm.fromConfig(it, config) }
                },
                invalidConfigKeys = if (state.isConfigSheetOpen) state.invalidConfigKeys else emptySet()
            )
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

            // Dọn cả hai cờ chờ: một `error` có thể là câu trả lời cho
            // `lobby:config-update` hoặc cho `game:start` (VD GAME_ALREADY_STARTED),
            // và cờ nào đang tắt thì gán false cũng vô hại.
            else -> {
                startTimeoutJob?.cancel()
                _uiState.update {
                    it.copy(
                        isSavingConfig = false,
                        isStarting = false,
                        isConfigSheetOpen = false,
                        errorMessage = message
                    )
                }
            }
        }
    }

    private fun openConfigSheet() {
        val state = _uiState.value
        if (!state.canEditConfig) {
            _uiState.update { it.copy(configNotice = MESSAGE_LOBBY_ONLY) }
            return
        }
        _uiState.update { it.copy(isConfigSheetOpen = true, configNotice = null) }
        if (state.descriptor == null) loadModeSpec() else rebuildFormFromConfig()
    }

    private fun dismissConfigSheet() {
        _uiState.update {
            it.copy(
                isConfigSheetOpen = false,
                invalidConfigKeys = emptySet(),
                // Bỏ mọi thay đổi chưa lưu: lần mở sau phải bắt đầu từ config thật.
                configForm = it.descriptor?.let { descriptor ->
                    it.config?.let { config -> RoomConfigForm.fromConfig(descriptor, config) }
                }
            )
        }
    }

    /**
     * Tải mode của phòng và đặc tả cấu hình của mode đó — song song.
     *
     * Hai request độc lập nhau nên chạy tuần tự là tự gánh thêm một vòng RTT ngay
     * trước mắt người dùng đang chờ bảng mở ra.
     */
    private fun loadModeSpec() {
        if (specJob?.isActive == true) return
        specJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSpec = true, errorMessage = null) }
            val (lookupResult, modesResult) = coroutineScope {
                val lookup = async { lookupRoom(sessionCode) }
                val modes = async { getGameModes() }
                lookup.await() to modes.await()
            }

            if (lookupResult is Result.Error) {
                failSpecLoad(lookupResult.error.toUserMessage())
                return@launch
            }
            if (modesResult is Result.Error) {
                failSpecLoad(modesResult.error.toUserMessage())
                return@launch
            }

            val mode = (lookupResult as Result.Success).data.mode
            val descriptor = (modesResult as Result.Success).data.firstOrNull { it.mode == mode }
            if (descriptor == null) {
                failSpecLoad("Máy chủ không mô tả cấu hình cho chế độ chơi của phòng này.")
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoadingSpec = false,
                    mode = mode,
                    descriptor = descriptor,
                    configForm = state.config?.let { RoomConfigForm.fromConfig(descriptor, it) },
                    invalidConfigKeys = emptySet()
                )
            }
        }
    }

    private fun failSpecLoad(message: String) {
        _uiState.update { it.copy(isLoadingSpec = false, errorMessage = message) }
    }

    private fun rebuildFormFromConfig() {
        _uiState.update { state ->
            val descriptor = state.descriptor ?: return@update state
            val config = state.config ?: return@update state
            state.copy(
                configForm = RoomConfigForm.fromConfig(descriptor, config),
                invalidConfigKeys = emptySet()
            )
        }
    }

    private fun updateBoolean(key: GameConfigKey, value: Boolean) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                configForm = it.configForm?.updateBoolean(key, value),
                invalidConfigKeys = it.invalidConfigKeys - key
            )
        }
    }

    private fun updateNumber(key: GameConfigKey, value: String) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                // Lọc ngay tại đây: bàn phím số trên Android vẫn cho dán chứ và dấu.
                configForm = it.configForm?.updateNumber(
                    key,
                    value.filter { char -> char.isDigit() }.take(MAX_NUMBER_LENGTH)
                ),
                invalidConfigKeys = it.invalidConfigKeys - key
            )
        }
    }

    private fun updateChoice(key: GameConfigKey, value: String) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                configForm = it.configForm?.updateChoice(key, value),
                invalidConfigKeys = it.invalidConfigKeys - key
            )
        }
    }

    /**
     * Chỉ nhận thay đổi cho field mà mode này cho sửa.
     *
     * `locked` thắng `editable`: gửi field locked lên thì backend bỏ qua và trả lại
     * trong `ignored`, nên chặn sớm ở đây để UI không hiển thị một giá trị không
     * bao giờ được áp.
     */
    private fun canEdit(key: GameConfigKey): Boolean {
        val state = _uiState.value
        return state.isConfigFormEnabled && key in state.descriptor?.editable.orEmpty()
    }

    /**
     * Lưu cấu hình.
     *
     * Quy ước về việc đóng bảng: bảng chỉ ở lại khi form còn field sai định dạng —
     * đó là lỗi host phải sửa ngay tại chỗ. Mọi trường hợp khác (không có gì để
     * lưu, lưu xong, hoặc lỗi khi gọi máy chủ) đều đóng bảng và nói kết quả bằng
     * snackbar: giữ một bảng đã hết việc chỉ khiến host phải bấm Đóng thêm lần nữa.
     */
    private fun saveConfig() {
        val state = _uiState.value
        if (state.isSavingConfig) return
        if (!state.canEditConfig) {
            _uiState.update { it.copy(isConfigSheetOpen = false, configNotice = MESSAGE_LOBBY_ONLY) }
            return
        }
        val descriptor = state.descriptor ?: return
        val config = state.config ?: return
        val form = state.configForm ?: return

        val invalidKeys = form.invalidKeys(descriptor)
        if (invalidKeys.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    invalidConfigKeys = invalidKeys,
                    configNotice = "Hãy kiểm tra lại các thiết lập đang được đánh dấu."
                )
            }
            return
        }

        // So với config THẬT của phòng, không phải default của mode — nếu so với
        // default thì việc host đổi một field về đúng giá trị default sẽ bị lọc mất.
        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = form.values(),
            baseline = config.baselineFor(descriptor)
        )
        if (patch.isEmpty()) {
            _uiState.update {
                it.copy(
                    isConfigSheetOpen = false,
                    invalidConfigKeys = emptySet(),
                    configNotice = "Chưa có thay đổi nào để lưu."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingConfig = true, errorMessage = null, configNotice = null) }
            when (val result = updateRoomConfig(patch)) {
                is Result.Success -> applyAck(result.data, descriptor)
                is Result.Error -> _uiState.update {
                    it.copy(
                        isSavingConfig = false,
                        isConfigSheetOpen = false,
                        errorMessage = result.error.toUserMessage()
                    )
                }
            }
        }
    }

    /** Dựng lại form từ config sau normalize — xem ghi chú (6) ở đầu class. */
    private fun applyAck(ack: ConfigUpdateAck, descriptor: GameModeDescriptor) {
        _uiState.update {
            it.copy(
                isSavingConfig = false,
                isConfigSheetOpen = false,
                config = ack.config,
                configForm = RoomConfigForm.fromConfig(descriptor, ack.config),
                invalidConfigKeys = emptySet(),
                configNotice = noticeFor(ack)
            )
        }
    }

    private fun noticeFor(ack: ConfigUpdateAck): String = when {
        // changed = false: backend đã bỏ sạch patch và KHÔNG broadcast lobby:updated.
        !ack.changed && ack.ignored.isEmpty() -> "Máy chủ không ghi nhận thay đổi nào."
        ack.ignored.isNotEmpty() ->
            "Đã lưu. Máy chủ bỏ qua ${ack.ignored.size} thiết lập không áp được cho chế độ này."
        else -> "Đã lưu cấu hình phòng."
    }

    /**
     * Gửi `game:start` rồi chờ `game:started`.
     *
     * Hai điểm đáng chú ý:
     *
     * - Không coi "hàm trả về" là thành công. Backend không ack lệnh này, nên nếu
     *   không đặt hạn chờ thì nút sẽ quay mãi mãi khi gói tin rơi mất.
     * - Nút "Bắt đầu" khi phòng chưa có ai vẫn được phép (host tự chọn), phần cảnh
     *   báo nằm ở UI — ViewModel không chặn để khỏi đóng cả đường cho mode luyện tập.
     */
    private fun startGame() {
        if (!_uiState.value.canStartGame) return
        _uiState.update {
            it.copy(
                isStarting = true,
                isConfigSheetOpen = false,
                errorMessage = null,
                configNotice = null
            )
        }
        viewModelScope.launch { socketRepository.startGame() }
        startTimeoutJob?.cancel()
        startTimeoutJob = viewModelScope.launch {
            delay(START_TIMEOUT_MS)
            if (_uiState.value.isStarting) {
                _uiState.update {
                    it.copy(
                        isStarting = false,
                        errorMessage = "Máy chủ chưa xác nhận trận bắt đầu, hãy thử lại."
                    )
                }
            }
        }
    }

    private suspend fun onGameStarted() {
        startTimeoutJob?.cancel()
        _uiState.update { it.copy(isStarting = false, isConfigSheetOpen = false) }
        _effect.send(
            HostLobbyEffect.NavigateToHostGame(gameId = gameId, socketToken = socketToken)
        )
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

        /** Độ dài tối đa của ô số: max lớn nhất trong đặc tả là 7200 (4 chữ số). */
        const val MAX_NUMBER_LENGTH = 4

        /**
         * Hạn chờ `game:started` sau khi bấm Bắt đầu.
         *
         * Bằng ACK_TIMEOUT_MS của socket client (5 giây) cho đứng nhất quán: cùng
         * một đường truyền, không có lý do để hai lệnh có hai mức kiên nhẫn.
         */
        const val START_TIMEOUT_MS = 5_000L

        const val MESSAGE_LOBBY_ONLY = "Trận đã bắt đầu, không thể sửa cấu hình nữa."

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
