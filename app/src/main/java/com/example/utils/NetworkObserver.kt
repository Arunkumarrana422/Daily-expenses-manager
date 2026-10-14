package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed class NetworkConnectionState {
    object Connected : NetworkConnectionState()
    object Checking : NetworkConnectionState()
    object NoInternet : NetworkConnectionState()
    data class FirestoreUnavailable(val message: String) : NetworkConnectionState()
}

/**
 * Robust, stable network monitor ensuring the app functions strictly online only.
 * Accurately detects hardware link and internet availability without false-positive
 * disconnects, flickering, or repeatedly showing the No Internet screen while online.
 */
class NetworkObserver(private val context: Context) {
    private val TAG = "NetworkObserver"

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var verificationJob: Job? = null

    // Initialize with real hardware status: if hardware connected, treat as Connected initially
    // to avoid blocking user during startup when internet is present.
    private val initialConnected = checkHardwareConnection()

    private val _connectionState = MutableStateFlow<NetworkConnectionState>(
        if (initialConnected) NetworkConnectionState.Connected else NetworkConnectionState.NoInternet
    )
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _isOnline = MutableStateFlow(initialConnected)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerCallback()
        // If not connected at launch, verify and ensure NoInternet is set
        if (!initialConnected) {
            verifyConnectivity(isUserRetry = false)
        }
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                verifyConnectivity(isUserRetry = false)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                // Check if any other network interface is still active
                if (!checkHardwareConnection()) {
                    scope.launch {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                    }
                } else {
                    verifyConnectivity(isUserRetry = false)
                }
            }

            override fun onUnavailable() {
                scope.launch {
                    _connectionState.value = NetworkConnectionState.NoInternet
                    _isOnline.value = false
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (!hasInternet) {
                    scope.launch {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                    }
                } else {
                    // Only re-verify if we were previously offline, to prevent constant flickering
                    // on bandwidth/signal capability updates
                    if (_connectionState.value !is NetworkConnectionState.Connected) {
                        verifyConnectivity(isUserRetry = false)
                    }
                }
            }
        }
        networkCallback = callback

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
            verifyConnectivity(isUserRetry = false)
        }
    }

    fun checkHardwareConnection(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun isNetworkValidated(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    fun checkConnection(): Boolean = checkHardwareConnection()

    /**
     * Verifies connectivity cleanly.
     * When isUserRetry is true, shows Checking state for UI feedback.
     * When already connected, never flashes Checking to prevent interrupting the user.
     */
    fun verifyConnectivity(isUserRetry: Boolean = false) {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            if (!checkHardwareConnection()) {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                return@launch
            }

            if (isUserRetry && _connectionState.value !is NetworkConnectionState.Connected) {
                _connectionState.value = NetworkConnectionState.Checking
            }

            // If Android system already validated the network, it is definitely online
            if (isNetworkValidated()) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
                return@launch
            }

            // Fallback: check HTTP reachability
            val hasRealInternet = withContext(Dispatchers.IO) {
                checkHttpProbe()
            }

            if (hasRealInternet) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
            } else {
                // If hardware says connected but HTTP check completely failed, mark as NoInternet
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
            }
        }
    }

    private fun checkHttpProbe(): Boolean {
        return try {
            val url = URL("https://clients3.google.com/generate_204")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                instanceFollowRedirects = false
                useCaches = false
            }
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        } catch (e: Exception) {
            try {
                val url = URL("https://www.google.com/generate_204")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    instanceFollowRedirects = false
                    useCaches = false
                }
                val code = connection.responseCode
                connection.disconnect()
                code in 200..299
            } catch (e2: Exception) {
                // If both HTTP probes fail, check if hardware still has active internet capability
                checkHardwareConnection()
            }
        }
    }

    fun cleanup() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        scope.cancel()
    }

    fun refresh() {
        verifyConnectivity(isUserRetry = true)
    }
}
