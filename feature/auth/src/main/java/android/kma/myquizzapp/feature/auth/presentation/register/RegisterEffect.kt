package android.kma.myquizzapp.feature.auth.presentation.register

/**
 * One-shot side effects for Register screen (MVI pattern).
 */
sealed interface RegisterEffect {
    data object NavigateToHostHome : RegisterEffect
    data class ShowMessage(val message: String) : RegisterEffect
}
