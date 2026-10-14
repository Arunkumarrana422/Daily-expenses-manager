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
    RECHARGE_EXPIRED_OR_DATA_OVER, // 2G/3G/4G/5G tower connected, but internet packets blocked
    CAPTIVE_PORTAL,                // Portal redirect detected (login or recharge required)
    WIFI_NO_INTERNET,              // Wi-Fi connected to router, no external uplink
    MOBILE_DATA_OFF,               // SIM ready but Mobile Data turned off
    AIRPLANE_MODE_ON,              // Airplane mode active
    NO_SIM_CARD,                   // No SIM card inserted
    SERVER_DOWN,                   // Cloud Firestore unreachable
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
    val isInternetValidated: Boolean = false,
    val isHttpProbeSuccess: Boolean = false,
    val status: DiagnosticStatus = DiagnosticStatus.UNKNOWN_OFFLINE,
    val statusTitle: String = "No Internet Connection",
    val statusDetail: String = "Please check your network settings.",
    val estimatedBandwidthKbps: Int = 0
)

/**
 * Robust network monitor ensuring the app functions strictly online only.
 * Includes complete diagnostics for 2G / 3G / 4G / 5G / Wi-Fi detection,
 * Carrier / SIM identification, and Recharge Validity / Data Pack exhaustion diagnosis.
 */
class NetworkObserver(private val context: Context) {
    private val TAG = "NetworkObserver"

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var verificationJob: Job? = null

    private val initialConnected = checkHardwareConnection()

