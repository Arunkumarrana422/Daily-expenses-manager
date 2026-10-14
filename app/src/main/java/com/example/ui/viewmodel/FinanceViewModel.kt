package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import com.example.data.local.entity.RecurringExpenseEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.preferences.AppUserPreferences
import com.example.data.remote.SyncState
import com.example.data.repository.FinanceRepository
import com.example.utils.DateTimeUtils
import com.example.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.ui.components.showSystemTopToast

data class TransactionItem(
    val id: Long,
    val isExpense: Boolean,
    val amount: Double,
    val title: String,
    val categoryName: String = "",
    val categoryIcon: String,
    val categoryColor: Long,
    val paymentMethod: String,
    val date: String,
    val time: String,
    val note: String,
    val receiptUri: String? = null,
    val accountId: Long = 1,
    val syncStatus: String = "SYNCED",
    val createdAt: Long = 0L
)

data class FilterCriteria(
    val searchQuery: String = "",
    val categoryFilter: String = "All",
    val paymentMethodFilter: String = "All",
    val typeFilter: String = "ALL", // "ALL", "EXPENSE", "INCOME"
    val sortOrder: String = "NEWEST", // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val dateRange: String = "ALL", // "ALL", "TODAY", "THIS_WEEK", "THIS_MONTH", "LAST_MONTH", "CUSTOM_MONTH"
    val customYear: Int? = null,
    val customMonth: Int? = null
)

