package android.kma.myquizzapp.core.network

import android.content.Context
import android.kma.myquizzapp.core.common.network.NetworkMonitor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) : NetworkMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    private var currentDefaultNetwork: Network? = connectivityManager.activeNetwork

    private val _isOnline = MutableStateFlow(
        currentDefaultNetwork
            ?.let(connectivityManager::getNetworkCapabilities)
            .hasValidatedInternet()
    )
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            currentDefaultNetwork = network
            _isOnline.value = connectivityManager
                .getNetworkCapabilities(network)
                .hasValidatedInternet()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (network == currentDefaultNetwork) {
                _isOnline.value = networkCapabilities.hasValidatedInternet()
            }
        }

        override fun onLost(network: Network) {
            if (network == currentDefaultNetwork) {
                currentDefaultNetwork = null
                _isOnline.value = false
            }
        }

        override fun onUnavailable() {
            currentDefaultNetwork = null
            _isOnline.value = false
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun NetworkCapabilities?.hasValidatedInternet(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
