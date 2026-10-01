package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.DateTimeUtils
import com.example.utils.ExportUtils
import com.example.utils.PdfExportUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val reportsPeriod by viewModel.reportsPeriod.collectAsStateWithLifecycle()
    val reportsMonthOffset by viewModel.reportsMonthOffset.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val currency = prefs.currency

    // Calculate targeted month based on offset
    val targetMonthDate = remember(reportsMonthOffset) {
        LocalDate.now().plusMonths(reportsMonthOffset.toLong())
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val currentFormattedMonth = remember(targetMonthDate) { targetMonthDate.format(monthFormatter) }

    // Filter expenses based on period
    val periodExpenses = remember(expenses, reportsPeriod, reportsMonthOffset) {
        when (reportsPeriod) {
            "DAILY" -> expenses.filter { it.date == DateTimeUtils.getTodayString() }
            "WEEKLY" -> expenses.filter { DateTimeUtils.isDateInCurrentWeek(it.date) }
            "LAST_MONTH" -> expenses.filter { DateTimeUtils.isDateInPreviousMonth(it.date) }
            "YEARLY" -> expenses.filter { it.date.startsWith(LocalDate.now().year.toString()) }
            else -> expenses.filter {
                DateTimeUtils.isDateInMonth(it.date, targetMonthDate.year, targetMonthDate.monthValue)
            }
        }
    }

    val periodIncomes = remember(incomes, reportsPeriod, reportsMonthOffset) {
        when (reportsPeriod) {
            "DAILY" -> incomes.filter { it.date == DateTimeUtils.getTodayString() }
            "WEEKLY" -> incomes.filter { DateTimeUtils.isDateInCurrentWeek(it.date) }
            "LAST_MONTH" -> incomes.filter { DateTimeUtils.isDateInPreviousMonth(it.date) }
            "YEARLY" -> incomes.filter { incomes.let { _ -> true } && it.date.startsWith(LocalDate.now().year.toString()) }
            else -> incomes.filter {
                DateTimeUtils.isDateInMonth(it.date, targetMonthDate.year, targetMonthDate.monthValue)
            }
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
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
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
                        val reportTitle = when (reportsPeriod) {
                            "DAILY" -> "Daily Report - ${DateTimeUtils.getFormattedToday()}"
                            "WEEKLY" -> "Weekly Report"
                            "LAST_MONTH" -> "Previous Month Report (${DateTimeUtils.getPreviousMonthDisplay()})"
                            "YEARLY" -> "Yearly Report - ${LocalDate.now().year}"
                            else -> "Monthly Report - $currentFormattedMonth"
                        }
                        val file = PdfExportUtils.generateComprehensiveReportPdf(
                            context = context,
                            title = reportTitle,
                            period = reportsPeriod,
                            totalIncome = totalIncome,
                            totalExpenses = totalSpent,
                            currency = currency,
                            categoryBreakdown = breakdown,
                            expenses = periodExpenses,
                            incomes = periodIncomes
                        )
                        file?.let {
                            PdfExportUtils.sharePdf(context, it, "Share Financial Report PDF")
                        }
                    },
                    modifier = Modifier.testTag("share_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Timeframe Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "DAILY" to "Daily",
                    "WEEKLY" to "Weekly",
                    "MONTHLY" to "This Month",
                    "LAST_MONTH" to "Previous Month (${DateTimeUtils.getPreviousMonthShortName()})",
                    "YEARLY" to "Yearly"
                ).forEach { (period, label) ->
                    val isSelected = reportsPeriod == period
                    Surface(
                        onClick = {
                            viewModel.setReportsPeriod(period)
                            if (period == "MONTHLY") {
                                viewModel.resetReportMonth()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("period_chip_$period")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Month Navigator (When viewing Monthly reports)
        if (reportsPeriod == "MONTHLY") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousReportMonth() },
                            modifier = Modifier.testTag("prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentFormattedMonth,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (reportsMonthOffset == 0) {
                                Text(
                                    text = "Current Month",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (reportsMonthOffset == -1) {
                                Text(
                                    text = "Previous Month",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "${-reportsMonthOffset} months ago",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.nextReportMonth() },
                            enabled = reportsMonthOffset < 0,
                            modifier = Modifier.testTag("next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = if (reportsMonthOffset < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
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
