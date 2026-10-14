package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
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
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.ToastType
import com.example.ui.components.TopToastHost
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransferMoneyDialog
import com.example.ui.components.rememberTopToastState
import com.example.ui.viewmodel.TransactionItem
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntroScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NoInternetScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.TermsAndConditionsScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.NetworkConnectionState
import com.example.utils.NetworkObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkObserver = remember { NetworkObserver(context.applicationContext) }
    val connectionState by networkObserver.connectionState.collectAsStateWithLifecycle()
    val isOnline by networkObserver.isOnline.collectAsStateWithLifecycle()

    // Dispose network observer properly when MainScreen leaves composition
    DisposableEffect(networkObserver) {
        onDispose {
            networkObserver.cleanup()
        }
    }

    // Lifecycle observer: automatically re-verify connectivity whenever app resumes from background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                networkObserver.verifyConnectivity()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val selectedTx by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val isSplashDismissed by viewModel.isSplashDismissed.collectAsStateWithLifecycle()

    val topToastState = rememberTopToastState()

    // Dialog Visibility states
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var activeLegalScreen by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    // Collect snackbar messages and show both TopToast and Android Toast
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            val toastType = when {
                msg.contains("failed", ignoreCase = true) ||
                msg.contains("error", ignoreCase = true) ||
                msg.contains("offline", ignoreCase = true) ||
                msg.contains("internet", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) -> ToastType.ERROR
                msg.contains("success", ignoreCase = true) ||
                msg.contains("saved", ignoreCase = true) ||
                msg.contains("welcome", ignoreCase = true) ||
                msg.contains("updated", ignoreCase = true) -> ToastType.SUCCESS
                else -> ToastType.INFO
            }
            topToastState.show(msg, toastType)
            android.widget.Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var lastObservedState by remember { mutableStateOf<NetworkConnectionState?>(null) }

    // Strict internet connection handling: Application does NOT work without internet
    LaunchedEffect(connectionState) {
        if (lastObservedState == null) {
            lastObservedState = connectionState
            if (connectionState is NetworkConnectionState.Connected) {
                viewModel.triggerCloudSync(showToastOnNoInternet = false)
            } else if (connectionState is NetworkConnectionState.NoInternet) {
                topToastState.show("No internet connection", ToastType.ERROR)
                android.widget.Toast.makeText(context, "No internet connection. Internet is required to use this application.", Toast.LENGTH_SHORT).show()
            } else if (connectionState is NetworkConnectionState.FirestoreUnavailable) {
                topToastState.show("Firebase server unavailable", ToastType.ERROR)
                android.widget.Toast.makeText(context, "Firebase server unavailable. Please tap retry.", Toast.LENGTH_SHORT).show()
            }
        } else if (lastObservedState != connectionState) {
            val wasBlocked = lastObservedState !is NetworkConnectionState.Connected
            lastObservedState = connectionState
            if (connectionState is NetworkConnectionState.Connected) {
                if (wasBlocked) {
                    topToastState.show("Connected! Syncing data with cloud...", ToastType.SUCCESS)
                    android.widget.Toast.makeText(context, "Connected! Syncing data with cloud...", Toast.LENGTH_SHORT).show()
                }
                viewModel.triggerCloudSync(showToastOnNoInternet = false)
            } else if (connectionState is NetworkConnectionState.NoInternet) {
                topToastState.show("No internet connection", ToastType.ERROR)
                android.widget.Toast.makeText(context, "No internet connection. Internet is required to use this application.", Toast.LENGTH_SHORT).show()
            } else if (connectionState is NetworkConnectionState.FirestoreUnavailable) {
                topToastState.show("Database server unavailable", ToastType.ERROR)
                android.widget.Toast.makeText(context, "Database server unavailable. Please tap retry.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 1. BLOCKING NO INTERNET / SERVER UNAVAILABLE SCREEN
    // If device is offline or Firestore is unreachable, block the entire application immediately
    if (connectionState is NetworkConnectionState.NoInternet || connectionState is NetworkConnectionState.FirestoreUnavailable) {
        NoInternetScreen(
            connectionState = connectionState,
            onRetry = {
                networkObserver.verifyConnectivity()
            }
        )
        return
    }

    // 2. Splash Screen on Initial App Launch Only (survives dark/light mode switches)
    if (!isSplashDismissed) {
        SplashScreen(
            onTimeout = {
                viewModel.dismissSplash()
            }
        )
        return
    }

    // 3. If splash dismissed but still checking connection, keep blocked on NoInternetScreen
    if (connectionState !is NetworkConnectionState.Connected) {
        NoInternetScreen(
            connectionState = connectionState,
            onRetry = {
                networkObserver.verifyConnectivity()
            }
        )
        return
    }

    // 0. Intro Screen for new customers
    if (!prefs.hasSeenIntro) {
        IntroScreen(
            viewModel = viewModel,
            onFinished = {
                viewModel.setHasSeenIntro(true)
            }
        )
        return
    }

    // 0.1 Legal Screens Overlay
    if (activeLegalScreen == "privacy") {
        PrivacyPolicyScreen(onBack = { activeLegalScreen = null })
        return
    }
    if (activeLegalScreen == "terms") {
        TermsAndConditionsScreen(onBack = { activeLegalScreen = null })
        return
    }

    // 1. Authentication Check (Google Sign-In Only)
    if (!prefs.isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {},
            onOpenPrivacyPolicy = { activeLegalScreen = "privacy" },
            onOpenTermsAndConditions = { activeLegalScreen = "terms" }
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
            onEdit = {
                editingTransaction = tx
                viewModel.selectTransaction(null)
            }
        )
    }

    // Edit Transaction Dialog Overlay
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            currencySymbol = prefs.currency,
            onDismiss = { editingTransaction = null },
            onConfirm = { amount, catName, catIcon, catColor, paymentMethod, date, time, note, onFinish ->
                viewModel.updateTransaction(
                    item = tx,
                    amount = amount,
                    categoryName = catName,
                    categoryIcon = catIcon,
                    categoryColor = catColor,
                    paymentMethod = paymentMethod,
                    date = date,
                    time = time,
                    note = note,
                    onComplete = { success ->
                        onFinish(success)
                        if (success) {
                            editingTransaction = null
                        }
                    }
                )
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

    val homeListState = rememberLazyListState()
    val transactionsListState = rememberLazyListState()
    val reportsListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    var fabVisible by remember { mutableStateOf(true) }
    var previousPagerPage by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        val current = pagerState.currentPage
        if (current != previousPagerPage) {
            when (current) {
                0 -> homeListState.scrollToItem(0)
                1 -> transactionsListState.scrollToItem(0)
                3 -> reportsListState.scrollToItem(0)
                4 -> settingsListState.scrollToItem(0)
            }
        }
        previousPagerPage = current
    }

    var backPressedTime by remember { mutableStateOf(0L) }
    BackHandler(enabled = pagerState.currentPage == 0) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedTime = currentTime
            topToastState.show("Press back again to exit", ToastType.INFO)
        }
    }

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
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
                visible = pagerState.currentPage == 0 && fabVisible,
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
                .padding(top = innerPadding.calculateTopPadding())
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
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) },
                        listState = homeListState,
                        onScrollDirectionChanged = { isDown -> fabVisible = !isDown }
                    )

                    1 -> TransactionsScreen(
                        viewModel = viewModel,
                        onTransactionClick = { tx -> viewModel.selectTransaction(tx) },
                        listState = transactionsListState
                    )

                    2 -> AddTransactionScreen(
                        viewModel = viewModel,
                        initialIsExpense = addScreenIsExpense,
                        onTransactionSaved = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )

                    3 -> ReportsScreen(
                        viewModel = viewModel,
                        listState = reportsListState
                    )

                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenAddBudgetDialog = { showAddBudgetDialog = true },
                        onOpenAddAccountDialog = { showAddAccountDialog = true },
                        onLogout = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        listState = settingsListState
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
