package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.ChartCategorySlice
import com.example.ui.components.DonutChart
import com.example.ui.components.SpendingTrendBar
import com.example.ui.components.SpendingTrendChart
import com.example.ui.theme.FinanceSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateTimeUtils
import com.example.utils.ExportUtils
import com.example.utils.PdfExportUtils
import java.time.LocalDate

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val reportsPeriod by viewModel.reportsPeriod.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val currency = prefs.currency

    // Filter expenses based on period
    val periodExpenses = remember(expenses, reportsPeriod) {
        when (reportsPeriod) {
            "DAILY" -> expenses.filter { it.date == DateTimeUtils.getTodayString() }
            "WEEKLY" -> expenses.filter { DateTimeUtils.isDateInCurrentWeek(it.date) }
            "YEARLY" -> expenses.filter { it.date.startsWith(LocalDate.now().year.toString()) }
            else -> expenses.filter { DateTimeUtils.isDateInCurrentMonth(it.date) }
        }
    }

    val periodIncomes = remember(incomes, reportsPeriod) {
        when (reportsPeriod) {
            "DAILY" -> incomes.filter { it.date == DateTimeUtils.getTodayString() }
            "WEEKLY" -> incomes.filter { DateTimeUtils.isDateInCurrentWeek(it.date) }
            "YEARLY" -> incomes.filter { it.date.startsWith(LocalDate.now().year.toString()) }
            else -> incomes.filter { DateTimeUtils.isDateInCurrentMonth(it.date) }
        }
    }

    val totalSpent = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
    val totalIncome = remember(periodIncomes) { periodIncomes.sumOf { it.amount } }

    // Category distribution slices
    val categorySlices = remember(periodExpenses, totalSpent) {
        val grouped = periodExpenses.groupBy { it.categoryName }
        grouped.map { (catName, items) ->
            val catTotal = items.sumOf { it.amount }
            val pct = if (totalSpent > 0) (catTotal / totalSpent * 100.0).toFloat() else 0f
            val firstItem = items.first()
            ChartCategorySlice(
                categoryName = catName,
                icon = firstItem.categoryIcon,
                amount = catTotal,
                percentage = pct,
                color = Color(firstItem.categoryColor)
            )
        }.sortedByDescending { it.amount }
    }

    // Spending Trend Bars for the past 7 days
    val trendBars = remember(expenses) {
        val today = LocalDate.now()
        val bars = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.toString()
            val dayName = if (daysAgo == 0) "Today" else date.dayOfWeek.name.take(3)
            val daySum = expenses.filter { it.date == dateStr }.sumOf { it.amount }
            SpendingTrendBar(label = dayName, amount = daySum)
        }
        val maxVal = bars.maxOfOrNull { it.amount } ?: 0.0
        bars.map { it.copy(isPeak = it.amount > 0 && it.amount == maxVal) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        val breakdown = categorySlices.map { Triple(it.categoryName, it.amount, it.percentage.toDouble()) }
                        val file = PdfExportUtils.generateFinancialReportPdf(
                            context = context,
                            title = "Financial Report Summary",
                            period = reportsPeriod,
                            totalIncome = totalIncome,
                            totalExpenses = totalSpent,
                            currency = currency,
                            categoryBreakdown = breakdown
                        )
                        file?.let {
                            PdfExportUtils.sharePdf(context, it, "Share Financial Report PDF")
                        }
                    },
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report PDF", tint = IndigoPrimary)
                }
            }
        }

        // Timeframe Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "DAILY" to "Daily",
                    "WEEKLY" to "Weekly",
                    "MONTHLY" to "Monthly",
                    "YEARLY" to "Yearly"
                ).forEach { (period, label) ->
                    FilterChip(
                        selected = reportsPeriod == period,
                        onClick = { viewModel.setReportsPeriod(period) },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = IndigoPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("period_chip_$period")
                    )
                }
            }
        }

        // Summary Card: Income vs Expense
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Total Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(totalIncome, currency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceSuccess
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Total Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(totalSpent, currency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }

        // Donut Chart: Category Spending
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    DonutChart(
                        slices = categorySlices,
                        totalAmount = totalSpent,
                        currencySymbol = currency
                    )
                }
            }
        }

        // Spending Trend Chart (Weekly Activity)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Recent Daily Spending Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SpendingTrendChart(
                        bars = trendBars,
                        currencySymbol = currency
                    )
                }
            }
        }

        // Top Spending Categories Ranked List
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Top Spending Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categorySlices.isEmpty()) {
                        Text(
                            text = "No category data available for this timeframe.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categorySlices.forEachIndexed { index, slice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = slice.color.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(slice.categoryName, slice.icon),
                                                contentDescription = slice.categoryName,
                                                tint = slice.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${index + 1}. ${slice.categoryName}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format("%.1f", slice.percentage)}% of total",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyFormatter.format(slice.amount, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
