package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currencySymbol: String = "₹", showDecimals: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val pattern = if (showDecimals) "#,##0.00" else "#,##0"
        val formatter = DecimalFormat(pattern, symbols)
        return "$currencySymbol${formatter.format(amount)}"
    }
}
