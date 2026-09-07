package android.kma.myquizzapp.core.common.model

/**
 * Kết quả ack của `lobby:config-update` (host sửa cấu hình phòng trong lobby).
 *
 * Shape lấy từ code backend thật (`game.socket.ts::onConfigUpdate`):
 * `ack({ ok: true, changed, config: session.config, ignored })`.
 *
 * Ba điểm phải nhớ vì chúng quyết định cách UI phản ứng:
 *
 * 1. [config] là **CẤU HÌNH ĐẦY ĐỦ sau normalize**, không phải các field vừa gửi.
 *    `normalizeConfig` ở backend có thể sửa cả field host KHÔNG chạm tới (bật
 *    `reviewMode` sẽ tự bật `showCorrectAnswer`, `perQuestionSeconds = 0` tự tắt
 *    `speedBonus`, marathon bị ép `autoAdvance = true`...). Vì vậy form phải
 *    render lại từ [config] chứ không giữ giá trị optimistic của chính mình.
 *
 * 2. [changed] = false nghĩa là patch bị bỏ sạch — khi đó backend KHÔNG broadcast
 *    `lobby:updated`. Đừng ngồi chờ event, hãy dùng [config] trong ack này.
 *
 * 3. [ignored] mang lý do cụ thể ([IgnoredGameConfigReason]) nên UI nói được
 *    "trường này bị khoá ở chế độ này" thay vì báo lỗi chung.
 *
 * Không có field `ok` ở đây: `ok` luôn true khi handler không throw, còn khi throw
 * thì ack trả `{ error: { code } }` và tầng network đã quy về `Result.Error`.
 */
data class ConfigUpdateAck(
    val changed: Boolean,
    val config: GameConfig,
    val ignored: List<IgnoredGameConfigField> = emptyList()
)
