package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FilterCriteria

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    initialCriteria: FilterCriteria,
    availableCategories: List<String>,
    onApply: (FilterCriteria) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var criteria by remember(initialCriteria) { mutableStateOf(initialCriteria) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Transaction Type Filter
            Text(
                text = "Transaction Type",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (type, label) ->
                    FilterChip(
                        selected = criteria.typeFilter == type,
                        onClick = { criteria = criteria.copy(typeFilter = type) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = IndigoPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Range Filter
            Text(
                text = "Date Range",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Any Time",
                    "TODAY" to "Today",
                    "THIS_WEEK" to "This Week",
                    "THIS_MONTH" to "This Month"
                ).forEach { (range, label) ->
                    FilterChip(
                        selected = criteria.dateRange == range,
                        onClick = { criteria = criteria.copy(dateRange = range) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Order
            Text(
                text = "Sort By",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "NEWEST" to "Newest First",
                    "OLDEST" to "Oldest First",
                    "HIGHEST" to "Highest Amount",
                    "LOWEST" to "Lowest Amount"
                ).forEach { (sort, label) ->
                    FilterChip(
                        selected = criteria.sortOrder == sort,
                        onClick = { criteria = criteria.copy(sortOrder = sort) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Filter
            Text(
                text = "Category",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf("All") + availableCategories
                cats.take(12).forEach { cat ->
                    FilterChip(
                        selected = criteria.categoryFilter == cat,
                        onClick = { criteria = criteria.copy(categoryFilter = cat) },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method
            Text(
                text = "Payment Method",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Cash", "UPI", "Debit Card", "Credit Card", "Bank Transfer").forEach { method ->
                    FilterChip(
                        selected = criteria.paymentMethodFilter == method,
                        onClick = { criteria = criteria.copy(paymentMethodFilter = method) },
                        label = { Text(method) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        criteria = FilterCriteria()
                        onReset()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_filters_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        onApply(criteria)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_filters_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
