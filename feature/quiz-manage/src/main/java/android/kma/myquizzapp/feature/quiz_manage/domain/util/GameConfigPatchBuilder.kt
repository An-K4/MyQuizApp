package android.kma.myquizzapp.feature.quiz_manage.domain.util

import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameModeDescriptor

/** Tính diff typed; việc dựng dotted-path JSON thuộc trách nhiệm core:network. */
fun buildGameConfigPatch(
    descriptor: GameModeDescriptor,
    values: Map<GameConfigKey, GameConfigValue>
): Map<GameConfigKey, GameConfigValue> = buildMap {
    descriptor.editable.forEach { (key, spec) ->
        val value = values[key] ?: return@forEach
        if (value != spec.defaultValue) put(key, value)
    }
}
