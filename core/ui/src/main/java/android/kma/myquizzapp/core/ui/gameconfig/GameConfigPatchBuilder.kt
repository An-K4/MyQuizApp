package android.kma.myquizzapp.core.ui.gameconfig

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameModeDescriptor

/**
 * Tính diff typed; việc dựng dotted-path JSON thuộc trách nhiệm core:network.
 *
 * [baseline] là "giá trị đang có", mặc định là default của mode. Đó đúng cho
 * CreateRoom (phòng chưa tồn tại nên mọi thứ đều đang ở default) nhưng SAI cho
 * HostLobby: phòng đang mở có config riêng, nếu vẫn diff so với default thì
 * patch sẽ chứa cả những field host không hề đụng tới, và tệ hơn là sẽ Bỏ SÓT
 * field mà host vừa đổi về đúng giá trị default (vì trùng default nên bị lọc ra,
 * trong khi config hiện tại đang khác default) — host bấm lưu mà không gì xảy ra.
 *
 * Chỉ xét các key `editable` của mode: gửi field locked lên thì backend
 * `sanitizeConfigPatch` cũng loại và trả lại trong `ignored`.
 */
fun buildGameConfigPatch(
    descriptor: GameModeDescriptor,
    values: Map<GameConfigKey, GameConfigValue>,
    baseline: Map<GameConfigKey, GameConfigValue> = descriptor.defaultBaseline()
): Map<GameConfigKey, GameConfigValue> = buildMap {
    descriptor.editable.forEach { (key, spec) ->
        val value = values[key] ?: return@forEach
        val current = baseline[key] ?: spec.defaultValue
        if (value != current) put(key, value)
    }
}

/** Baseline mặc định: default của từng field editable trong mode. */
fun GameModeDescriptor.defaultBaseline(): Map<GameConfigKey, GameConfigValue> =
    editable.mapValues { it.value.defaultValue }

/**
 * Baseline lấy từ config thật của một phòng đang tồn tại. Dùng ở HostLobby.
 */
fun GameConfig.baselineFor(descriptor: GameModeDescriptor): Map<GameConfigKey, GameConfigValue> =
    descriptor.editable.keys.associateWith { key -> valueOf(key) }
