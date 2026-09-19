package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoGradientEnd
import com.example.ui.theme.IndigoGradientStart
import com.example.utils.CurrencyFormatter

@Composable
fun FinanceBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("balance_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(IndigoGradientStart, IndigoGradientEnd)
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Active",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = CurrencyFormatter.format(totalBalance, currencySymbol, showDecimals = false),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("total_balance_text")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Income & Expense Breakdown Row with Glass Effect Border
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Income",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = CurrencyFormatter.format(totalIncome, currencySymbol, showDecimals = false),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White // Clear bright white for visibility in both light & dark modes
                            )
                        }
                    }

                    // Expense
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Expense",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = CurrencyFormatter.format(totalExpense, currencySymbol, showDecimals = false),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444) // Distinct clear red for expenses
                            )
                        }
                    }
                }
            }
        }
    }
}
