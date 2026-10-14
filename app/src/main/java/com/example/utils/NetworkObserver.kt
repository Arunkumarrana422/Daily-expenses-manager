package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
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

sealed class NetworkConnectionState {
    object Connected : NetworkConnectionState()
    object Checking : NetworkConnectionState()
    object NoInternet : NetworkConnectionState()
    data class FirestoreUnavailable(val message: String) : NetworkConnectionState()
}

enum class NetworkGeneration {
    GEN_5G,
    GEN_4G,
    GEN_3G,
    GEN_2G,
    WIFI,
    ETHERNET,
    NONE
}

enum class DiagnosticStatus {
    ONLINE,
    WIFI_NO_INTERNET,
    MOBILE_DATA_OFF,
    AIRPLANE_MODE_ON,
    NO_SIM_CARD,
    UNKNOWN_OFFLINE
}

data class NetworkDiagnostics(
    val generation: NetworkGeneration = NetworkGeneration.NONE,
    val generationLabel: String = "No Connection",
    val operatorName: String = "Unknown Operator",
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isAirplaneMode: Boolean = false,
    val hasLinkConnection: Boolean = false,
    val status: DiagnosticStatus = DiagnosticStatus.UNKNOWN_OFFLINE,
    val statusTitle: String = "No Internet Connection",
    val statusDetail: String = "Please connect to Wi-Fi or Mobile Data to continue."
)

/**
 * Reliable, production-grade network monitor ensuring the app functions strictly online.
 * Uses native Android ConnectivityManager with handover debouncing to prevent false lockouts,
 * flickering, or screen flapping during cell tower or Wi-Fi handovers.
 */
class NetworkObserver(private val context: Context) {
    private val TAG = "NetworkObserver"

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var debounceJob: Job? = null
    private var verificationJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var backgroundRetryJob: Job? = null

    private val initialConnected = checkHardwareConnection()

    private val _connectionState = MutableStateFlow<NetworkConnectionState>(
        if (initialConnected) NetworkConnectionState.Connected else NetworkConnectionState.NoInternet
    )
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _isOnline = MutableStateFlow(initialConnected)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _diagnostics = MutableStateFlow(evaluateDiagnostics(isConnected = initialConnected))
    val diagnostics: StateFlow<NetworkDiagnostics> = _diagnostics.asStateFlow()

    init {
        registerCallback()
        startBackgroundRetryLoop()
    }

    private fun startBackgroundRetryLoop() {
        backgroundRetryJob?.cancel()
        backgroundRetryJob = scope.launch {
            while (true) {
                delay(4000) // Silently check every 4 seconds when offline
                val currentState = _connectionState.value
                if (currentState is NetworkConnectionState.NoInternet || currentState is NetworkConnectionState.FirestoreUnavailable) {
                    _connectionState.value = NetworkConnectionState.Checking
                    delay(400)
                    val connected = checkHardwareConnection()
                    if (connected) {
                        _connectionState.value = NetworkConnectionState.Connected
                        _isOnline.value = true
                        _diagnostics.value = evaluateDiagnostics(isConnected = true)
                    } else {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                        _diagnostics.value = evaluateDiagnostics(isConnected = false)
                    }
                }
            }
        }
    }

    private fun registerCallback() {
        val cm = connectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Cancel any pending lost/disconnect debounce
                debounceJob?.cancel()
                debounceJob = null
                scope.launch {
                    val isConnected = checkHardwareConnection()
                    if (isConnected) {
                        _connectionState.value = NetworkConnectionState.Connected
                        _isOnline.value = true
                        _diagnostics.value = evaluateDiagnostics(isConnected = true)
                    }
                }
            }

