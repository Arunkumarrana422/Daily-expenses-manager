package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircleOutline
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen blocking screen shown when the device is offline.
 * The application does not work without an active internet connection,
 * matching modern financial/online app behavior.
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || isSystemInDarkTheme()

    val screenBg = if (isDark) DarkBackground else Color(0xFFF8FAFC)
    val cardBg = if (isDark) DarkSurface else Color.White
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val errorColor = if (isDark) DarkError else FinanceError

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("no_internet_screen"),
        containerColor = screenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                // 1. Icon Container with Warning/Error Glow
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    errorColor.copy(alpha = 0.18f),
                                    errorColor.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = errorColor.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "No Internet Connection",
                        tint = errorColor,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = errorColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, errorColor.copy(alpha = 0.25f)),
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
                                .background(errorColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFLINE MODE BLOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = errorColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 3. Main Headline
                Text(
                    text = "No Internet Connection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Subtitle / Reason
                Text(
                    text = "This application requires an active internet connection to function. Offline usage is disabled to ensure data integrity and security.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 5. Help Card with Checklist
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
                            text = "Please check the following:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        TroubleshootItem(
                            text = "Check Wi-Fi or mobile data toggle",
                            iconColor = errorColor,
                            textColor = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TroubleshootItem(
                            text = "Disable Airplane mode if enabled",
                            iconColor = errorColor,
                            textColor = subtitleColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TroubleshootItem(
                            text = "Ensure active data plan on your device",
                            iconColor = errorColor,
                            textColor = subtitleColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 6. Primary Action: Try Again / Retry
                Button(
                    onClick = {
                        if (!isRetrying) {
                            isRetrying = true
                            scope.launch {
                                onRetry()
                                delay(900)
                                isRetrying = false
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
                    enabled = !isRetrying
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Checking Connection...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Try Again",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Secondary Action: Open Network Settings
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
                .background(iconColor.copy(alpha = 0.8f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = textColor
        )
    }
}
