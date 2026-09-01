package android.kma.myquizzapp.presentation.profile

/**
 * One-time effects cho màn Profile.
 * 
 * Effects được emit từ ViewModel và collect trong Screen để xử lý
 * navigation hoặc hiển thị toast/snackbar.
 */
sealed interface ProfileEffect {
    /**
     * Navigate back sau khi logout thành công.
     */
    data object NavigateBack : ProfileEffect
    
    /**
     * Hiển thị error message (toast/snackbar).
     */
    data class ShowError(val message: String) : ProfileEffect
}
