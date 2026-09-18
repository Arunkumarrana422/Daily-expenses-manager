package com.example.data.repository

import com.example.data.auth.AuthManager
import com.example.data.auth.AuthUser
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.IncomeDao
import com.example.data.local.dao.RecurringExpenseDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import com.example.data.local.entity.RecurringExpenseEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.preferences.AppUserPreferences
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.remote.FirebaseSyncManager
import com.example.data.remote.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val syncManager: FirebaseSyncManager,
    val authManager: AuthManager
) {
    // Flow sources
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allIncomes: Flow<List<IncomeEntity>> = incomeDao.getAllIncomes()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allRecurring: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAllRecurring()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val userPreferences: Flow<AppUserPreferences> = preferencesDataStore.preferencesFlow
    val syncState: StateFlow<SyncState> = syncManager.syncState

    // Expenses operations
    suspend fun addExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insertExpense(expense)
        // Deduct from account balance if account exists
        accountDao.adjustBalance(expense.accountId, -expense.amount)
        return id
    }

    suspend fun updateExpense(expense: ExpenseEntity, oldAmount: Double, oldAccountId: Long) {
        expenseDao.updateExpense(expense)
        // Revert old account deduction and apply new
        accountDao.adjustBalance(oldAccountId, oldAmount)
        accountDao.adjustBalance(expense.accountId, -expense.amount)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
        accountDao.adjustBalance(expense.accountId, expense.amount)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    fun getExpenseById(id: Long): Flow<ExpenseEntity?> = expenseDao.getExpenseById(id)

    // Incomes operations
    suspend fun addIncome(income: IncomeEntity): Long {
        val id = incomeDao.insertIncome(income)
        accountDao.adjustBalance(income.accountId, income.amount)
        return id
    }

    suspend fun updateIncome(income: IncomeEntity, oldAmount: Double, oldAccountId: Long) {
        incomeDao.updateIncome(income)
        accountDao.adjustBalance(oldAccountId, -oldAmount)
        accountDao.adjustBalance(income.accountId, income.amount)
    }

    suspend fun deleteIncome(income: IncomeEntity) {
        incomeDao.deleteIncome(income)
        accountDao.adjustBalance(income.accountId, -income.amount)
    }

    fun getIncomeById(id: Long): Flow<IncomeEntity?> = incomeDao.getIncomeById(id)

    // Categories
    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Budgets
    suspend fun addBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)
    suspend fun deleteBudgetById(id: Long) = budgetDao.deleteBudgetById(id)

    // Accounts
    suspend fun addAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun transferBetweenAccounts(fromId: Long, toId: Long, amount: Double) {
        accountDao.adjustBalance(fromId, -amount)
        accountDao.adjustBalance(toId, amount)
    }

    // Recurring
    suspend fun addRecurring(item: RecurringExpenseEntity): Long = recurringExpenseDao.insertRecurring(item)
    suspend fun updateRecurring(item: RecurringExpenseEntity) = recurringExpenseDao.updateRecurring(item)
    suspend fun deleteRecurring(item: RecurringExpenseEntity) = recurringExpenseDao.deleteRecurring(item)

    // Savings Goals
    suspend fun addGoal(goal: SavingsGoalEntity): Long = savingsGoalDao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingsGoalEntity) = savingsGoalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteGoal(goal)
    suspend fun contributeToGoal(id: Long, amount: Double) = savingsGoalDao.addContribution(id, amount)

    // Settings
    suspend fun setCurrency(currency: String) = preferencesDataStore.setCurrency(currency)
    suspend fun setThemeMode(mode: String) = preferencesDataStore.setThemeMode(mode)
    suspend fun setPinLock(enabled: Boolean, hash: String = "") = preferencesDataStore.setPinLock(enabled, hash)
    suspend fun setDailyReminder(enabled: Boolean) = preferencesDataStore.setDailyReminder(enabled)
    suspend fun setBudgetWarning(enabled: Boolean) = preferencesDataStore.setBudgetWarning(enabled)
    suspend fun setRecurringAlert(enabled: Boolean) = preferencesDataStore.setRecurringAlert(enabled)
    suspend fun setMonthlySummary(enabled: Boolean) = preferencesDataStore.setMonthlySummary(enabled)
    suspend fun setUserProfile(name: String, email: String) = preferencesDataStore.setUserProfile(name, email)

    // Auth
    suspend fun login(email: String, pass: String): Result<AuthUser> = authManager.login(email, pass)
    suspend fun register(name: String, email: String, pass: String): Result<AuthUser> = authManager.register(name, email, pass)
    suspend fun sendPasswordReset(email: String): Result<Unit> = authManager.sendPasswordReset(email)
    suspend fun logout() = authManager.logout()

    // Sync
    suspend fun syncWithCloud(): Boolean {
        val success = syncManager.triggerSync()
        if (success) {
            preferencesDataStore.updateSyncTimestamp(System.currentTimeMillis())
        }
        return success
    }
}