            override fun onLost(network: Network) {
                // Network handover debounce (e.g. Wi-Fi <-> Cellular)
                // Prevents transient drops during switching from falsely locking out user
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(1200)
                    val isConnected = checkHardwareConnection()
                    if (!isConnected) {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                        _diagnostics.value = evaluateDiagnostics(isConnected = false)
                    }
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    debounceJob?.cancel()
                    debounceJob = null
                    scope.launch {
                        _connectionState.value = NetworkConnectionState.Connected
                        _isOnline.value = true
                        _diagnostics.value = evaluateDiagnostics(isConnected = true)
                    }
                } else {
                    debounceJob?.cancel()
                    debounceJob = scope.launch {
                        delay(1200)
                        if (!checkHardwareConnection()) {
                            _connectionState.value = NetworkConnectionState.NoInternet
                            _isOnline.value = false
                            _diagnostics.value = evaluateDiagnostics(isConnected = false)
                        }
                    }
                }
            }

            override fun onUnavailable() {
                scope.launch {
                    _connectionState.value = NetworkConnectionState.NoInternet
                    _isOnline.value = false
                    _diagnostics.value = evaluateDiagnostics(isConnected = false)
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
        }
    }

    fun checkHardwareConnection(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun checkConnection(): Boolean = checkHardwareConnection()

    fun evaluateDiagnostics(isConnected: Boolean): NetworkDiagnostics {
        val cm = connectivityManager
        val tm = telephonyManager
        val activeNetwork = try { cm?.activeNetwork } catch (_: Exception) { null }
        val caps = if (activeNetwork != null) {
            try { cm?.getNetworkCapabilities(activeNetwork) } catch (_: Exception) { null }
        } else null

        val isAirplaneMode = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } catch (_: Exception) {
            false
        }

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

        var rawOpName = ""
        try {
            rawOpName = tm?.networkOperatorName?.trim().orEmpty()
            if (rawOpName.isBlank()) {
                rawOpName = tm?.simOperatorName?.trim().orEmpty()
            }
        } catch (_: Exception) {}

        val cleanOpName = when {
            rawOpName.isNotBlank() -> rawOpName
            isCellular -> "Mobile SIM"
            isWifi -> "Wi-Fi Network"
            else -> "No Network"
        }

        val (generation, generationLabel) = when {
            isWifi -> Pair(NetworkGeneration.WIFI, "Wi-Fi")
            isEthernet -> Pair(NetworkGeneration.ETHERNET, "Ethernet")
            isCellular -> {
                val bandwidthKbps = caps?.linkDownstreamBandwidthKbps ?: 0
                var netType = TelephonyManager.NETWORK_TYPE_UNKNOWN
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        netType = tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                    }
                } catch (_: SecurityException) {}

                when (netType) {
                    TelephonyManager.NETWORK_TYPE_NR -> Pair(NetworkGeneration.GEN_5G, "5G")
                    TelephonyManager.NETWORK_TYPE_LTE -> {
                        if (bandwidthKbps >= 100_000) Pair(NetworkGeneration.GEN_5G, "5G")
                        else Pair(NetworkGeneration.GEN_4G, "4G (LTE)")
                    }
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_UMTS -> Pair(NetworkGeneration.GEN_3G, "3G")
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GSM -> Pair(NetworkGeneration.GEN_2G, "2G")
                    else -> {
                        when {
                            bandwidthKbps >= 100_000 -> Pair(NetworkGeneration.GEN_5G, "5G")
                            bandwidthKbps >= 10_000 -> Pair(NetworkGeneration.GEN_4G, "4G")
                            bandwidthKbps >= 1_000 -> Pair(NetworkGeneration.GEN_3G, "3G")
                            bandwidthKbps > 0 -> Pair(NetworkGeneration.GEN_2G, "2G")
                            else -> Pair(NetworkGeneration.GEN_4G, "4G / 5G")
                        }
                    }
                }
            }
            else -> Pair(NetworkGeneration.NONE, "No Connection")
        }

        val hasLink = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val simState = try { tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN } catch (_: Exception) { TelephonyManager.SIM_STATE_UNKNOWN }

        val (diagStatus, statusTitle, statusDetail) = when {
            isConnected || hasLink -> {
                Triple(
                    DiagnosticStatus.ONLINE,
                    "Internet Connected",
                    "Active internet connection is working properly."
                )
            }
            isAirplaneMode -> {
                Triple(
                    DiagnosticStatus.AIRPLANE_MODE_ON,
                    "Airplane Mode is Active",
                    "Airplane mode is enabled. Turn it off to restore Mobile Data or Wi-Fi."
                )
            }
            isWifi && !hasLink -> {
                Triple(
                    DiagnosticStatus.WIFI_NO_INTERNET,
                    "Wi-Fi Connected Without Internet",
                    "Connected to Wi-Fi router, but the internet service line is offline."
                )
            }
            simState == TelephonyManager.SIM_STATE_ABSENT && !isWifi -> {
                Triple(
                    DiagnosticStatus.NO_SIM_CARD,
                    "No Network Available",
                    "No cellular network found. Please connect to Wi-Fi or mobile network."
                )
            }
            !hasLink -> {
                Triple(
                    DiagnosticStatus.MOBILE_DATA_OFF,
                    "Mobile Data is Turned Off",
                    "Mobile data is switched off. Please enable Mobile Data or Wi-Fi."
                )
            }
            else -> {
                Triple(
                    DiagnosticStatus.UNKNOWN_OFFLINE,
                    "No Internet Connection",
                    "Please connect to the internet to use this application."
                )
            }
        }

        return NetworkDiagnostics(
            generation = generation,
            generationLabel = generationLabel,
            operatorName = cleanOpName,
            isWifi = isWifi,
            isCellular = isCellular,
            isAirplaneMode = isAirplaneMode,
            hasLinkConnection = hasLink,
            status = diagStatus,
            statusTitle = statusTitle,
            statusDetail = statusDetail
        )
    }

    fun checkSilently() {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            val connected = checkHardwareConnection()
            if (connected) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
                _diagnostics.value = evaluateDiagnostics(isConnected = true)
            } else {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                _diagnostics.value = evaluateDiagnostics(isConnected = false)
            }
        }
    }

    fun verifyConnectivity(isUserRetry: Boolean = false) {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            _connectionState.value = NetworkConnectionState.Checking
            delay(500)
            val connected = checkHardwareConnection()
            if (connected) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
                _diagnostics.value = evaluateDiagnostics(isConnected = true)
            } else {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                _diagnostics.value = evaluateDiagnostics(isConnected = false)
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
