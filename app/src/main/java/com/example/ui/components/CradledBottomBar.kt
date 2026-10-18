package com.example.ui.components

import android.graphics.BlurMaskFilter
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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

val CRADLED_NAV_BAR_CONTENT_HEIGHT = 68.dp
val CRADLED_NAV_BAR_PROTRUSION = 18.dp
val CRADLED_NAV_BAR_BASE_HEIGHT = CRADLED_NAV_BAR_CONTENT_HEIGHT + CRADLED_NAV_BAR_PROTRUSION

/**
 * Dynamic content bottom padding that automatically adjusts when the user
 * switches between 3-button system navigation and gesture swipe navigation.
 * Ensures cards, buttons, and bottom content are never hidden behind the navigation bar.
 */
@Composable
fun rememberBottomBarPadding(extraPadding: androidx.compose.ui.unit.Dp = 16.dp): androidx.compose.ui.unit.Dp {
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return CRADLED_NAV_BAR_BASE_HEIGHT + navBarBottomInset + extraPadding
}

/**
 * Custom Cradled Bottom Navigation Bar:
 * - Docked to bottom (full width edge-to-edge, no left/right/bottom gaps).
 * - Exact circular cradle cutout conforming smoothly to the round Add button (not a V-cut).
 * - Rounded top-left and top-right corners; flush flat bottom.
 * - Supports Light and Dark modes with glowing green Add FAB and active pill indicators.
 */
@Composable
fun CradledBottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Colors
    val barBgColor = if (isDark) Color(0xFF141A17) else Color(0xFFFFFFFF)
    val barBorderColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0x18000000)

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

    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barContentHeight = CRADLED_NAV_BAR_CONTENT_HEIGHT
    val protrusion = CRADLED_NAV_BAR_PROTRUSION
    val totalHeight = barContentHeight + protrusion + navBarBottomInset

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .testTag("main_bottom_nav_bar")
    ) {
        // Full width background canvas with exact circular cradle cutout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    val barTop = protrusion.toPx()
                    val cornerRadius = 12.dp.toPx()
                    val cx = width / 2f

                    // Exact circular cradle concentric with 56dp Add button
                    val buttonRadius = 28.dp.toPx()
                    val gap = 5.dp.toPx()
                    val cradleRadius = buttonRadius + gap // ~33dp radius
                    val buttonCenterY = 28.dp.toPx()
                    val shoulderRadius = 8.dp.toPx()

                    // Magic constant for cubic bezier circular arc approximation
                    val k = 0.55228475f * cradleRadius

                    val path = Path().apply {
                        // 1. Top-left corner (reduced roundness to 12dp)
                        moveTo(0f, barTop + cornerRadius)
                        arcTo(
                            rect = Rect(0f, barTop, cornerRadius * 2, barTop + cornerRadius * 2),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // 2. Horizontal line to left shoulder
                        val leftShoulderStart = cx - cradleRadius - shoulderRadius
                        lineTo(leftShoulderStart, barTop)

                        // 3. Smooth left shoulder curving into vertical tangent of the circle
                        cubicTo(
                            x1 = cx - cradleRadius - shoulderRadius * 0.45f,
                            y1 = barTop,
                            x2 = cx - cradleRadius,
                            y2 = barTop + (buttonCenterY - barTop) * 0.35f,
                            x3 = cx - cradleRadius,
                            y3 = buttonCenterY
                        )

                        // 4. Exact semi-circular cradle arc: left to bottom
                        cubicTo(
                            x1 = cx - cradleRadius,
                            y1 = buttonCenterY + k,
                            x2 = cx - k,
                            y2 = buttonCenterY + cradleRadius,
                            x3 = cx,
                            y3 = buttonCenterY + cradleRadius
                        )

                        // 5. Exact semi-circular cradle arc: bottom to right
                        cubicTo(
                            x1 = cx + k,
                            y1 = buttonCenterY + cradleRadius,
                            x2 = cx + cradleRadius,
                            y2 = buttonCenterY + k,
                            x3 = cx + cradleRadius,
                            y3 = buttonCenterY
                        )

                        // 6. Smooth right shoulder curving back into horizontal line
                        cubicTo(
                            x1 = cx + cradleRadius,
                            y1 = barTop + (buttonCenterY - barTop) * 0.35f,
                            x2 = cx + cradleRadius + shoulderRadius * 0.45f,
                            y2 = barTop,
                            x3 = cx + cradleRadius + shoulderRadius,
                            y3 = barTop
                        )

                        // 7. Horizontal line to top-right corner (reduced roundness to 12dp)
                        lineTo(width - cornerRadius, barTop)
                        arcTo(
                            rect = Rect(width - cornerRadius * 2, barTop, width, barTop + cornerRadius * 2),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // 8. Right edge straight down to bottom (docked flush!)
                        lineTo(width, height)

                        // 9. Bottom edge straight to left (no bottom corners, no bottom gap!)
                        lineTo(0f, height)

                        // 10. Left edge straight up to start
                        close()
                    }

                    // Shadow (neutral drop shadow so the cradle cutout gap stays visible in dark mode too)
                    drawContext.canvas.nativeCanvas.apply {
                        val shadowColor = if (isDark) {
                            android.graphics.Color.argb(45, 0, 0, 0)
                        } else {
                            android.graphics.Color.argb(25, 0, 0, 0)
                        }
                        val shadowPaint = android.graphics.Paint().apply {
                            color = shadowColor
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        drawPath(path.asAndroidPath(), shadowPaint)
                    }

                    // Fill bar background completely to the bottom edge
                    drawPath(path, color = barBgColor)

                    // Draw border (along top curved line and sides)
                    drawPath(
                        path = path,
                        color = barBorderColor,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
        )

        // Navigation Items Row (placed in the top content area, safe from bottom nav insets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barContentHeight)
                .align(Alignment.TopCenter)
                .offset(y = protrusion),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index

                if (index == 2) {
                    // Center Add Item (Add button is floating above, label is at bottom)
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
                            modifier = Modifier.padding(bottom = 6.dp)
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

        // Floating Center Green Circular Action Button (Nested inside circular cradle)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp)
        ) {
            // Green Gradient Circular FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color(0x35000000),
                        ambientColor = Color(0x25000000)
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
                                center = Offset(18f, 16f),
                                radius = 100f
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
