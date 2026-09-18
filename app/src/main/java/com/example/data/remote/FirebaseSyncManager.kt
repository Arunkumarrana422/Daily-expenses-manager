package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.IncomeDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

class FirebaseSyncManager(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao
) {
    private val TAG = "FirebaseSyncManager"

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    /**
     * Upload an expense directly to Firestore
     */
    suspend fun uploadExpense(userId: String, expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            val data = hashMapOf(
                "id" to expense.id,
                "amount" to expense.amount,
                "categoryId" to expense.categoryId,
                "categoryName" to expense.categoryName,
                "categoryIcon" to expense.categoryIcon,
                "categoryColor" to expense.categoryColor,
                "paymentMethod" to expense.paymentMethod,
                "accountId" to expense.accountId,
                "note" to expense.note,
                "date" to expense.date,
                "time" to expense.time,
                "userId" to userId,
                "updatedAt" to expense.updatedAt
            )
            fs.collection("users").document(userId)
                .collection("expenses").document(expense.id.toString())
                .set(data).await()
            expenseDao.markSynced(listOf(expense.id))
        } catch (e: Exception) {
            Log.w(TAG, "uploadExpense error: ${e.message}")
        }
    }

    /**
     * Delete an expense from Firestore
     */
    suspend fun deleteExpense(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("expenses").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "deleteExpense error: ${e.message}")
        }
    }

    /**
     * Upload an income directly to Firestore
     */
    suspend fun uploadIncome(userId: String, income: IncomeEntity) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            val data = hashMapOf(
                "id" to income.id,
                "amount" to income.amount,
                "source" to income.source,
                "accountId" to income.accountId,
                "paymentMethod" to income.paymentMethod,
                "note" to income.note,
                "date" to income.date,
                "time" to income.time,
                "userId" to userId,
                "updatedAt" to income.updatedAt
            )
            fs.collection("users").document(userId)
                .collection("incomes").document(income.id.toString())
                .set(data).await()
            incomeDao.markSynced(listOf(income.id))
        } catch (e: Exception) {
            Log.w(TAG, "uploadIncome error: ${e.message}")
        }
    }

    /**
     * Delete an income from Firestore
     */
    suspend fun deleteIncome(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("incomes").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "deleteIncome error: ${e.message}")
        }
    }

    /**
     * Upload an account to Firestore
     */
    suspend fun uploadAccount(userId: String, account: AccountEntity) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            val data = hashMapOf(
                "id" to account.id,
                "name" to account.name,
                "type" to account.type,
                "balance" to account.balance,
                "userId" to userId
            )
            fs.collection("users").document(userId)
                .collection("accounts").document(account.id.toString())
                .set(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "uploadAccount error: ${e.message}")
        }
    }

    /**
     * Upload a budget to Firestore
     */
    suspend fun uploadBudget(userId: String, budget: BudgetEntity) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            val data = hashMapOf(
                "id" to budget.id,
                "categoryName" to (budget.categoryName ?: ""),
                "amount" to budget.amount,
                "period" to budget.period,
                "warningThreshold" to budget.warningThreshold,
                "userId" to userId
            )
            fs.collection("users").document(userId)
                .collection("budgets").document(budget.id.toString())
                .set(data).await()
        } catch (e: Exception) {
            Log.w(TAG, "uploadBudget error: ${e.message}")
        }
    }

    /**
     * Delete a budget from Firestore
     */
    suspend fun deleteBudget(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("budgets").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "deleteBudget error: ${e.message}")
        }
    }

    /**
     * Fetch all user data from Firebase and sync to local Room database
     */
    suspend fun fetchFromFirebase(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext false
        val fs = firestore ?: return@withContext false

        try {
            // 1. Fetch expenses
            val expSnapshot = fs.collection("users").document(userId)
                .collection("expenses").get().await()

            val fetchedExpenses = expSnapshot.documents.mapNotNull { doc ->
                try {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val categoryName = doc.getString("categoryName") ?: "Other"
                    val categoryIcon = doc.getString("categoryIcon") ?: "category"
                    val categoryColor = doc.getLong("categoryColor") ?: 0xFF3949AB
                    val categoryId = doc.getLong("categoryId") ?: 1L
                    val paymentMethod = doc.getString("paymentMethod") ?: "Cash"
                    val accountId = doc.getLong("accountId") ?: 1L
                    val note = doc.getString("note") ?: ""
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val id = doc.id.toLongOrNull() ?: 0L

                    ExpenseEntity(
                        id = id,
                        userId = userId,
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
                        syncStatus = "SYNCED"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            for (exp in fetchedExpenses) {
                expenseDao.insertExpense(exp)
            }

            // 2. Fetch incomes
            val incSnapshot = fs.collection("users").document(userId)
                .collection("incomes").get().await()

            val fetchedIncomes = incSnapshot.documents.mapNotNull { doc ->
                try {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val source = doc.getString("source") ?: "Income"
                    val accountId = doc.getLong("accountId") ?: 1L
                    val paymentMethod = doc.getString("paymentMethod") ?: "Cash"
                    val note = doc.getString("note") ?: ""
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val id = doc.id.toLongOrNull() ?: 0L

                    IncomeEntity(
                        id = id,
                        userId = userId,
                        amount = amount,
                        source = source,
                        accountId = accountId,
                        paymentMethod = paymentMethod,
                        note = note,
                        date = date,
                        time = time,
                        syncStatus = "SYNCED"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            for (inc in fetchedIncomes) {
                incomeDao.insertIncome(inc)
            }

            // 3. Fetch accounts
            val accSnapshot = fs.collection("users").document(userId)
                .collection("accounts").get().await()

            val fetchedAccounts = accSnapshot.documents.mapNotNull { doc ->
                try {
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val type = doc.getString("type") ?: "Cash"
                    val balance = doc.getDouble("balance") ?: 0.0
                    val id = doc.id.toLongOrNull() ?: 0L

                    AccountEntity(
                        id = id,
                        userId = userId,
                        name = name,
                        type = type,
                        balance = balance
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (fetchedAccounts.isNotEmpty()) {
                accountDao.insertAccounts(fetchedAccounts)
            }

            // 4. Fetch budgets
            val bgSnapshot = fs.collection("users").document(userId)
                .collection("budgets").get().await()

            val fetchedBudgets = bgSnapshot.documents.mapNotNull { doc ->
                try {
                    val categoryName = doc.getString("categoryName") ?: "All"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val period = doc.getString("period") ?: "MONTHLY"
                    val warningThreshold = (doc.getDouble("warningThreshold") ?: 0.75).toFloat()
                    val id = doc.id.toLongOrNull() ?: 0L

                    BudgetEntity(
                        id = id,
                        userId = userId,
                        categoryName = categoryName,
                        amount = amount,
                        period = period,
                        warningThreshold = warningThreshold
                    )
                } catch (e: Exception) {
                    null
                }
            }

            for (bg in fetchedBudgets) {
                budgetDao.insertBudget(bg)
            }

            true
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromFirebase error: ${e.message}")
            false
        }
    }

    /**
     * Trigger full two-way synchronization
     */
    suspend fun triggerSync(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            _syncState.value = SyncState.Error("Internet connection required to sync data with Firebase.")
            return@withContext false
        }

        _syncState.value = SyncState.Syncing

        try {
            val currentUserId = auth?.currentUser?.uid
            if (currentUserId != null) {
                // 1. Push pending items to Firebase
                val pendingExpenses = expenseDao.getPendingExpenses()
                val pendingIncomes = incomeDao.getPendingIncomes()

                for (exp in pendingExpenses) {
                    uploadExpense(currentUserId, exp)
                }
                for (inc in pendingIncomes) {
                    uploadIncome(currentUserId, inc)
                }

                // 2. Fetch fresh items from Firebase
                fetchFromFirebase(currentUserId)
            }

            val now = System.currentTimeMillis()
            _syncState.value = SyncState.Success("Data synchronized with Firebase", now)
            true
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("Sync failed: ${e.message}")
            false
        }
    }
}
