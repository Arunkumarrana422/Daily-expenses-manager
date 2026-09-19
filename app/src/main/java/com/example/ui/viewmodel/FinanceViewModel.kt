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
    val syncStatus: String = "SYNCED"
)

data class FilterCriteria(
    val searchQuery: String = "",
    val categoryFilter: String = "All",
    val paymentMethodFilter: String = "All",
    val typeFilter: String = "ALL", // "ALL", "EXPENSE", "INCOME"
    val sortOrder: String = "NEWEST", // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val dateRange: String = "ALL" // "ALL", "TODAY", "THIS_WEEK", "THIS_MONTH"
)

data class FinanceDashboardSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val todayExpense: Double = 0.0,
    val thisWeekExpense: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val savings: Double = 0.0,
    val savingsPercentage: Double = 0.0
)

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    // Filter and search state
    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    // Reports timeframe
    private val _reportsPeriod = MutableStateFlow("MONTHLY") // DAILY, WEEKLY, MONTHLY, YEARLY
    val reportsPeriod: StateFlow<String> = _reportsPeriod.asStateFlow()

    // Selected transaction for details dialog
    private val _selectedTransaction = MutableStateFlow<TransactionItem?>(null)
    val selectedTransaction: StateFlow<TransactionItem?> = _selectedTransaction.asStateFlow()

    // App Lock PIN Verification state
    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

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

    init {
        // Trigger cloud sync to load all data from Firebase on startup
        viewModelScope.launch {
            repository.syncWithCloud()
        }

        // Observe preferences to see if PIN lock is active on app startup
        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                if (prefs.isPinLockEnabled && prefs.pinCodeHash.isNotEmpty()) {
                    // lock app initially until user inputs PIN
                    _isAppUnlocked.value = false
                } else {
                    _isAppUnlocked.value = true
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

        val netSavings = if (monthInc > monthExp) monthInc - monthExp else 0.0
        val savingsPct = if (monthInc > 0) (netSavings / monthInc) * 100.0 else 0.0

        FinanceDashboardSummary(
            totalBalance = balance,
            totalIncome = totalInc,
            totalExpense = totalExp,
            todayExpense = todayExp,
            thisWeekExpense = weekExp,
            thisMonthExpense = monthExp,
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
                syncStatus = it.syncStatus
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
                syncStatus = it.syncStatus
            )
        }
        (expItems + incItems).sortedWith(
            compareByDescending<TransactionItem> { it.date }
                .thenByDescending { it.time }
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
                else -> true
            }

            matchesQuery && matchesCategory && matchesPayment && matchesType && matchesMin && matchesMax && matchesDate
        }.let { list ->
            when (filter.sortOrder) {
                "OLDEST" -> list.sortedWith(compareBy<TransactionItem> { it.date }.thenBy { it.time })
                "HIGHEST" -> list.sortedByDescending { it.amount }
                "LOWEST" -> list.sortedBy { it.amount }
                else -> list.sortedWith(compareByDescending<TransactionItem> { it.date }.thenByDescending { it.time })
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

    fun resetFilters() {
        _filterCriteria.value = FilterCriteria()
    }

    fun setReportsPeriod(period: String) {
        _reportsPeriod.value = period
    }

    fun selectTransaction(transaction: TransactionItem?) {
        _selectedTransaction.value = transaction
    }

    fun unlockWithPin(pin: String): Boolean {
        val currentHash = userPreferences.value.pinCodeHash
        if (pin == currentHash) {
            _isAppUnlocked.value = true
            return true
        }
        return false
    }

    fun resetPinViaEmail(onResult: (Boolean, String?) -> Unit) {
        val email = userPreferences.value.userEmail
        if (email.isBlank()) {
            onResult(false, "No account email found.")
            return
        }
        viewModelScope.launch {
            repository.sendPasswordReset(email).onSuccess {
                _snackbarMessage.emit("PIN reset instructions sent to $email")
                onResult(true, "PIN reset instructions sent to $email")
            }.onFailure { err ->
                onResult(false, err.message ?: "Could not send reset email")
            }
        }
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
        receiptUri: String? = null
    ) {
        viewModelScope.launch {
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
            repository.addExpense(expense)
            _snackbarMessage.emit("Expense of $amount saved successfully!")
        }
    }

    fun addIncome(
        amount: Double,
        source: String,
        paymentMethod: String,
        accountId: Long,
        note: String,
        date: String,
        time: String
    ) {
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
            repository.addIncome(income)
            _snackbarMessage.emit("Income of $amount added successfully!")
        }
    }

    fun deleteTransaction(item: TransactionItem) {
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
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _snackbarMessage.emit("Budget removed")
        }
    }

    // Accounts & Transfer
    fun addAccount(name: String, type: String, initialBalance: Double) {
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
        viewModelScope.launch {
            repository.transferBetweenAccounts(fromId, toId, amount)
            _snackbarMessage.emit("Transferred $amount successfully!")
        }
    }

    // Recurring
    fun addRecurringExpense(title: String, amount: Double, frequency: String, dueDate: String) {
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
        viewModelScope.launch {
            repository.contributeToGoal(id, amount)
            _snackbarMessage.emit("Added $amount to goal!")
        }
    }

    // Settings actions
    fun setCurrency(currency: String) {
        viewModelScope.launch { repository.setCurrency(currency) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setPinLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            repository.setPinLock(enabled, pin)
            _snackbarMessage.emit(if (enabled) "PIN Lock activated" else "PIN Lock disabled")
        }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
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

    fun register(name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
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
        viewModelScope.launch {
            repository.logout()
            _snackbarMessage.emit("You have been signed out")
        }
    }

    fun setNotificationSetting(type: String, enabled: Boolean) {
        viewModelScope.launch {
            when (type) {
                "DAILY" -> repository.setDailyReminder(enabled)
                "BUDGET" -> repository.setBudgetWarning(enabled)
                "RECURRING" -> repository.setRecurringAlert(enabled)
                "MONTHLY" -> repository.setMonthlySummary(enabled)
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }
}
