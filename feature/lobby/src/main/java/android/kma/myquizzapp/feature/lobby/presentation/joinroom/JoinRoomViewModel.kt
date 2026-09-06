package android.kma.myquizzapp.feature.lobby.presentation.joinroom

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.lobby.domain.usecase.JoinGameUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.LookupRoomUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel của thế nhập mã phòng ([JoinRoomCard], trước N19.6 là một màn riêng).
 *
 * Luồng cố tình chia hai bước — TRA CỨU rồi mới JOIN:
 *
 * `GET /games/{code}` là endpoint public và không tạo dự liệu, nên dùng nó để trả
 * lời ba câu hỏi trước khi động vào dự liệu: mã có đúng không, phòng còn nhận
 * người không, và phòng có cho khách vào không. Nếu join thắng rồi đọc lỗi, ta sẽ
 * bắt khách nhập tên xong mới báo "phòng không nhận khách" — trải nghiệm tệ,
 * và còn có nguy cơ để lại bản ghi player rác.
 *
 * Phân loại lỗi: sai mã / phòng đầy / trận đã bắt đầu là lỗi CỦA Ô NHẬP — gắn
 * ngay dưới ô để người dùng sửa mã; mất mạng / lỗi server là sự cố nhất thời —
 * để ở `errorMessage` cho thế hiện riêng.
 *
 * N19.6: trạng thái đăng nhập đọc từ [SessionRepository] thay cho
 * `CheckAuthStateUseCase` cũ — xem [resolveSession].
 */
@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val lookupRoom: LookupRoomUseCase,
    private val joinGame: JoinGameUseCase,
    private val session: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    private val _effect = Channel<JoinRoomEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: JoinRoomIntent) {
        when (intent) {
            is JoinRoomIntent.CodeChanged -> _uiState.update {
                // Chuẩn hóa ngay khi gõ: mã phòng luôn viết hoa và không có khoảng trắng,
                // tránh người dùng dán mã kèm space rồi nhận "không tìm thấy phòng".
                it.copy(
                    sessionCode = intent.value
                        .filter { ch -> ch.isLetterOrDigit() }
                        .uppercase()
                        .take(SESSION_CODE_LENGTH),
                    codeError = null
                )
            }

            JoinRoomIntent.Submit -> submit()

            JoinRoomIntent.GuestBlockedDismissed ->
                _uiState.update { it.copy(guestBlocked = false) }

            JoinRoomIntent.GuestBlockedLoginClicked -> {
                _uiState.update { it.copy(guestBlocked = false) }
                viewModelScope.launch { _effect.send(JoinRoomEffect.NavigateToLogin) }
            }

            JoinRoomIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submit() {
        val code = _uiState.value.sessionCode.trim()
        if (code.length != SESSION_CODE_LENGTH || _uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, codeError = null, errorMessage = null) }
            when (val result = lookupRoom(code)) {
                is Result.Success -> onRoomFound(code, result.data)
                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, codeError = result.error.toLookupMessage())
                }
            }
        }
    }

    private suspend fun onRoomFound(code: String, room: RoomLookup) {
        when {
            !room.isOpenForJoin -> failCode(room.closedReason())
            room.isFull -> failCode("Phòng đã đủ ${room.maxPlayers} người")
            else -> when (resolveSession()) {
                is SessionState.LoggedIn -> joinAsAccount(code, room)

                is SessionState.Guest -> if (room.allowGuests) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(JoinRoomEffect.NavigateToGuestNickname(code))
                } else {
                    // Chặn ngay tại client, KHÔNG gọi join để ăn lỗi 403: người dùng
                    // biết ngay và có đường đi tiếp (đăng nhập) thay vì ngõ cụt.
                    _uiState.update { it.copy(isSubmitting = false, guestBlocked = true) }
                }

                is SessionState.Unknown -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Chưa xác định được bạn đang đăng nhập hay vào với " +
                            "tư cách khách. Kiểm tra kết nối rồi thử lại."
                    )
                }
            }
        }
    }

    /**
     * Trả về trạng thái phiên ĐÃ CHỐT để phân luồng.
     *
     * Khác với chốt gác đăng nhập ở AppNavGraph (cho đi qua khi chưa biết), ở đây
     * KHÔNG được phép đoán: hai nhánh dẫn tới hai nơi khác nhau (vào thẳng phòng
     * chờ hay sang màn nhập tên), đoán sai là hỏi tên một người đã đăng nhập rồi
     * bỏ tên đó đi (server lấy fullname từ tài khoản).
     *
     * Nên khi chưa biết thì đợi một lần `refresh()`. Đợi ở đây không tốn thêm lượt
     * mạng trong đa số trường hợp: app đã gọi `refresh()` lúc mở, và repository tự
     * gộp các lời gọi trùng thành một request.
     */
    private suspend fun resolveSession(): SessionState {
        val current = session.state.value
        if (current !is SessionState.Unknown) return current
        session.refresh()
        return session.state.value
    }

    private suspend fun joinAsAccount(code: String, room: RoomLookup) {
        when (val result = joinGame(sessionCode = code)) {
            is Result.Success -> {
                _uiState.update { it.copy(isSubmitting = false) }
                _effect.send(
                    JoinRoomEffect.NavigateToPlayerLobby(
                        gameId = room.gameId,
                        playerId = result.data.player.id,
                        socketToken = result.data.socketToken
                    )
                )
            }

            is Result.Error -> _uiState.update {
                it.copy(isSubmitting = false, errorMessage = result.error.toUserMessage())
            }
        }
    }

    private fun failCode(message: String) {
        _uiState.update { it.copy(isSubmitting = false, codeError = message) }
    }
}

/** 404 ở ô nhập mã chỉ có một nghĩa duy nhất: gõ sai mã. Nói thắng cho dễ hiểu. */
private fun AppError.toLookupMessage(): String = when {
    this is AppError.NotFound -> "Không tìm thấy phòng với mã này"
    this is AppError.Api && code == "GAME_ROOM_NOT_FOUND" -> "Không tìm thấy phòng với mã này"
    else -> toUserMessage()
}

private fun RoomLookup.closedReason(): String = when (status) {
    SessionStatus.ACTIVE, SessionStatus.PAUSED ->
        "Trận đã bắt đầu và phòng không cho vào muộn"
    SessionStatus.FINISHED -> "Trận này đã kết thúc"
    SessionStatus.CANCELLED -> "Phòng này đã bị hủy"
    SessionStatus.LOBBY -> "Phòng không nhận thêm người chơi"
}
