package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkConnection())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    init {
        val cm = connectivityManager
        if (cm != null) {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = checkConnection()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkConnection()
                }

                override fun onUnavailable() {
                    _isOnline.value = false
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    _isOnline.value = checkConnection()
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(networkCallback)
                } else {
                    val networkRequest = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    cm.registerNetworkCallback(networkRequest, networkCallback)
                }
            } catch (e: Exception) {
                _isOnline.value = checkConnection()
            }
        }
    }

    fun checkConnection(): Boolean {
        return try {
            val cm = connectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            hasInternet && (hasValidated || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            true
        }
    }

    fun refresh() {
        _isOnline.value = checkConnection()
    }
}
