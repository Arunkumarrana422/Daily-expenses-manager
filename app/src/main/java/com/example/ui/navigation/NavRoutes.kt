package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Transactions : Screen("transactions", "Transactions")
    object Add : Screen("add", "Add")
    object Reports : Screen("reports", "Reports")
    object Settings : Screen("settings", "Settings")
}
