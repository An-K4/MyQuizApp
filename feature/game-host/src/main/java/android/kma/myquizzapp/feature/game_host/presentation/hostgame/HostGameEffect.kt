package android.kma.myquizzapp.feature.game_host.presentation.hostgame

/**
 * Việc một lần mà ViewModel yêu cầu tầng navigation làm.
 *
 * N21 chỉ có đường ra: màn kết quả cuối trận thuộc N24. Khi trận xong, màn này
 * tự hiện bảng xếp hạng cuối (payload `game:ended` đã mang sẵn) rồi để host tự bấm
 * thoát, thay vì điều hướng tới một màn chưa tồn tại.
 */
sealed interface HostGameEffect {

    /** Đóng màn điều khiển và quay về trước đó, kèm thông báo nếu có. */
    data class ExitGame(val message: String? = null) : HostGameEffect
}
