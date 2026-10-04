package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.core.common.network.NetworkMonitor
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NetworkStatusViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {
    val isOnline = networkMonitor.isOnline
}