data class FinanceDashboardSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val todayExpense: Double = 0.0,
    val thisWeekExpense: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val lastMonthExpense: Double = 0.0,
    val lastMonthIncome: Double = 0.0,
    val savings: Double = 0.0,
    val savingsPercentage: Double = 0.0
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val context: android.content.Context
) : ViewModel() {

    // Filter and search state
    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    // Reports timeframe: DAILY, WEEKLY, MONTHLY, LAST_MONTH, YEARLY
    private val _reportsPeriod = MutableStateFlow("MONTHLY")
    val reportsPeriod: StateFlow<String> = _reportsPeriod.asStateFlow()

    // Reports Month Offset (0 = Current Month, -1 = Previous Month, -2 = 2 months ago, etc.)
    private val _reportsMonthOffset = MutableStateFlow(0)
    val reportsMonthOffset: StateFlow<Int> = _reportsMonthOffset.asStateFlow()

    // Selected transaction for details dialog
    private val _selectedTransaction = MutableStateFlow<TransactionItem?>(null)
    val selectedTransaction: StateFlow<TransactionItem?> = _selectedTransaction.asStateFlow()

    // App Lock PIN Verification state
    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Splash screen state (shown once on app launch, survives theme/dark mode changes)
    private val _isSplashDismissed = MutableStateFlow(false)
    val isSplashDismissed: StateFlow<Boolean> = _isSplashDismissed.asStateFlow()

    fun dismissSplash() {
        _isSplashDismissed.value = true
    }

    // Feedback message channel
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomes: StateFlow<List<IncomeEntity>> = repository.allIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurring: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<AppUserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUserPreferences())

    val syncState: StateFlow<SyncState> = repository.syncState

    private val _isLoadingTransactions = MutableStateFlow(true)
    val isLoadingTransactions: StateFlow<Boolean> = _isLoadingTransactions.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                if (repository.isOnline()) {
                    repository.syncWithCloud()
                }
            } finally {
                _isLoadingTransactions.value = false
            }
        }

        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                if (prefs.dailyReminderEnabled) {
                    NotificationHelper.scheduleDailyEveningReminder(context)
                }
            }
        }
    }

    // Combined Dashboard summary
    val dashboardSummary: StateFlow<FinanceDashboardSummary> = combine(
        expenses,
        incomes
    ) { expList, incList ->
        val totalInc = incList.sumOf { it.amount }
        val totalExp = expList.sumOf { it.amount }
        val balance = totalInc - totalExp

        val todayStr = DateTimeUtils.getTodayString()
        val todayExp = expList.filter { it.date == todayStr }.sumOf { it.amount }
        val weekExp = expList.filter { DateTimeUtils.isDateInCurrentWeek(it.date) }.sumOf { it.amount }
        val monthExp = expList.filter { DateTimeUtils.isDateInCurrentMonth(it.date) }.sumOf { it.amount }
        val monthInc = incList.filter { DateTimeUtils.isDateInCurrentMonth(it.date) }.sumOf { it.amount }
        val lastMonthExp = expList.filter { DateTimeUtils.isDateInPreviousMonth(it.date) }.sumOf { it.amount }
        val lastMonthInc = incList.filter { DateTimeUtils.isDateInPreviousMonth(it.date) }.sumOf { it.amount }

        val netSavings = if (monthInc > monthExp) monthInc - monthExp else 0.0
        val savingsPct = if (monthInc > 0) (netSavings / monthInc) * 100.0 else 0.0

        FinanceDashboardSummary(
            totalBalance = balance,
            totalIncome = totalInc,
            totalExpense = totalExp,
            todayExpense = todayExp,
            thisWeekExpense = weekExp,
            thisMonthExpense = monthExp,
            lastMonthExpense = lastMonthExp,
            lastMonthIncome = lastMonthInc,
            savings = netSavings,
            savingsPercentage = savingsPct
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinanceDashboardSummary()
    )

    // All unified transaction items
    val allTransactions: StateFlow<List<TransactionItem>> = combine(
        expenses,
        incomes
    ) { expList, incList ->
        val expItems = expList.map {
            TransactionItem(
                id = it.id,
                isExpense = true,
                amount = it.amount,
                title = if (it.note.isNotBlank()) it.note else it.categoryName,
                categoryName = it.categoryName,
                categoryIcon = it.categoryIcon,
                categoryColor = it.categoryColor,
                paymentMethod = it.paymentMethod,
                date = it.date,
                time = it.time,
                note = it.note,
                receiptUri = it.receiptUri,
                accountId = it.accountId,
                syncStatus = it.syncStatus,
                createdAt = it.createdAt
            )
        }
        val incItems = incList.map {
            TransactionItem(
                id = it.id,
                isExpense = false,
                amount = it.amount,
                title = if (it.note.isNotBlank()) it.note else it.source,
                categoryName = it.source,
                categoryIcon = "payments",
                categoryColor = 0xFF2E7D32,
                paymentMethod = it.paymentMethod,
                date = it.date,
                time = it.time,
                note = it.note,
                receiptUri = null,
                accountId = it.accountId,
                syncStatus = it.syncStatus,
                createdAt = it.createdAt
            )
        }
        (expItems + incItems).sortedWith(
            compareByDescending<TransactionItem> { it.date }
                .thenByDescending { DateTimeUtils.parseTimeToMinutes(it.time) }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        allTransactions,
        filterCriteria
    ) { txList, filter ->
        txList.filter { tx ->
            // Search query match
            val matchesQuery = filter.searchQuery.isBlank() ||
                tx.title.contains(filter.searchQuery, ignoreCase = true) ||
                tx.note.contains(filter.searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(filter.searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(filter.searchQuery)

            // Category filter
            val matchesCategory = filter.categoryFilter == "All" || tx.categoryName.equals(filter.categoryFilter, ignoreCase = true)

            // Payment method filter
            val matchesPayment = filter.paymentMethodFilter == "All" || tx.paymentMethod.equals(filter.paymentMethodFilter, ignoreCase = true)

            // Type filter
            val matchesType = when (filter.typeFilter) {
                "EXPENSE" -> tx.isExpense
                "INCOME" -> !tx.isExpense
                else -> true
            }

            // Min & Max amount
            val matchesMin = filter.minAmount == null || tx.amount >= filter.minAmount
            val matchesMax = filter.maxAmount == null || tx.amount <= filter.maxAmount

            // Date range
            val matchesDate = when (filter.dateRange) {
                "TODAY" -> tx.date == DateTimeUtils.getTodayString()
                "THIS_WEEK" -> DateTimeUtils.isDateInCurrentWeek(tx.date)
                "THIS_MONTH" -> DateTimeUtils.isDateInCurrentMonth(tx.date)
                "LAST_MONTH", "PREVIOUS_MONTH" -> DateTimeUtils.isDateInPreviousMonth(tx.date)
                "CUSTOM_MONTH" -> {
                    if (filter.customYear != null && filter.customMonth != null) {
                        DateTimeUtils.isDateInMonth(tx.date, filter.customYear, filter.customMonth)
                    } else {
                        true
                    }
                }
                else -> true
            }

            matchesQuery && matchesCategory && matchesPayment && matchesType && matchesMin && matchesMax && matchesDate
        }.let { list ->
            when (filter.sortOrder) {
                "OLDEST" -> list.sortedWith(
                    compareBy<TransactionItem> { it.date }
                        .thenBy { DateTimeUtils.parseTimeToMinutes(it.time) }
                        .thenBy { it.createdAt }
                        .thenBy { it.id }
                )
                "HIGHEST" -> list.sortedByDescending { it.amount }
                "LOWEST" -> list.sortedBy { it.amount }
                else -> list.sortedWith(
                    compareByDescending<TransactionItem> { it.date }
                        .thenByDescending { DateTimeUtils.parseTimeToMinutes(it.time) }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun updateSearchQuery(query: String) {
        _filterCriteria.value = _filterCriteria.value.copy(searchQuery = query)
    }

    fun updateFilter(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
    }

    fun setQuickDateFilter(range: String) {
        _filterCriteria.value = _filterCriteria.value.copy(dateRange = range, customYear = null, customMonth = null)
    }

    fun resetFilters() {
        _filterCriteria.value = FilterCriteria()
    }

    fun setReportsPeriod(period: String) {
        _reportsPeriod.value = period
    }

    fun previousReportMonth() {
        _reportsMonthOffset.value -= 1
    }

    fun nextReportMonth() {
        if (_reportsMonthOffset.value < 0) {
            _reportsMonthOffset.value += 1
        }
    }

    fun resetReportMonth() {
        _reportsMonthOffset.value = 0
    }

    fun selectTransaction(transaction: TransactionItem?) {
        _selectedTransaction.value = transaction
    }

    fun isOnline(): Boolean = repository.isOnline()

    fun showNoInternetToast() {
        // No-op: banner is already displayed at the top, so toast popups are omitted
    }

    fun checkOnlineOrShowToast(): Boolean {
        return repository.isOnline()
    }

    fun addExpense(
        amount: Double,
        categoryId: Long,
        categoryName: String,
        categoryIcon: String,
        categoryColor: Long,
        paymentMethod: String,
        accountId: Long,
        note: String,
        date: String,
        time: String,
        receiptUri: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!checkOnlineOrShowToast()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            // Pre-calculate current monthly totals BEFORE adding the new expense to avoid double counting
            val currentCatExpenses = expenses.value.filter {
                it.categoryName.equals(categoryName, ignoreCase = true) && DateTimeUtils.isDateInCurrentMonth(it.date)
            }.sumOf { it.amount }
            val totalCatSpent = currentCatExpenses + amount

            val currentOverallExpenses = expenses.value.filter {
                DateTimeUtils.isDateInCurrentMonth(it.date)
            }.sumOf { it.amount }
            val totalOverallSpent = currentOverallExpenses + amount

            val expense = ExpenseEntity(
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                paymentMethod = paymentMethod,
                accountId = accountId,
                note = note,
                date = date,
                time = time,
                receiptUri = receiptUri
            )
            val result = repository.addExpenseWithFirestoreCheck(expense)
            if (result.isSuccess) {
                _snackbarMessage.emit("Expense of $amount saved successfully!")
                val prefs = userPreferences.value
                if (prefs.budgetWarningEnabled) {
                    checkBudgetAndNotify(categoryName, totalCatSpent, totalOverallSpent)
                }
                onComplete(true)
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to save entry to Firestore")
                onComplete(false)
            }
        }
    }

    private fun checkBudgetAndNotify(categoryName: String, totalSpent: Double, totalOverall: Double) {
        val currentBudgets = budgets.value
        if (currentBudgets.isEmpty()) return

        // 1. Specific Category Budget
        val categoryBudget = currentBudgets.find { it.categoryName.equals(categoryName, ignoreCase = true) }
        if (categoryBudget != null) {
            val warningLimit = categoryBudget.amount * categoryBudget.warningThreshold

            if (totalSpent >= categoryBudget.amount) {
                NotificationHelper.showNotification(
                    context,
                    "🚨 Budget Exceeded Alert!",
                    "You exceeded your $categoryName budget! Spent: ₹${totalSpent.toInt()} / Limit: ₹${categoryBudget.amount.toInt()}"
                )
                viewModelScope.launch {
                    _snackbarMessage.emit("Alert: $categoryName budget limit exceeded!")
                }
            } else if (totalSpent >= warningLimit) {
                val pct = (categoryBudget.warningThreshold * 100).toInt()
                NotificationHelper.showNotification(
                    context,
                    "⚠️ Budget Overspending Alert",
                    "You have reached ${pct}% of your $categoryName budget! Spent: ₹${totalSpent.toInt()} / Limit: ₹${categoryBudget.amount.toInt()}"
                )
                viewModelScope.launch {
                    _snackbarMessage.emit("Warning: Reached ${pct}% of $categoryName budget!")
                }
            }
        }

        // 2. Overall Monthly Budget (categoryName == "All" or "Overall")
        val overallBudget = currentBudgets.find {
            it.categoryName.equals("All", ignoreCase = true) || it.categoryName.equals("Overall", ignoreCase = true)
        }
        if (overallBudget != null) {
            val warningLimit = overallBudget.amount * overallBudget.warningThreshold

            if (totalOverall >= overallBudget.amount) {
                NotificationHelper.showNotification(
                    context,
                    "🚨 Overall Budget Exceeded!",
                    "You have exceeded your overall monthly budget limit of ₹${overallBudget.amount.toInt()}!"
                )
            } else if (totalOverall >= warningLimit) {
                val pct = (overallBudget.warningThreshold * 100).toInt()
                NotificationHelper.showNotification(
                    context,
                    "⚠️ Overall Budget Warning",
                    "You have reached ${pct}% of your monthly overall budget (₹${totalOverall.toInt()} / ₹${overallBudget.amount.toInt()})."
                )
            }
        }
    }

    fun addIncome(
        amount: Double,
        source: String,
        paymentMethod: String,
        accountId: Long,
        note: String,
        date: String,
        time: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!checkOnlineOrShowToast()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val income = IncomeEntity(
                amount = amount,
                source = source,
                paymentMethod = paymentMethod,
                accountId = accountId,
                note = note,
                date = date,
                time = time
            )
            val result = repository.addIncomeWithFirestoreCheck(income)
            if (result.isSuccess) {
                _snackbarMessage.emit("Income of $amount added successfully!")
                onComplete(true)
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to save entry to Firestore")
                onComplete(false)
            }
        }
    }

    fun updateTransaction(
        item: TransactionItem,
        amount: Double,
        categoryName: String,
        categoryIcon: String = item.categoryIcon,
        categoryColor: Long = item.categoryColor,
        paymentMethod: String,
        date: String,
        time: String,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!checkOnlineOrShowToast()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            if (item.isExpense) {
                val expense = expenses.value.find { it.id == item.id }
                if (expense != null) {
                    val updatedExpense = expense.copy(
                        amount = amount,
                        categoryName = categoryName.ifBlank { item.title },
                        categoryIcon = categoryIcon,
                        categoryColor = categoryColor,
                        paymentMethod = paymentMethod,
                        date = date,
                        time = time,
                        note = note
                    )
                    val result = repository.updateExpenseWithFirestoreCheck(
                        expense = updatedExpense,
                        oldAmount = expense.amount,
                        oldAccountId = expense.accountId
                    )
                    if (result.isSuccess) {
                        _snackbarMessage.emit("Expense updated successfully")
                        onComplete(true)
                    } else {
                        _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to update expense")
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } else {
                val income = incomes.value.find { it.id == item.id }
                if (income != null) {
                    val updatedIncome = income.copy(
                        amount = amount,
                        source = categoryName.ifBlank { item.title },
                        paymentMethod = paymentMethod,
                        date = date,
                        time = time,
                        note = note
                    )
                    val result = repository.updateIncomeWithFirestoreCheck(
                        income = updatedIncome,
                        oldAmount = income.amount,
                        oldAccountId = income.accountId
                    )
                    if (result.isSuccess) {
                        _snackbarMessage.emit("Income updated successfully")
                        onComplete(true)
                    } else {
                        _snackbarMessage.emit(result.exceptionOrNull()?.message ?: "Failed to update income")
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            }
        }
    }

    fun deleteTransaction(item: TransactionItem) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            if (item.isExpense) {
                repository.deleteExpenseById(item.id)
            } else {
                val inc = incomes.value.find { it.id == item.id }
                if (inc != null) {
                    repository.deleteIncome(inc)
                }
            }
            if (_selectedTransaction.value?.id == item.id) {
                _selectedTransaction.value = null
            }
            _snackbarMessage.emit("Transaction deleted")
        }
    }

    fun duplicateTransaction(item: TransactionItem) {
        if (!checkOnlineOrShowToast()) return
        val today = DateTimeUtils.getTodayString()
        val nowTime = DateTimeUtils.getCurrentTimeString()
        if (item.isExpense) {
            addExpense(
                amount = item.amount,
                categoryId = 1,
                categoryName = item.categoryName.ifBlank { item.title },
                categoryIcon = item.categoryIcon,
                categoryColor = item.categoryColor,
                paymentMethod = item.paymentMethod,
                accountId = item.accountId,
                note = if (item.note.isNotBlank()) "${item.note} (Copy)" else "Duplicated transaction",
                date = today,
                time = nowTime,
                receiptUri = item.receiptUri
            )
        } else {
            addIncome(
                amount = item.amount,
                source = item.categoryName.ifBlank { item.title },
                paymentMethod = item.paymentMethod,
                accountId = item.accountId,
                note = if (item.note.isNotBlank()) "${item.note} (Copy)" else "Duplicated income",
                date = today,
                time = nowTime
            )
        }
    }

    // Categories
    fun addCustomCategory(name: String, icon: String, color: Long, type: String) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    name = name,
                    icon = icon,
                    color = color,
                    type = type
                )
            )
            _snackbarMessage.emit("Category '$name' added!")
        }
    }

    // Budget
    fun addBudget(categoryName: String, amount: Double, period: String, threshold: Float) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.addBudget(
                BudgetEntity(
                    categoryName = categoryName,
                    amount = amount,
                    period = period,
                    warningThreshold = threshold
                )
            )
            _snackbarMessage.emit("Budget of $amount set for $categoryName")
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _snackbarMessage.emit("Budget removed")
        }
    }

    // Accounts & Transfer
    fun addAccount(name: String, type: String, initialBalance: Double) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = initialBalance
                )
            )
            _snackbarMessage.emit("Account '$name' created!")
        }
    }

    fun transferBetweenAccounts(fromId: Long, toId: Long, amount: Double) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.transferBetweenAccounts(fromId, toId, amount)
            _snackbarMessage.emit("Transferred $amount successfully!")
        }
    }

    // Recurring
    fun addRecurringExpense(title: String, amount: Double, frequency: String, dueDate: String) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.addRecurring(
                RecurringExpenseEntity(
                    title = title,
                    amount = amount,
                    categoryId = 1,
                    categoryName = title,
                    frequency = frequency,
                    nextDueDate = dueDate,
                    enabled = true
                )
            )
            _snackbarMessage.emit("Recurring bill '$title' saved!")
        }
    }

    // Savings Goals
    fun addSavingsGoal(name: String, targetAmount: Double, deadline: String) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.addGoal(
                SavingsGoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    deadline = deadline
                )
            )
            _snackbarMessage.emit("Savings goal '$name' created!")
        }
    }

    fun contributeToGoal(id: Long, amount: Double) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.contributeToGoal(id, amount)
            _snackbarMessage.emit("Added $amount to goal!")
        }
    }

    // Settings actions
    fun setHasSeenIntro(seen: Boolean) {
        viewModelScope.launch { repository.setHasSeenIntro(seen) }
    }

    fun setCurrency(currency: String) {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch { repository.setCurrency(currency) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.login(email, pass)
            result.onSuccess { user ->
                _snackbarMessage.emit("Welcome back, ${user.displayName}!")
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Authentication failed")
            }
        }
    }

    fun googleLoginWithToken(
        idToken: String,
        email: String,
        displayName: String,
        photoUrl: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken, displayName, email, photoUrl)
            result.onSuccess { user ->
                _snackbarMessage.emit("Welcome, ${user.displayName}!")
                onResult(true, null)
            }.onFailure { err ->
                _snackbarMessage.emit("Google Sign-In failed: ${err.message}")
                onResult(false, err.message ?: "Google Sign-In failed")
            }
        }
    }

    fun googleLogin(email: String, displayName: String, photoUrl: String = "", onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            try {
                repository.setLoggedInUser(email.hashCode().toString(), displayName, email, photoUrl)
                _snackbarMessage.emit("Welcome, $displayName!")
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.deleteUserAccount()
            result.onSuccess {
                _snackbarMessage.emit("Account deleted successfully")
                onResult(true, null)
            }.onFailure { err ->
                val msg = err.message ?: "Failed to delete account"
                _snackbarMessage.emit(msg)
                onResult(false, msg)
            }
        }
    }

    fun register(name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.register(name, email, pass)
            result.onSuccess { user ->
                _snackbarMessage.emit("Account created for ${user.displayName}!")
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Registration failed")
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.sendPasswordReset(email)
            result.onSuccess {
                _snackbarMessage.emit("Password reset instructions sent to $email")
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Could not send reset email")
            }
        }
    }

    fun updateProfilePhoto(base64: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.updateProfilePhoto(base64)
            result.onSuccess {
                _snackbarMessage.emit("Profile photo updated successfully")
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update profile photo")
            }
        }
    }

    fun updateDisplayName(name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.updateDisplayName(name)
            result.onSuccess {
                _snackbarMessage.emit("Name updated successfully")
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update name")
            }
        }
    }

    fun updatePassword(currentPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        if (!checkOnlineOrShowToast()) {
            onResult(false, "No internet connection")
            return
        }
        viewModelScope.launch {
            val result = repository.updatePassword(currentPass, newPass)
            result.onSuccess {
                _snackbarMessage.emit("Password updated successfully")
                onResult(true, null)
            }.onFailure { err ->
                val errorMsg = if (err.message?.contains("credential", ignoreCase = true) == true ||
                                   err.message?.contains("password", ignoreCase = true) == true ||
                                   err.message?.contains("mismatch", ignoreCase = true) == true ||
                                   err.message?.contains("expired", ignoreCase = true) == true) {
                    "Current password incorrect"
                } else {
                    err.message ?: "Failed to update password"
                }
                onResult(false, errorMsg)
            }
        }
    }

    fun logout() {
        if (!checkOnlineOrShowToast()) return
        viewModelScope.launch {
            repository.logout()
            _snackbarMessage.emit("You have been signed out")
        }
    }

    fun setNotificationSetting(type: String, enabled: Boolean) {
        viewModelScope.launch {
            when (type) {
                "DAILY" -> {
                    repository.setDailyReminder(enabled)
                    if (enabled) {
                        NotificationHelper.scheduleDailyEveningReminder(context)
                    } else {
                        NotificationHelper.cancelDailyEveningReminder(context)
                    }
                }
                "BUDGET" -> repository.setBudgetWarning(enabled)
                "RECURRING" -> repository.setRecurringAlert(enabled)
                "MONTHLY" -> repository.setMonthlySummary(enabled)
            }
        }
    }

    fun enableAllNotifications(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.enableAllNotifications()
            onComplete()
        }
    }

    fun triggerCloudSync(showToastOnNoInternet: Boolean = false) {
        viewModelScope.launch {
            if (!repository.isOnline()) {
                if (showToastOnNoInternet) {
                    _snackbarMessage.emit("No internet connection")
                }
                return@launch
            }
            try {
                _isLoadingTransactions.value = true
                repository.syncWithCloud()
            } finally {
                _isLoadingTransactions.value = false
            }
        }
    }
}
