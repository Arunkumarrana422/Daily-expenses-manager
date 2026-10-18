package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavigationTabItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * Custom Cradled Bottom Navigation Bar matching the provided reference designs.
 * Supports both Light mode and Dark mode with custom cradle notch, glowing center FAB,
 * and pill-shaped active tab indicators.
 */
@Composable
fun CradledBottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Colors matching the reference photos
    val barBgColor = if (isDark) Color(0xFF141A17) else Color(0xFFFFFFFF)
    val barBorderColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color.Transparent

    val activePillBg = if (isDark) Color(0xFF0D3322) else Color(0xFFE2F8EB)
    val activePillBorder = if (isDark) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)) else null

    val activeContentColor = if (isDark) Color(0xFF00E676) else Color(0xFF00A855)
    val activeIconColor = if (isDark) Color(0xFF00E676) else Color(0xFF00C853)

    val inactiveIconColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF37474F)
    val inactiveTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF455A64)

    val tabs = remember {
        listOf(
            NavigationTabItem("home", Icons.Outlined.Home, "Home"),
            NavigationTabItem("transactions", Icons.Default.ReceiptLong, "Expenses"),
            NavigationTabItem("add", Icons.Default.Add, "Add"),
            NavigationTabItem("reports", Icons.Default.BarChart, "Reports"),
            NavigationTabItem("settings", Icons.Default.Settings, "Settings")
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp)
            .padding(bottom = 6.dp)
            .height(94.dp)
            .testTag("main_bottom_nav_bar")
    ) {
        // Draw the cradled bar background with smooth cutout notch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .align(Alignment.BottomCenter)
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    val cornerRadius = 26.dp.toPx()
                    val cx = width / 2f
                    val cradleRadius = 37.dp.toPx()
                    val cradleBottomY = 40.dp.toPx()
                    val shoulderWidth = 14.dp.toPx()

                    val path = Path().apply {
                        // Start top-left corner
                        moveTo(0f, cornerRadius)
                        arcTo(
                            rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // Top line to left of cradle
                        val leftStart = cx - cradleRadius - shoulderWidth
                        lineTo(leftStart, 0f)

                        // Left shoulder down to cradle bottom
                        val leftCradleDipMidX = cx - cradleRadius * 0.65f
                        val leftCradleDipMidY = cradleBottomY * 0.65f

                        cubicTo(
                            x1 = leftStart + shoulderWidth * 0.5f,
                            y1 = 0f,
                            x2 = cx - cradleRadius,
                            y2 = leftCradleDipMidY * 0.4f,
                            x3 = leftCradleDipMidX,
                            y3 = leftCradleDipMidY
                        )

                        cubicTo(
                            x1 = cx - cradleRadius * 0.35f,
                            y1 = cradleBottomY,
                            x2 = cx - 12.dp.toPx(),
                            y2 = cradleBottomY,
                            x3 = cx,
                            y3 = cradleBottomY
                        )

                        // Cradle bottom to right shoulder
                        val rightCradleDipMidX = cx + cradleRadius * 0.65f
                        val rightEnd = cx + cradleRadius + shoulderWidth

                        cubicTo(
                            x1 = cx + 12.dp.toPx(),
                            y1 = cradleBottomY,
                            x2 = cx + cradleRadius * 0.35f,
                            y2 = cradleBottomY,
                            x3 = rightCradleDipMidX,
                            y3 = leftCradleDipMidY
                        )

                        cubicTo(
                            x1 = cx + cradleRadius,
                            y1 = leftCradleDipMidY * 0.4f,
                            x2 = rightEnd - shoulderWidth * 0.5f,
                            y2 = 0f,
                            x3 = rightEnd,
                            y3 = 0f
                        )

                        // Top line to right corner
                        lineTo(width - cornerRadius, 0f)
                        arcTo(
                            rect = Rect(width - cornerRadius * 2, 0f, width, cornerRadius * 2),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // Right edge down
                        lineTo(width, height - cornerRadius)
                        arcTo(
                            rect = Rect(width - cornerRadius * 2, height - cornerRadius * 2, width, height),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // Bottom line to left
                        lineTo(cornerRadius, height)
                        arcTo(
                            rect = Rect(0f, height - cornerRadius * 2, cornerRadius * 2, height),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        close()
                    }

                    // Shadow / Ambient Glow
                    if (!isDark) {
                        drawContext.canvas.nativeCanvas.apply {
                            val shadowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(38, 0, 0, 0)
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            drawPath(path.asAndroidPath(), shadowPaint)
                        }
                    } else {
                        drawContext.canvas.nativeCanvas.apply {
                            val glowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(22, 16, 185, 129)
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(10.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            drawPath(path.asAndroidPath(), glowPaint)
                        }
                    }

                    // Fill bar background
                    drawPath(path, color = barBgColor)

                    // Draw border in dark mode
                    if (isDark) {
                        drawPath(
                            path = path,
                            color = barBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
        )

        // Navigation Items Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index

                if (index == 2) {
                    // Center Add Item (Empty placeholder at top, label at bottom)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onAddClick() }
                            .testTag("nav_item_add"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) activeContentColor else inactiveTextColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    // Regular Tab Items: Home, Expenses, Reports, Settings
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) }
                            .testTag("nav_item_${tab.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pill icon container for selected tab
                        Box(
                            modifier = Modifier
                                .width(62.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .then(
                                    if (isSelected) {
                                        if (activePillBorder != null) {
                                            Modifier
                                                .background(activePillBg, RoundedCornerShape(17.dp))
                                                .border(activePillBorder, RoundedCornerShape(17.dp))
                                        } else {
                                            Modifier.background(activePillBg, RoundedCornerShape(17.dp))
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) activeIconColor else inactiveIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) activeContentColor else inactiveTextColor
                        )
                    }
                }
            }
        }

        // Floating Center Green Plus Action Button (Nested in cradle)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
        ) {
            // Ambient Green Radial Glow in Dark Mode
            if (isDark) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF10B981).copy(alpha = 0.55f),
                                        Color(0xFF10B981).copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width / 2f
                                )
                            )
                        }
                )
            }

            // Green Gradient Circular FAB
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .align(Alignment.Center)
                    .shadow(
                        elevation = if (isDark) 8.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                        ambientColor = if (isDark) Color(0xFF10B981) else Color(0x33000000)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF34D399),
                                    Color(0xFF10B981),
                                    Color(0xFF047857)
                                ),
                                center = Offset(20f, 18f),
                                radius = 110f
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF22C55E),
                                    Color(0xFF059669)
                                )
                            )
                        }
                    )
                    .clickable { onAddClick() }
                    .testTag("home_fab_add"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
