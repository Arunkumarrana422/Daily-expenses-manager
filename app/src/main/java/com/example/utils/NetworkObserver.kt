package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

sealed class NetworkConnectionState {
    object Connected : NetworkConnectionState()
    object Checking : NetworkConnectionState()
    object NoInternet : NetworkConnectionState()
    data class FirestoreUnavailable(val message: String) : NetworkConnectionState()
}

/**
 * Robust network monitor ensuring the app functions strictly online only.
 * Validates hardware network link, real internet reachability (HTTP 204 ping),
 * and live Firebase Firestore server connectivity without local cache bypass.
 */
class NetworkObserver(private val context: Context) {
    private val TAG = "NetworkObserver"

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var verificationJob: Job? = null

    private val _connectionState = MutableStateFlow<NetworkConnectionState>(
        if (checkHardwareConnection()) NetworkConnectionState.Checking else NetworkConnectionState.NoInternet
    )
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _isOnline = MutableStateFlow(_connectionState.value is NetworkConnectionState.Connected)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerCallback()
        verifyConnectivity()
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network, verifying connection...")
                verifyConnectivity()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                if (!checkHardwareConnection()) {
                    scope.launch {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                    }
                } else {
                    verifyConnectivity()
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
                    verifyConnectivity()
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
            verifyConnectivity()
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

    fun checkConnection(): Boolean = checkHardwareConnection()

    /**
     * Complete live verification:
     * 1. Hardware network check
     * 2. Live HTTP 204 ping (detects Wi-Fi connected with no internet or captive portal)
     * 3. Live Firebase Firestore server probe with Source.SERVER
     */
    fun verifyConnectivity() {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            if (!checkHardwareConnection()) {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                return@launch
            }

            _connectionState.value = NetworkConnectionState.Checking

            // Step 2: Live HTTP probe for actual internet access
            val hasRealInternet = withContext(Dispatchers.IO) {
                checkHttpProbe()
            }

            if (!hasRealInternet) {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                return@launch
            }

            // Step 3: Firebase Firestore live server check
            val firestoreStatus = withContext(Dispatchers.IO) {
                checkFirestoreServer()
            }

            if (firestoreStatus is FirestoreProbeResult.Success) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
            } else if (firestoreStatus is FirestoreProbeResult.Unavailable) {
                _connectionState.value = NetworkConnectionState.FirestoreUnavailable(firestoreStatus.message)
                _isOnline.value = false
            } else {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
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
                false
            }
        }
    }

    private suspend fun checkFirestoreServer(): FirestoreProbeResult {
        return try {
            val fs = FirebaseFirestore.getInstance()
            withTimeoutOrNull(4500L) {
                try {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    val docRef = if (uid != null) {
                        fs.collection("users").document(uid)
                    } else {
                        fs.collection("_app_connection_probe").document("ping")
                    }

                    // Source.SERVER guarantees checking real backend availability, not cached documents
                    docRef.get(Source.SERVER).await()
                    FirestoreProbeResult.Success
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                        FirestoreProbeResult.Unavailable("Firebase Firestore is currently unreachable. Please retry.")
                    } else {
                        // Received server response (such as PERMISSION_DENIED or NOT_FOUND) from the live server
                        FirestoreProbeResult.Success
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("UNAVAILABLE", ignoreCase = true) ||
                        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                        msg.contains("Failed to get document from server", ignoreCase = true)) {
                        FirestoreProbeResult.Unavailable("Firebase server connection failed. Please retry.")
                    } else {
                        FirestoreProbeResult.Success
                    }
                }
            } ?: FirestoreProbeResult.Unavailable("Firebase server connection timed out. Please check your internet and retry.")
        } catch (e: Exception) {
            FirestoreProbeResult.Unavailable(e.message ?: "Could not reach Firestore database")
        }
    }

    fun cleanup() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        scope.cancel()
    }

    fun refresh() {
        verifyConnectivity()
    }

    sealed class FirestoreProbeResult {
        object Success : FirestoreProbeResult()
        data class Unavailable(val message: String) : FirestoreProbeResult()
    }
}
