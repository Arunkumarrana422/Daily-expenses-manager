package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CategoryIconHelper {
    fun getIcon(categoryName: String, iconKey: String? = null): ImageVector {
        val combined = "${categoryName.lowercase()} ${(iconKey ?: "").lowercase()}"
        return when {
            combined.contains("food") || combined.contains("🍔") || combined.contains("restaurant") || combined.contains("cafe") || combined.contains("coffee") || combined.contains("☕") -> Icons.Default.ShoppingCart
            combined.contains("grocery") || combined.contains("🛒") || combined.contains("shop") || combined.contains("store") || combined.contains("market") -> Icons.Default.ShoppingCart
            combined.contains("transport") || combined.contains("car") || combined.contains("🚗") || combined.contains("fuel") || combined.contains("⛽") || combined.contains("gas") || combined.contains("taxi") || combined.contains("🚕") -> Icons.Default.DirectionsCar
            combined.contains("rent") || combined.contains("house") || combined.contains("home") || combined.contains("🏠") -> Icons.Default.Home
            combined.contains("mobile") || combined.contains("phone") || combined.contains("📱") || combined.contains("recharge") -> Icons.Default.Phone
            combined.contains("salary") || combined.contains("💼") || combined.contains("money") || combined.contains("income") || combined.contains("cash") || combined.contains("🪙") || combined.contains("💰") || combined.contains("emi") || combined.contains("credit") || combined.contains("💳") -> Icons.Default.Payments
            combined.contains("bank") || combined.contains("investment") || combined.contains("📈") -> Icons.Default.AccountBalance
            combined.contains("star") || combined.contains("gift") || combined.contains("🎁") -> Icons.Default.Star
            combined.contains("settings") || combined.contains("work") || combined.contains("business") || combined.contains("freelance") || combined.contains("💻") || combined.contains("tech") -> Icons.Default.Build
            combined.contains("travel") || combined.contains("trip") || combined.contains("✈") || combined.contains("place") -> Icons.Default.Place
            combined.contains("notice") || combined.contains("reminder") -> Icons.Default.Notifications
            combined.contains("person") || combined.contains("user") -> Icons.Default.Person
            else -> Icons.Default.Category
        }
    }
}

@Composable
fun CategoryIconBadge(
    categoryName: String,
    iconKey: String? = null,
    backgroundColor: Color,
    tintColor: Color = backgroundColor,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIconHelper.getIcon(categoryName, iconKey),
            contentDescription = categoryName,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
