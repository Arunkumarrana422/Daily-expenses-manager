package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FinanceSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.utils.CurrencyFormatter

@Composable
fun SummaryCardsRow(
    todayExpense: Double,
    thisWeekExpense: Double,
    thisMonthExpense: Double,
    savings: Double,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMiniCard(
                label = "Today's Expense",
                amount = todayExpense,
                currencySymbol = currencySymbol,
                accentColor = Color(0xFFEF5350),
                modifier = Modifier
                    .weight(1f)
                    .testTag("today_expense_card")
            )
            SummaryMiniCard(
                label = "This Week",
                amount = thisWeekExpense,
                currencySymbol = currencySymbol,
                accentColor = Color(0xFFFFA726),
                modifier = Modifier
                    .weight(1f)
                    .testTag("this_week_expense_card")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMiniCard(
                label = "This Month",
                amount = thisMonthExpense,
                currencySymbol = currencySymbol,
                accentColor = IndigoPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("this_month_expense_card")
            )
            SummaryMiniCard(
                label = "Savings",
                amount = savings,
                currencySymbol = currencySymbol,
                accentColor = FinanceSuccess,
                modifier = Modifier
                    .weight(1f)
                    .testTag("savings_summary_card")
            )
        }
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    amount: Double,
    currencySymbol: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = CurrencyFormatter.format(amount, currencySymbol, showDecimals = false),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 17.sp
            )
        }
    }
}
