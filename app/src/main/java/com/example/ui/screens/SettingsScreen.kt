package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.FinanceError
import com.example.ui.theme.FinanceSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.CurrencyFormatter
import com.example.utils.ExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onOpenAddBudgetDialog: () -> Unit,
    onOpenAddAccountDialog: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurring.collectAsStateWithLifecycle()
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allIncomes by viewModel.incomes.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val topToastState = rememberTopToastState()
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var showUpdatePasswordDialog by remember { mutableStateOf(false) }
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

    // Update Password Dialog
    if (showUpdatePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var showCurPass by remember { mutableStateOf(false) }
        var showNewPass by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var isUpdatingPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingPassword) showUpdatePasswordDialog = false },
            title = {
                Text("Update Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = FinanceError,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            passwordError = null
                        },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = if (showCurPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurPass = !showCurPass }) {
                                Icon(
                                    if (showCurPass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("current_password_input")
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordError = null
                        },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPass = !showNewPass }) {
                                Icon(
                                    if (showNewPass) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = null
                        },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "New password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        isUpdatingPassword = true
                        viewModel.updatePassword(currentPassword, newPassword) { success, err ->
                            isUpdatingPassword = false
                            if (success) {
                                showUpdatePasswordDialog = false
                                topToastState.show("Password updated successfully", ToastType.SUCCESS)
                            } else {
                                passwordError = err ?: "Failed to update password"
                            }
                        }
                    },
                    enabled = !isUpdatingPassword && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.testTag("submit_update_password_button")
                ) {
                    Text(if (isUpdatingPassword) "Updating..." else "Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdatePasswordDialog = false },
                    enabled = !isUpdatingPassword
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (prefs.isPinLockEnabled) "Disable PIN Lock" else "Set 4-Digit App PIN") },
            text = {
                Column {
                    Text("Enter 4-digit numeric code to protect your financial data:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_setup_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prefs.isPinLockEnabled) {
                            viewModel.setPinLock(false, "")
                        } else if (pinInput.length == 4) {
                            viewModel.setPinLock(true, pinInput)
                        }
                        showPinDialog = false
                        pinInput = ""
                    },
                    modifier = Modifier.testTag("save_pin_setup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile & User Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            name = prefs.userDisplayName,
                            profilePhotoBase64 = prefs.profilePhotoBase64,
                            size = 56.dp,
                            fontSize = 22.sp,
                            showEditBadge = true,
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
                                        tint = IndigoPrimary,
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
                                color = IndigoPrimary,
                                modifier = Modifier
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(top = 2.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                showLogoutConfirmDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("settings_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = FinanceError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sign Out",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinanceError,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

        // 2. Preferences: Currency & Theme
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "General Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Currency Selector
                    Text("Currency Symbol", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("₹" to "INR (₹)", "$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)").forEach { (sym, label) ->
                            FilterChip(
                                selected = prefs.currency == sym,
                                onClick = { viewModel.setCurrency(sym) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("currency_chip_$sym")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme Selector
                    Text("Theme Appearance", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (theme, label) ->
                            FilterChip(
                                selected = prefs.themeMode == theme,
                                onClick = { viewModel.setThemeMode(theme) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("theme_chip_$theme")
                            )
                        }
                    }
                }
            }
        }

        // 3. App Security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Security & Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-digit PIN toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "PIN", tint = IndigoPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("4-Digit PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Require PIN to open app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = prefs.isPinLockEnabled,
                            onCheckedChange = { showPinDialog = true },
                            modifier = Modifier.testTag("pin_lock_switch")
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Update Password Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showUpdatePasswordDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("update_password_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Update Password",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Update Password", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Change your login password", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Update",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 4. Accounts & Balances Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "Accounts", tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accounts & Balances",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = onOpenAddAccountDialog,
                            modifier = Modifier.testTag("add_account_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(acc.type, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = CurrencyFormatter.format(acc.balance, prefs.currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 5. Budgets Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = "Budgets", tint = IndigoPrimary, modifier = Modifier.size(20.dp))
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

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    .padding(vertical = 6.dp),
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
                                        onClick = { viewModel.deleteBudget(b) },
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
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Notifications & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationSettingRow(
                        title = "Daily Evening Reminder",
                        subtitle = "Alert at 9:00 PM to log daily expenses",
                        checked = prefs.dailyReminderEnabled,
                        onCheckedChange = { viewModel.setNotificationSetting("DAILY", it) }
                    )

                    NotificationSettingRow(
                        title = "Budget Overspending Alert",
                        subtitle = "Notify when reaching warning threshold",
                        checked = prefs.budgetWarningEnabled,
                        onCheckedChange = { viewModel.setNotificationSetting("BUDGET", it) }
                    )

                    NotificationSettingRow(
                        title = "Recurring Payment Due Alerts",
                        subtitle = "Alerts 2 days before bills are due",
                        checked = prefs.recurringAlertEnabled,
                        onCheckedChange = { viewModel.setNotificationSetting("RECURRING", it) }
                    )
                }
            }
        }

        // 9. Cloud Sync & Backup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Cloud Backup & Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val lastSyncText = if (prefs.lastSyncTimestamp > 0) {
                        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(prefs.lastSyncTimestamp))
                    } else {
                        "Never"
                    }

                    Text(
                        text = "Last synced: $lastSyncText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_cloud_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync with Cloud")
                    }
                }
            }
        }

        // 10. About Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Daily Expense Manager",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0 • True Native Android (Kotlin + Room)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    TopToastHost(state = topToastState)
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
