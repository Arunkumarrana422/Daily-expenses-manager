package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import com.example.ui.components.AnimatedAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import com.example.ui.components.CustomDatePickerDialog
import com.example.ui.components.CustomTimePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    initialIsExpense: Boolean = true,
    onTransactionSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpense by remember(initialIsExpense) { mutableStateOf(initialIsExpense) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(DateTimeUtils.getTodayString()) }
    var selectedTime by remember { mutableStateOf(DateTimeUtils.getCurrentTimeString()) }
    var selectedPaymentMethod by remember { mutableStateOf("UPI") }
    var isSaving by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val incomeCategories = categories.filter { it.type == "INCOME" }

    val currentCategoryList = if (isExpense) expenseCategories else incomeCategories
    var selectedCategory by remember(isExpense, currentCategoryList) {
        mutableStateOf(currentCategoryList.firstOrNull())
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("category") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        CustomDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = java.time.Instant.ofEpochMilli(millis)
                    val date = java.time.LocalDate.ofInstant(instant, java.time.ZoneId.of("UTC"))
                    selectedDate = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }
                showDatePicker = false
            },
            datePickerState = datePickerState
        )
    }

    if (showTimePicker) {
        val currentTime = java.time.LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
            is24Hour = false,
        )
        CustomTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                val hour = timePickerState.hour
                val minute = timePickerState.minute
                val localTime = java.time.LocalTime.of(hour, minute)
                selectedTime = localTime.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                showTimePicker = false
            },
            timePickerState = timePickerState
        )
    }

    val quickAmounts = listOf(100, 500, 1000, 2000)
    val paymentMethods = listOf("Cash", "UPI", "Debit Card", "Credit Card", "Bank Transfer", "Wallet")

    if (showAddCategoryDialog) {
        AnimatedAlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select Icon:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("restaurant", "shopping_cart", "directions_car", "home", "phone", "payments", "account_balance", "star", "build", "place").forEach { iconKey ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (newCategoryIcon == iconKey) IndigoPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { newCategoryIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (newCategoryIcon == iconKey) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!viewModel.checkOnlineOrShowToast()) return@Button
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCategoryName.trim(),
                                icon = newCategoryIcon,
                                color = 0xFF5C6BC0,
                                type = if (isExpense) "EXPENSE" else "INCOME"
                            )
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.testTag("save_category_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .testTag("add_transaction_screen"),
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
                    text = "Add Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. Expense / Income Switch Tabs (Sliding Pill Switch matching Image 1)
        item {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .testTag("transaction_type_tabs")
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / 2
                val indicatorOffset by animateDpAsState(
                    targetValue = if (isExpense) 0.dp else tabWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tabIndicator"
                )

                // Sliding pill background indicator
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                )

                // Clickable tabs row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expense Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isExpense = true
                                selectedCategory = expenseCategories.firstOrNull()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isExpense) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Income Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isExpense = false
                                selectedCategory = incomeCategories.firstOrNull()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (!isExpense) FinanceSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Amount Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isExpense) "How much did you spend?" else "How much did you receive?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = prefs.currency,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) Color(0xFFD32F2F) else FinanceSuccess
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .heightIn(min = 60.dp)
                                .testTag("amount_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (quick in quickAmounts) {
                            Surface(
                                onClick = {
                                    val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (currentVal + quick).toInt().toString()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("quick_amount_$quick")
                            ) {
                                Text(
                                    text = "+${prefs.currency}$quick",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Item Name / What did you spend on? (Clear purpose field)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isExpense) "What did you spend on? (Item / Detail)" else "What is this income for? (Source / Detail)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(if (isExpense) "Expense Title / Item Name" else "Income Title / Source") },
                        placeholder = {
                            Text(
                                text = if (isExpense) "e.g. Milk, Petrol, Coffee" else "e.g. Salary, Freelance",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Item Title",
                                tint = IndigoPrimary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_note_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // 4. Category Selector Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("add_custom_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCategoryList.forEach { category ->
                            val isSelected = selectedCategory?.id == category.id
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) IndigoPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary) else null,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { selectedCategory = category }
                                    .testTag("category_chip_${category.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(category.name, category.icon),
                                        contentDescription = category.name,
                                        tint = if (isSelected) IndigoPrimary else Color(category.color),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Payment Method
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = IndigoPrimary
                                ),
                                modifier = Modifier.testTag("payment_method_$method")
                            )
                        }
                    }
                }
            }
        }

        // 5. Date, Time & Notes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date and Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { showDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Date (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transaction_date_input"),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { showTimePicker = true }
                        ) {
                            OutlinedTextField(
                                value = selectedTime,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Time (12-hr)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transaction_time_input"),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Save Button
        item {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            val isFormValid = amount > 0.0 && selectedCategory != null

            Button(
                onClick = {
                    if (!viewModel.checkOnlineOrShowToast()) {
                        return@Button
                    }
                    if (isFormValid && !isSaving) {
                        isSaving = true
                        val cat = selectedCategory!!
                        if (isExpense) {
                            viewModel.addExpense(
                                amount = amount,
                                categoryId = cat.id,
                                categoryName = cat.name,
                                categoryIcon = cat.icon,
                                categoryColor = cat.color,
                                paymentMethod = selectedPaymentMethod,
                                accountId = 1,
                                note = noteText.trim(),
                                date = selectedDate,
                                time = selectedTime
                            ) { success ->
                                isSaving = false
                                if (success) {
                                    amountText = ""
                                    noteText = ""
                                    selectedDate = DateTimeUtils.getTodayString()
                                    selectedTime = DateTimeUtils.getCurrentTimeString()
                                    onTransactionSaved()
                                }
                            }
                        } else {
                            viewModel.addIncome(
                                amount = amount,
                                source = cat.name,
                                paymentMethod = selectedPaymentMethod,
                                accountId = 1,
                                note = noteText.trim(),
                                date = selectedDate,
                                time = selectedTime
                            ) { success ->
                                isSaving = false
                                if (success) {
                                    amountText = ""
                                    noteText = ""
                                    selectedDate = DateTimeUtils.getTodayString()
                                    selectedTime = DateTimeUtils.getCurrentTimeString()
                                    onTransactionSaved()
                                }
                            }
                        }
                    }
                },
                enabled = isFormValid && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) IndigoPrimary else FinanceSuccess
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExpense) "Save Expense" else "Save Income",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
