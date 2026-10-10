package android.kma.myquizzapp.core.ui.components

/** Room codes are alphanumeric; OTPs are ASCII digits. No API validation or auto-submit here. */
enum class SixCharacterCodeKind { Otp, Room }

/** Pure input normalization, preserving room letters instead of restricting every code to digits. */
internal fun normalizeSixCharacterCode(raw: String, kind: SixCharacterCodeKind): String {
    val normalized = buildString {
        for (character in raw) {
            val upper = if (character in 'a'..'z') character.uppercaseChar() else character
            if (upper in '0'..'9' || (kind == SixCharacterCodeKind.Room && upper in 'A'..'Z')) {
                append(upper)
                if (length == 6) break
            }
        }
    }
    return normalized
}

