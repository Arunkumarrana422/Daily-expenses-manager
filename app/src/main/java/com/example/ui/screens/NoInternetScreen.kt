package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.utils.NetworkConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium full-screen blocking screen displayed when the device is offline
 * or when the Firebase database is unreachable.
 * The application is strictly online-only; access to all features is completely
 * blocked until connectivity and Firebase reachability are restored.
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    connectionState: NetworkConnectionState = NetworkConnectionState.NoInternet
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

    // Smooth gradient background matching existing app theme
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
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. Stylish Animated Wi-Fi-Off / Cloud-Off Icon with Pulsing Halo Glow
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Pulsing Glow Ring
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = pulseAlpha))
                    )

                    // Inner Glow Container
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                        Icon(
                            imageVector = if (isFirestoreError) Icons.Default.CloudOff else Icons.Default.WifiOff,
                            contentDescription = if (isFirestoreError) "Server Offline" else "No Internet Connection",
                            tint = accentColor,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(bottom = 12.dp)
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
                    text = if (isFirestoreError) "Database Server Unavailable" else "No Internet Connection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Subtitle / Reason
                Text(
                    text = if (isFirestoreError) {
                        (connectionState as? NetworkConnectionState.FirestoreUnavailable)?.message
                            ?: "Connected to internet, but unable to establish a secure live session with the Firebase Cloud Firestore server. Please tap Retry to reconnect."
                    } else {
                        "Please connect to the internet to use this application."
                    },
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Help Checklist Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isFirestoreError) "Connection Diagnostics:" else "Please check the following:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        TroubleshootItem(
                            text = "Ensure Wi-Fi or Mobile Data is switched on",
                            iconColor = accentColor,
                            textColor = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TroubleshootItem(
                            text = "Disable Airplane mode if currently enabled",
                            iconColor = accentColor,
                            textColor = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TroubleshootItem(
                            text = "Confirm active internet package or router signal",
                            iconColor = accentColor,
                            textColor = subtitleColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 6. Prominent "Retry Connection" Button with Loading Animation
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
                        .height(54.dp)
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
                            text = "Verifying Connection...",
                            fontSize = 15.sp,
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

                // 7. Secondary Action: Open Device Network Settings
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_network_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Network Settings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TroubleshootItem(
    text: String,
    iconColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = textColor
        )
    }
}
