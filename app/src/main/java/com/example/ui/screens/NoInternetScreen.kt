package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkError
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FinanceError
import com.example.ui.theme.MintGreenPrimary
import com.example.utils.DiagnosticStatus
import com.example.utils.NetworkConnectionState
import com.example.utils.NetworkDiagnostics
import com.example.utils.NetworkGeneration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium full-screen blocking screen displayed when offline or server unreachable.
 * Includes clean live network status for 2G / 3G / 4G / 5G / Wi-Fi detection,
 * Carrier SIM identification, and Retry / Network Settings actions.
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    connectionState: NetworkConnectionState = NetworkConnectionState.NoInternet,
    diagnostics: NetworkDiagnostics = NetworkDiagnostics()
) {
    // Intercept hardware and gesture back button to strictly prevent bypassing the restriction
    BackHandler(enabled = true) {
        // Back press is intentionally consumed to block navigation while offline
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localRetrying by remember { mutableStateOf(false) }

    val isChecking = localRetrying || connectionState is NetworkConnectionState.Checking
    val isFirestoreError = connectionState is NetworkConnectionState.FirestoreUnavailable

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || isSystemInDarkTheme()

    val screenBgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF09131F),
                Color(0xFF020617)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFEEF2F6),
                Color(0xFFE2E8F0)
            )
        )
    }

    val cardBg = if (isDark) DarkSurface else Color.White
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentColor = if (isFirestoreError) Color(0xFFF59E0B) else if (isDark) DarkError else FinanceError

    // Infinite breathing/pulsing animation for the Wi-Fi-off / Cloud-off icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val iconFloatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_float"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("no_internet_screen"),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBgGradient)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Stylish Animated Icon with Pulsing Halo Glow
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = pulseAlpha))
                    )

                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .offset(y = iconFloatY.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        accentColor.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = accentColor.copy(alpha = 0.45f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconVector = when {
                            isFirestoreError -> Icons.Default.CloudOff
                            diagnostics.isAirplaneMode -> Icons.Default.AirplanemodeActive
                            diagnostics.isCellular -> Icons.Default.SignalCellularConnectedNoInternet0Bar
                            else -> Icons.Default.WifiOff
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = "No Internet Connection",
                            tint = accentColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Status Pill Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFirestoreError) "DATABASE SERVER OFFLINE" else "STRICT ONLINE MODE REQUIRED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 3. Main Headline
                Text(
                    text = if (isFirestoreError) "Database Server Unavailable" else diagnostics.statusTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Subtitle / Reason in English
                Text(
                    text = if (isFirestoreError) {
                        (connectionState as? NetworkConnectionState.FirestoreUnavailable)?.message
                            ?: "Connected to internet, but unable to connect to Firebase Cloud Firestore. Please tap Retry."
                    } else {
                        diagnostics.statusDetail
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 5. LIVE NETWORK DIAGNOSTICS CARD (2G, 3G, 4G, 5G & Data Status)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("network_diagnostics_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = MintGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Network Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor
                                )
                            }

                            // 2G / 3G / 4G / 5G / Wi-Fi Generation Badge
                            NetworkGenerationBadge(generation = diagnostics.generation, label = diagnostics.generationLabel)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )

                        // Signal Generation Row
                        DiagnosticRow(
                            icon = if (diagnostics.isWifi) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                            label = "Signal Generation:",
                            value = diagnostics.generationLabel,
                            textColor = titleColor,
                            subtitleColor = subtitleColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Data Status Row
                        val dataStatusText = when (diagnostics.status) {
                            DiagnosticStatus.ONLINE -> "Connected"
                            DiagnosticStatus.MOBILE_DATA_OFF -> "OFF (Disabled)"
                            DiagnosticStatus.AIRPLANE_MODE_ON -> "Airplane Mode ON"
                            DiagnosticStatus.WIFI_NO_INTERNET -> "No ISP Internet"
                            DiagnosticStatus.NO_SIM_CARD -> "No Cellular Network"
                            else -> "Disconnected"
                        }
                        val dataStatusColor = if (diagnostics.status == DiagnosticStatus.ONLINE) MintGreenPrimary else accentColor

                        DiagnosticRow(
                            icon = Icons.Default.Info,
                            label = "Data Connection:",
                            value = dataStatusText,
                            textColor = dataStatusColor,
                            subtitleColor = subtitleColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Action: "Retry Connection" Button with Loading Animation
                Button(
                    onClick = {
                        if (!isChecking) {
                            localRetrying = true
                            scope.launch {
                                onRetry()
                                delay(1200)
                                localRetrying = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("retry_connection_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintGreenPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Checking Connection...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Connection",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Action: Open Device Mobile Network / SIM Settings
                OutlinedButton(
                    onClick = {
                        openNetworkSettings(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_network_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Mobile Network Settings",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NetworkGenerationBadge(
    generation: NetworkGeneration,
    label: String
) {
    val (badgeBg, textColor) = when (generation) {
        NetworkGeneration.GEN_5G -> Pair(Color(0xFF8B5CF6), Color.White)
        NetworkGeneration.GEN_4G -> Pair(Color(0xFF0EA5E9), Color.White)
        NetworkGeneration.GEN_3G -> Pair(Color(0xFFF59E0B), Color.White)
        NetworkGeneration.GEN_2G -> Pair(Color(0xFFF97316), Color.White)
        NetworkGeneration.WIFI -> Pair(Color(0xFF10B981), Color.White)
        NetworkGeneration.ETHERNET -> Pair(Color(0xFF06B6D4), Color.White)
        NetworkGeneration.NONE -> Pair(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeBg
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    label: String,
    value: String,
    textColor: Color,
    subtitleColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = subtitleColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = subtitleColor
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

private fun openNetworkSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
