package com.example.ui.screens

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileImageHelper
import com.example.ui.components.ToastType
import com.example.ui.components.TopToastHost
import com.example.ui.components.rememberTopToastState
import com.example.data.local.entity.BudgetEntity
import com.example.ui.theme.FinanceError
import com.example.ui.theme.FinanceSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.ExportUtils
import com.example.utils.NotificationHelper
import com.example.utils.NotificationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onOpenAddBudgetDialog: () -> Unit,
    onOpenAddAccountDialog: () -> Unit = {},
    onLogout: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurring.collectAsStateWithLifecycle()
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allIncomes by viewModel.incomes.collectAsStateWithLifecycle()

    val topToastState = rememberTopToastState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            topToastState.show("Notification permission allowed!", ToastType.SUCCESS)
        } else {
            topToastState.show("Notification permission denied", ToastType.ERROR)
        }
    }

    fun checkAndRequestNotificationPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onGranted()
        }
    }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmationInput by remember { mutableStateOf("") }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            val base64 = ProfileImageHelper.processUriToBase64(context, uri)
            if (base64 != null) {
                viewModel.updateProfilePhoto(base64) { success, err ->
                    isUploadingPhoto = false
                    if (success) {
                        topToastState.show("Profile photo updated successfully", ToastType.SUCCESS)
                    } else {
                        topToastState.show(err ?: "Failed to update profile photo", ToastType.ERROR)
                    }
                }
            } else {
                isUploadingPhoto = false
                topToastState.show("Could not process selected image", ToastType.ERROR)
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceError),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_sign_out_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text("Edit Name", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter your display name:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        singleLine = true,
                        label = { Text("Display Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newNameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.updateDisplayName(trimmed) { success, err ->
                                if (success) {
                                    topToastState.show("Name updated successfully", ToastType.SUCCESS)
                                } else {
                                    topToastState.show(err ?: "Failed to update name", ToastType.ERROR)
                                }
                            }
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.testTag("save_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }



    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
                .testTag("settings_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile & User Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            name = prefs.userDisplayName,
                            profilePhotoBase64 = prefs.profilePhotoBase64,
                            size = 56.dp,
                            fontSize = 22.sp,
                            showEditBadge = false,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("settings_profile_avatar")
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prefs.userDisplayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = {
                                        newNameInput = prefs.userDisplayName
                                        showEditNameDialog = true
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(start = 4.dp)
                                        .testTag("edit_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (prefs.userEmail.isNotBlank()) prefs.userEmail else "Personal Account",
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (prefs.profilePhotoBase64.isNotBlank()) "Tap photo to change" else "Tap photo to upload",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

        // 2. Preferences: Currency & Theme
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
                        text = "General Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Currency Selector
                    Text("Currency Symbol", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("₹" to "₹ INR", "$" to "$ USD", "€" to "€ EUR", "£" to "£ GBP").forEach { (sym, label) ->
                            val isSelected = prefs.currency == sym
                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

                            Surface(
                                onClick = { viewModel.setCurrency(sym) },
                                shape = RoundedCornerShape(10.dp),
                                color = containerColor,
                                contentColor = contentColor,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("currency_chip_$sym")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Theme selector removed - app follows system theme automatically
                }
            }
        }



        // 5. Budgets Management
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = "Budgets", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budgets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = onOpenAddBudgetDialog,
                            modifier = Modifier.testTag("open_add_budget_dialog_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (budgets.isEmpty()) {
                        Text(
                            text = "No budgets set yet. Tap '+ New' to set spending limits.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        budgets.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${b.categoryName ?: "Overall"} Budget", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Alert threshold at ${(b.warningThreshold * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyFormatter.format(b.amount, prefs.currency),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { budgetToDelete = b },
                                        modifier = Modifier.size(28.dp).testTag("delete_budget_${b.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Notifications Preferences
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
                        text = "Notifications & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    NotificationSettingRow(
                        title = "Daily Evening Reminder",
                        subtitle = "Alert at 9:00 PM to log daily expenses",
                        checked = prefs.dailyReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                checkAndRequestNotificationPermission {
                                    viewModel.setNotificationSetting("DAILY", true)
                                    NotificationHelper.scheduleDailyEveningReminder(context)
                                    topToastState.show("Daily 9:00 PM reminder active & scheduled!", ToastType.SUCCESS)
                                }
                            } else {
                                viewModel.setNotificationSetting("DAILY", false)
                                NotificationHelper.cancelDailyEveningReminder(context)
                                topToastState.show("Daily Reminder disabled", ToastType.INFO)
                            }
                        }
                    )

                    NotificationSettingRow(
                        title = "Budget Overspending Alert",
                        subtitle = "Notify when reaching warning threshold",
                        checked = prefs.budgetWarningEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                checkAndRequestNotificationPermission {
                                    viewModel.setNotificationSetting("BUDGET", true)
                                    topToastState.show("Budget Overspending Alert activated!", ToastType.SUCCESS)
                                }
                            } else {
                                viewModel.setNotificationSetting("BUDGET", false)
                                topToastState.show("Budget Overspending Alert disabled", ToastType.INFO)
                            }
                        }
                    )
                }
            }
        }

        // 7. Sign Out Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, FinanceError.copy(alpha = 0.35f)) else BorderStroke(1.dp, FinanceError.copy(alpha = 0.2f))
            ) {
                Button(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(44.dp)
                        .testTag("settings_logout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinanceError.copy(alpha = 0.12f),
                        contentColor = FinanceError
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = FinanceError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceError
                        )
                    }
                }
            }
        }

        // 8. Delete Account Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, FinanceError.copy(alpha = 0.35f)) else BorderStroke(1.dp, FinanceError.copy(alpha = 0.2f))
            ) {
                Button(
                    onClick = {
                        deleteConfirmationInput = ""
                        showDeleteAccountDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(44.dp)
                        .testTag("settings_delete_account_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinanceError.copy(alpha = 0.18f),
                        contentColor = FinanceError
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Account",
                            tint = FinanceError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinanceError
                        )
                    }
                }
            }
        }
    }

    TopToastHost(
        state = topToastState,
        modifier = Modifier.align(Alignment.TopCenter)
    )

    // Budget Delete Confirmation Dialog
    if (budgetToDelete != null) {
        val budget = budgetToDelete!!
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete the budget for '${budget.categoryName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBudget(budget)
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        var isDeletingAccount by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = FinanceError
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will permanently delete all your data from Firestore and remove your account authentication. This action CANNOT be undone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Type 'DELETE' to confirm:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = deleteConfirmationInput,
                        onValueChange = { deleteConfirmationInput = it },
                        singleLine = true,
                        placeholder = { Text("DELETE") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("delete_account_confirm_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteConfirmationInput.trim().equals("DELETE", ignoreCase = true)) {
                            isDeletingAccount = true
                            viewModel.deleteAccount { success, err ->
                                isDeletingAccount = false
                                showDeleteAccountDialog = false
                                if (success) {
                                    onLogout()
                                } else {
                                    topToastState.show(err ?: "Failed to delete account", ToastType.ERROR)
                                }
                            }
                        }
                    },
                    enabled = !isDeletingAccount && deleteConfirmationInput.trim().equals("DELETE", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceError),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text(if (isDeletingAccount) "Deleting..." else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationInput = ""
                        showDeleteAccountDialog = false
                    },
                    enabled = !isDeletingAccount
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
private fun NotificationSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