    private val _connectionState = MutableStateFlow<NetworkConnectionState>(
        if (initialConnected) NetworkConnectionState.Connected else NetworkConnectionState.NoInternet
    )
    val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    private val _isOnline = MutableStateFlow(initialConnected)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _diagnostics = MutableStateFlow(evaluateDiagnostics(isProbeSuccess = initialConnected))
    val diagnostics: StateFlow<NetworkDiagnostics> = _diagnostics.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerCallback()
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
                if (!checkHardwareConnection()) {
                    scope.launch {
                        _connectionState.value = NetworkConnectionState.NoInternet
                        _isOnline.value = false
                        _diagnostics.value = evaluateDiagnostics(isProbeSuccess = false)
                    }
                } else {
                    verifyConnectivity(isUserRetry = false)
                }
            }

            override fun onUnavailable() {
                scope.launch {
                    _connectionState.value = NetworkConnectionState.NoInternet
                    _isOnline.value = false
                    _diagnostics.value = evaluateDiagnostics(isProbeSuccess = false)
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
                        _diagnostics.value = evaluateDiagnostics(isProbeSuccess = false)
                    }
                } else {
                    // Update diagnostics live (e.g. 4G/5G changes, bandwidth changes)
                    if (_connectionState.value !is NetworkConnectionState.Connected) {
                        verifyConnectivity(isUserRetry = false)
                    } else {
                        scope.launch {
                            _diagnostics.value = evaluateDiagnostics(isProbeSuccess = true)
                        }
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
     * Evaluates comprehensive network diagnostics including 2G/3G/4G/5G generation,
     * carrier identity, captive portal, and recharge validity / data pack exhaustion.
     */
    fun evaluateDiagnostics(isProbeSuccess: Boolean): NetworkDiagnostics {
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

        // Extract Operator Name
        var rawOpName = ""
        try {
            rawOpName = tm?.networkOperatorName?.trim().orEmpty()
            if (rawOpName.isBlank()) {
                rawOpName = tm?.simOperatorName?.trim().orEmpty()
            }
        } catch (_: Exception) {}

        // Map USSD / Operator Info for Recharge Checking
        val lowerOp = rawOpName.lowercase()
        val (ussdCode, cleanOpName) = when {
            lowerOp.contains("jio") || lowerOp.contains("reliance") -> Pair("1299", "Jio")
            lowerOp.contains("airtel") -> Pair("*121#", "Airtel")
            lowerOp.contains("vi") || lowerOp.contains("vodafone") || lowerOp.contains("idea") -> Pair("*199#", "Vi")
            lowerOp.contains("bsnl") || lowerOp.contains("cellone") -> Pair("*123#", "BSNL")
            rawOpName.isNotBlank() -> Pair("*121#", rawOpName)
            isCellular -> Pair("*121#", "Mobile SIM")
            isWifi -> Pair(null, "Wi-Fi Network")
            else -> Pair(null, "No Network")
        }

        // Determine Generation (5G, 4G, 3G, 2G, Wi-Fi)
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
                        if (bandwidthKbps >= 120_000) Pair(NetworkGeneration.GEN_5G, "5G (NSA)")
                        else Pair(NetworkGeneration.GEN_4G, "4G (LTE)")
                    }
                    TelephonyManager.NETWORK_TYPE_HSPAP,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_UMTS,
                    TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_EVDO_A,
                    TelephonyManager.NETWORK_TYPE_EVDO_B,
                    TelephonyManager.NETWORK_TYPE_EHRPD -> Pair(NetworkGeneration.GEN_3G, "3G")
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyManager.NETWORK_TYPE_IDEN,
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

        val isCaptivePortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        val hasLink = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val simState = try { tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN } catch (_: Exception) { TelephonyManager.SIM_STATE_UNKNOWN }

        // Determine Diagnostic Status & User-friendly English explanations
        val (diagStatus, statusTitle, statusDetail) = when {
            isProbeSuccess && (isValidated || hasLink) -> {
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
            isCaptivePortal -> {
                Triple(
                    DiagnosticStatus.CAPTIVE_PORTAL,
                    "Recharge / Login Required",
                    "The network is redirecting requests. Your recharge pack or daily data limit may be expired."
                )
            }
            isCellular && !isProbeSuccess -> {
                Triple(
                    DiagnosticStatus.RECHARGE_EXPIRED_OR_DATA_OVER,
                    "Recharge Expired or Data Pack Over",
                    "Connected to $generationLabel tower ($cleanOpName), but internet data is not responding. Likely reasons: 1. Daily data quota (1.5GB/2GB) is exhausted. 2. Main plan validity has expired."
                )
            }
            isWifi && !isProbeSuccess -> {
                Triple(
                    DiagnosticStatus.WIFI_NO_INTERNET,
                    "Wi-Fi Connected Without Internet",
                    "Connected to Wi-Fi router, but the internet service line is offline or disconnected."
                )
            }
            simState == TelephonyManager.SIM_STATE_ABSENT -> {
                Triple(
                    DiagnosticStatus.NO_SIM_CARD,
                    "No SIM Card Detected",
                    "No SIM card found in the device. Connect to Wi-Fi to use the application."
                )
            }
            !hasLink -> {
                Triple(
                    DiagnosticStatus.MOBILE_DATA_OFF,
                    "Mobile Data is Turned Off",
                    "Mobile data ($cleanOpName) is switched off. Please enable Mobile Data or Wi-Fi."
                )
            }
            else -> {
                Triple(
                    DiagnosticStatus.UNKNOWN_OFFLINE,
                    "No Internet Connection",
                    "Unable to communicate with the network. Please verify connection and retry."
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
            isInternetValidated = isValidated,
            isHttpProbeSuccess = isProbeSuccess,
            status = diagStatus,
            statusTitle = statusTitle,
            statusDetail = statusDetail,
            estimatedBandwidthKbps = caps?.linkDownstreamBandwidthKbps ?: 0
        )
    }

    /**
     * Verifies connectivity cleanly and updates diagnostics.
     */
    fun verifyConnectivity(isUserRetry: Boolean = false) {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            if (!checkHardwareConnection()) {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                _diagnostics.value = evaluateDiagnostics(isProbeSuccess = false)
                return@launch
            }

            if (isUserRetry && _connectionState.value !is NetworkConnectionState.Connected) {
                _connectionState.value = NetworkConnectionState.Checking
            }

            if (isNetworkValidated()) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
                _diagnostics.value = evaluateDiagnostics(isProbeSuccess = true)
                return@launch
            }

            // Fallback: check HTTP reachability
            val hasRealInternet = withContext(Dispatchers.IO) {
                checkHttpProbe()
            }

            if (hasRealInternet) {
                _connectionState.value = NetworkConnectionState.Connected
                _isOnline.value = true
                _diagnostics.value = evaluateDiagnostics(isProbeSuccess = true)
            } else {
                _connectionState.value = NetworkConnectionState.NoInternet
                _isOnline.value = false
                _diagnostics.value = evaluateDiagnostics(isProbeSuccess = false)
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

