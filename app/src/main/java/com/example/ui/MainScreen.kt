package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.PinLockScreen
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransferMoneyDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NoInternetScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.NetworkObserver

@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkObserver = remember { NetworkObserver(context.applicationContext) }
    val isOnline by networkObserver.isOnline.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val selectedTx by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog Visibility states
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Automatically trigger cloud fetch/sync whenever internet is available or reconnected
    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.triggerCloudSync()
        }
    }

    // 0. Strict Internet Connection Check: Block access when offline
    if (!isOnline) {
        NoInternetScreen(
            onRetry = {
                networkObserver.refresh()
            }
        )
        return
    }

    // 1. Authentication Check (Login / Register / Forgot Password)
    if (!prefs.isLoggedIn) {
        var authRoute by remember { mutableStateOf(Screen.Login.route) }
        when (authRoute) {
            Screen.Login.route -> {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { authRoute = Screen.Register.route },
                    onNavigateToForgotPassword = { authRoute = Screen.ForgotPassword.route }
                )
            }
            Screen.Register.route -> {
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { authRoute = Screen.Login.route }
                )
            }
            Screen.ForgotPassword.route -> {
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onNavigateBack = { authRoute = Screen.Login.route }
                )
            }
        }
        return
    }

    // 2. PIN Lock Protection
    if (prefs.isPinLockEnabled && !isAppUnlocked) {
        PinLockScreen(
            onPinEntered = { pin ->
                viewModel.unlockWithPin(pin)
            }
        )
        return
    }

    // Detail Dialog Overlay
    selectedTx?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            currencySymbol = prefs.currency,
            onDismiss = { viewModel.selectTransaction(null) },
            onDelete = { viewModel.deleteTransaction(tx) },
            onDuplicate = {
                viewModel.duplicateTransaction(tx)
                viewModel.selectTransaction(null)
            }
        )
    }

    // Dialogs
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = categories,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { catName, amount, period, threshold ->
                viewModel.addBudget(catName, amount, period, threshold)
                showAddBudgetDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, type, bal ->
                viewModel.addAccount(name, type, bal)
                showAddAccountDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferMoneyDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount ->
                viewModel.transferBetweenAccounts(fromId, toId, amount)
                showTransferDialog = false
            }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple(Screen.Home, Icons.Default.Home, "Home"),
                    Triple(Screen.Transactions, Icons.Default.ReceiptLong, "Transactions"),
                    Triple(Screen.Add, Icons.Default.Add, "Add"),
                    Triple(Screen.Reports, Icons.Default.BarChart, "Reports"),
                    Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                )

                navItems.forEach { (screen, icon, label) ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentRoute == Screen.Home.route,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Add.route) },
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag("home_fab_add")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddExpense = { navController.navigate(Screen.Add.route) },
                        onNavigateToAddIncome = { navController.navigate(Screen.Add.route) },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onOpenTransferDialog = { showTransferDialog = true },
                        onOpenAddBudgetDialog = { showAddBudgetDialog = true },
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )
                }

                composable(Screen.Add.route) {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onTransactionSaved = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Reports.route) {
                    ReportsScreen(viewModel = viewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenAddBudgetDialog = { showAddBudgetDialog = true },
                        onOpenAddAccountDialog = { showAddAccountDialog = true },
                        onLogout = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
