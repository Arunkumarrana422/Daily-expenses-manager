package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    // Do not instantly mark offline on backgrounding; verify active connection
                    scope.launch(Dispatchers.IO) {
                        delay(400)
                        _isOnline.value = checkConnection()
                    }
                }

                override fun onUnavailable() {
                    scope.launch(Dispatchers.IO) {
                        delay(400)
                        _isOnline.value = checkConnection()
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    if (hasInternet) {
                        _isOnline.value = true
                    } else {
                        scope.launch(Dispatchers.IO) {
                            delay(400)
                            _isOnline.value = checkConnection()
                        }
                    }
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

        // Active background poller to ensure continuous sync and accurate state
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2000)
                val currentStatus = checkConnection()
                if (_isOnline.value != currentStatus) {
                    _isOnline.value = currentStatus
                }
            }
        }
    }

    fun checkConnection(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun refresh() {
        _isOnline.value = checkConnection()
    }
}
