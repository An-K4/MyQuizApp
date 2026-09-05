package android.kma.myquizzapp.feature.lobby.presentation.joinroom

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.AuthState
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.usecase.CheckAuthStateUseCase
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
 * ViewModel màn nhập mã phòng.
 *
 * Luồng cố tình chia hai bước — TRA CỨU rồi mới JOIN:
 *
 * `GET /games/{code}` là endpoint public và không tạo dữ liệu, nên dùng nó để trả
 * lời ba câu hỏi trước khi động vào dữ liệu: mã có đúng không, phòng còn nhận
 * người không, và phòng có cho khách vào không. Nếu join thẳng rồi đọc lỗi, ta sẽ
 * bắt khách nhập tên xong mới báo "phòng không nhận khách" — trải nghiệm tệ,
 * và còn có nguy cơ để lại bản ghi player rác.
 *
 * Phân loại lỗi: sai mã / phòng đầy / trận đã bắt đầu là lỗi CỦA Ô NHẬP — gắn
 * ngay dưới ô để người dùng sửa mã; mất mạng / lỗi server là sự cố nhất thời —
 * bắn snackbar để họ thử lại.
 */
@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val lookupRoom: LookupRoomUseCase,
    private val joinGame: JoinGameUseCase,
    private val checkAuthState: CheckAuthStateUseCase
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
                    sessionCode = intent.value.filterNot { ch -> ch.isWhitespace() }.uppercase(),
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
        if (code.isEmpty() || _uiState.value.isSubmitting) return

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
            else -> when (checkAuthState()) {
                AuthState.AUTHENTICATED -> joinAsAccount(code, room)

                AuthState.GUEST -> if (room.allowGuests) {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(JoinRoomEffect.NavigateToGuestNickname(code))
                } else {
                    // Chặn ngay tại client, KHÔNG gọi join để ăn lỗi 403: người dùng
                    // biết ngay và có đường đi tiếp (đăng nhập) thay vì ngõ cụt.
                    _uiState.update { it.copy(isSubmitting = false, guestBlocked = true) }
                }
            }
        }
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

/** 404 ở màn này chỉ có một nghĩa duy nhất: gõ sai mã. Nói thẳng cho dễ hiểu. */
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
