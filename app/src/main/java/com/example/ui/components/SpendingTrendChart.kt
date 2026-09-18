package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary
import com.example.utils.CurrencyFormatter

data class SpendingTrendBar(
    val label: String,
    val amount: Double,
    val isPeak: Boolean = false
)

@Composable
fun SpendingTrendChart(
    bars: List<SpendingTrendBar>,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(bars) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(650))
    }

    val maxAmount = remember(bars) {
        bars.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (bar in bars) {
                val heightRatio = ((bar.amount / maxAmount) * animationProgress.value).toFloat().coerceIn(0.04f, 1.0f)
                val barColor = if (bar.isPeak) IndigoPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (bar.amount > 0) {
                        Text(
                            text = CurrencyFormatter.format(bar.amount, currencySymbol, showDecimals = false),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height((120 * heightRatio).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barColor)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bar.label,
                        fontSize = 11.sp,
                        fontWeight = if (bar.isPeak) FontWeight.Bold else FontWeight.Normal,
                        color = if (bar.isPeak) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
