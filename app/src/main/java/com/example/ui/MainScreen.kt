package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FinanceError
import com.example.ui.theme.IndigoPrimary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.ToastType
import com.example.ui.components.TopToastHost
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransferMoneyDialog
import com.example.ui.components.rememberTopToastState
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NoInternetScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
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

    val selectedTx by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }

    val topToastState = rememberTopToastState()

    // Dialog Visibility states
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            val toastType = when {
                msg.contains("failed", ignoreCase = true) || msg.contains("error", ignoreCase = true) || msg.contains("offline", ignoreCase = true) -> ToastType.ERROR
                msg.contains("success", ignoreCase = true) || msg.contains("saved", ignoreCase = true) || msg.contains("welcome", ignoreCase = true) || msg.contains("updated", ignoreCase = true) -> ToastType.SUCCESS
                else -> ToastType.INFO
            }
            topToastState.show(msg, toastType)
        }
    }

    // Automatically trigger cloud fetch/sync whenever internet is available or reconnected
    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.triggerCloudSync()
        }
    }

    // 0. Splash Screen on App Launch
    if (showSplash) {
        SplashScreen(
            onTimeout = {
                showSplash = false
            }
        )
        return
    }

    // 1. Strict Internet Connection Check: Block access when offline
    if (!isOnline) {
        NoInternetScreen(
            onRetry = {
                networkObserver.refresh()
            }
        )
        return
    }

    // 2. Authentication Check (Login / Register / Forgot Password)
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

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var addScreenIsExpense by remember { mutableStateOf(true) }

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {},
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple(Screen.Home, Icons.Default.Home, "Home"),
                    Triple(Screen.Transactions, Icons.Default.ReceiptLong, "Expenses"),
                    Triple(Screen.Add, Icons.Default.Add, "Add"),
                    Triple(Screen.Reports, Icons.Default.BarChart, "Reports"),
                    Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                )

                navItems.forEachIndexed { index, (screen, icon, label) ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (screen == Screen.Add) {
                                addScreenIsExpense = true
                            }
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
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
                visible = pagerState.currentPage == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        addScreenIsExpense = true
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
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
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddExpense = {
                            addScreenIsExpense = true
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onNavigateToAddIncome = {
                            addScreenIsExpense = false
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onNavigateToTransactions = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onNavigateToSettings = {
                            coroutineScope.launch { pagerState.animateScrollToPage(4) }
                        },
                        onOpenTransferDialog = { showTransferDialog = true },
                        onOpenAddBudgetDialog = { showAddBudgetDialog = true },
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )

                    1 -> TransactionsScreen(
                        viewModel = viewModel,
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) }
                    )

                    2 -> AddTransactionScreen(
                        viewModel = viewModel,
                        initialIsExpense = addScreenIsExpense,
                        onTransactionSaved = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )

                    3 -> ReportsScreen(viewModel = viewModel)

                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenAddBudgetDialog = { showAddBudgetDialog = true },
                        onOpenAddAccountDialog = { showAddAccountDialog = true },
                        onLogout = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            TopToastHost(
                state = topToastState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
