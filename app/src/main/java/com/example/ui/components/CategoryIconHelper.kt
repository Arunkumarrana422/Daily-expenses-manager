package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
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
            combined.contains("restaurant") || combined.contains("food") || combined.contains("cafe") || combined.contains("coffee") || combined.contains("burger") || combined.contains("pizza") || combined.contains("dinner") || combined.contains("lunch") || combined.contains("breakfast") || combined.contains("snack") || combined.contains("eat") || combined.contains("meal") || combined.contains("tea") -> Icons.Default.Restaurant
            combined.contains("gas") || combined.contains("fuel") || combined.contains("petrol") || combined.contains("diesel") || combined.contains("cng") -> Icons.Default.LocalGasStation
            combined.contains("transport") || combined.contains("car") || combined.contains("taxi") || combined.contains("cab") || combined.contains("auto") || combined.contains("bus") || combined.contains("train") || combined.contains("vehicle") || combined.contains("directions_car") -> Icons.Default.DirectionsCar
            combined.contains("grocery") || combined.contains("market") || combined.contains("mart") || combined.contains("kirana") || combined.contains("vegetable") || combined.contains("fruit") || combined.contains("milk") || combined.contains("shopping_cart") -> Icons.Default.ShoppingCart
            combined.contains("shopping") || combined.contains("cloth") || combined.contains("dress") || combined.contains("mall") || combined.contains("buy") || combined.contains("order") -> Icons.Default.ShoppingBag
            combined.contains("rent") || combined.contains("house") || combined.contains("home") || combined.contains("flat") || combined.contains("room") -> Icons.Default.Home
            combined.contains("electric") || combined.contains("power") || combined.contains("current") || combined.contains("light") || combined.contains("bolt") -> Icons.Default.Bolt
            combined.contains("mobile") || combined.contains("phone") || combined.contains("recharge") || combined.contains("call") -> Icons.Default.Phone
            combined.contains("internet") || combined.contains("wifi") || combined.contains("broadband") || combined.contains("fiber") || combined.contains("data") -> Icons.Default.Wifi
            combined.contains("school") || combined.contains("college") || combined.contains("education") || combined.contains("course") || combined.contains("study") || combined.contains("book") || combined.contains("tuition") || combined.contains("fees") -> Icons.Default.School
            combined.contains("health") || combined.contains("medical") || combined.contains("doctor") || combined.contains("hospital") || combined.contains("medicine") || combined.contains("pharmacy") || combined.contains("clinic") -> Icons.Default.MedicalServices
            combined.contains("travel") || combined.contains("flight") || combined.contains("trip") || combined.contains("vacation") || combined.contains("tour") -> Icons.Default.Flight
            combined.contains("place") || combined.contains("location") -> Icons.Default.Place
            combined.contains("movie") || combined.contains("cinema") || combined.contains("entertainment") || combined.contains("netflix") || combined.contains("show") || combined.contains("film") -> Icons.Default.Movie
            combined.contains("salary") || combined.contains("wage") || combined.contains("pay") || combined.contains("money") || combined.contains("income") || combined.contains("cash") || combined.contains("emi") || combined.contains("loan") || combined.contains("payments") -> Icons.Default.Payments
            combined.contains("business") || combined.contains("office") || combined.contains("client") || combined.contains("work") -> Icons.Default.Work
            combined.contains("freelance") || combined.contains("tech") || combined.contains("coding") || combined.contains("software") || combined.contains("laptop") || combined.contains("computer") -> Icons.Default.Computer
            combined.contains("bank") || combined.contains("investment") || combined.contains("mutual") || combined.contains("share") || combined.contains("stock") || combined.contains("sip") || combined.contains("deposit") || combined.contains("account_balance") -> Icons.Default.AccountBalance
            combined.contains("gift") || combined.contains("present") || combined.contains("card_giftcard") -> Icons.Default.CardGiftcard
            combined.contains("star") || combined.contains("bonus") || combined.contains("reward") -> Icons.Default.Star
            combined.contains("build") || combined.contains("repair") || combined.contains("service") || combined.contains("tool") -> Icons.Default.Build
            combined.contains("notice") || combined.contains("bell") || combined.contains("reminder") || combined.contains("notifications") -> Icons.Default.Notifications
            combined.contains("person") || combined.contains("user") || combined.contains("profile") -> Icons.Default.Person
            combined.contains("setting") || combined.contains("config") -> Icons.Default.Settings
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
