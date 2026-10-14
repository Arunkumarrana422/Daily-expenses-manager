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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Direct Live Cloud Firestore Manager.
 * Maintains persistent real-time snapshot listeners with Firebase Cloud Firestore.
 * All writes, edits, and deletions happen directly on Cloud Firestore with automatic real-time streaming.
 */
class FirebaseSyncManager(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val preferencesDataStore: com.example.data.local.preferences.UserPreferencesDataStore? = null
) {
    private val TAG = "LiveFirestoreManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Persistent cache settings notice: ${e.message}")
            }
            db
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}")
            null
        }
    }

    // Real-time snapshot listener registrations
    private var expenseListenerRegistration: ListenerRegistration? = null
    private var incomeListenerRegistration: ListenerRegistration? = null
    private var budgetListenerRegistration: ListenerRegistration? = null
    private var profileListenerRegistration: ListenerRegistration? = null
    private var activeUserId: String? = null

    init {
        // Automatically attach live listeners if user is already signed in
        val currentUid = auth?.currentUser?.uid
        if (currentUid != null) {
            startLiveListeners(currentUid)
        }
    }

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            hasInternet && (hasValidated || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    /**
     * Start live real-time snapshot listeners directly on Cloud Firestore
     */
    fun startLiveListeners(userId: String) {
        if (activeUserId == userId && expenseListenerRegistration != null) {
            return
        }
        stopLiveListeners()
        activeUserId = userId

        val fs = firestore ?: return

        try {
            // 1. Live Expenses Listener
            expenseListenerRegistration = fs.collection("users").document(userId)
                .collection("expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Live expenses listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val remoteIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() }.toSet()
                                val parsedExpenses = snapshot.documents.mapNotNull { doc ->
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
                                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

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
                                            createdAt = createdAt,
                                            updatedAt = updatedAt,
                                            syncStatus = "LIVE"
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                for (exp in parsedExpenses) {
                                    expenseDao.insertExpense(exp)
                                }

                                // Delete locally any items deleted in Firestore
                                val localList = expenseDao.getAllExpensesList()
                                for (local in localList) {
                                    if (local.id !in remoteIds && remoteIds.isNotEmpty()) {
                                        expenseDao.deleteExpense(local)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error updating live expenses: ${e.message}")
                            }
                        }
                    }
                }

            // 2. Live Incomes Listener
            incomeListenerRegistration = fs.collection("users").document(userId)
                .collection("incomes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Live incomes listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val remoteIds = snapshot.documents.mapNotNull { it.id.toLongOrNull() }.toSet()
                                val parsedIncomes = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        val amount = doc.getDouble("amount") ?: 0.0
                                        val source = doc.getString("source") ?: "Income"
                                        val accountId = doc.getLong("accountId") ?: 1L
                                        val paymentMethod = doc.getString("paymentMethod") ?: "Cash"
                                        val note = doc.getString("note") ?: ""
                                        val date = doc.getString("date") ?: ""
                                        val time = doc.getString("time") ?: ""
                                        val id = doc.id.toLongOrNull() ?: 0L
                                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

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
                                            createdAt = createdAt,
                                            updatedAt = updatedAt,
                                            syncStatus = "LIVE"
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                for (inc in parsedIncomes) {
                                    incomeDao.insertIncome(inc)
                                }

                                val localList = incomeDao.getAllIncomesList()
                                for (local in localList) {
                                    if (local.id !in remoteIds && remoteIds.isNotEmpty()) {
                                        incomeDao.deleteIncome(local)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error updating live incomes: ${e.message}")
                            }
                        }
                    }
                }

            // 3. Live Budgets Listener
            budgetListenerRegistration = fs.collection("users").document(userId)
                .collection("budgets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Live budgets listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            try {
                                val parsedBudgets = snapshot.documents.mapNotNull { doc ->
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

                                for (bg in parsedBudgets) {
                                    budgetDao.insertBudget(bg)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error updating live budgets: ${e.message}")
                            }
                        }
                    }
                }

            // 4. Live Profile Document Listener
            profileListenerRegistration = fs.collection("users").document(userId)
                .addSnapshotListener { doc, error ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    val name = doc.getString("name")
                    val photo = doc.getString("profilePhotoBase64")
                    scope.launch {
                        if (!name.isNullOrBlank()) {
                            preferencesDataStore?.setUserName(name)
                        }
                        if (!photo.isNullOrBlank()) {
                            preferencesDataStore?.setProfilePhoto(photo)
                        }
                    }
                }

            Log.i(TAG, "Direct Live Firestore listeners connected successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live Firestore listeners: ${e.message}")
        }
    }

    /**
     * Stop real-time snapshot listeners (called on sign out)
     */
    fun stopLiveListeners() {
        try {
            expenseListenerRegistration?.remove()
            incomeListenerRegistration?.remove()
            budgetListenerRegistration?.remove()
            profileListenerRegistration?.remove()
        } catch (e: Exception) {
            Log.w(TAG, "Error detaching listeners: ${e.message}")
        } finally {
            expenseListenerRegistration = null
            incomeListenerRegistration = null
            budgetListenerRegistration = null
            profileListenerRegistration = null
            activeUserId = null
        }
    }

    /**
     * Write an expense directly to Firestore
     */
    suspend fun uploadExpense(userId: String, expense: ExpenseEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext false
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
                "createdAt" to expense.createdAt,
                "updatedAt" to expense.updatedAt
            )
            fs.collection("users").document(userId)
                .collection("expenses").document(expense.id.toString())
                .set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct write expense error: ${e.message}")
            false
        }
    }

    /**
     * Delete an expense directly from Firestore
     */
    suspend fun deleteExpense(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("expenses").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Direct delete expense error: ${e.message}")
        }
    }

    /**
     * Write an income directly to Firestore
     */
    suspend fun uploadIncome(userId: String, income: IncomeEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext false
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
                "createdAt" to income.createdAt,
                "updatedAt" to income.updatedAt
            )
            fs.collection("users").document(userId)
                .collection("incomes").document(income.id.toString())
                .set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct write income error: ${e.message}")
            false
        }
    }

    /**
     * Delete an income directly from Firestore
     */
    suspend fun deleteIncome(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("incomes").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Direct delete income error: ${e.message}")
        }
    }

    /**
     * Write a budget directly to Firestore
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
            Log.w(TAG, "Direct write budget error: ${e.message}")
        }
    }

    /**
     * Delete a budget directly from Firestore
     */
    suspend fun deleteBudget(userId: String, id: Long) = withContext(Dispatchers.IO) {
        try {
            val fs = firestore ?: return@withContext
            fs.collection("users").document(userId)
                .collection("budgets").document(id.toString())
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Direct delete budget error: ${e.message}")
        }
    }

    /**
     * Write an account directly
     */
    suspend fun uploadAccount(userId: String, account: AccountEntity) = withContext(Dispatchers.IO) {
        // Accounts are stored in Room database
    }

    /**
     * Delete an account
     */
    suspend fun deleteAccount(userId: String, id: Long) = withContext(Dispatchers.IO) {
        // Accounts are stored in Room database
    }

    /**
     * Refresh connection / ensure listeners are active
     */
    suspend fun triggerSync(): Boolean = withContext(Dispatchers.IO) {
        val uid = auth?.currentUser?.uid
        if (uid != null) {
            startLiveListeners(uid)
        }
        true
    }

    suspend fun fetchFromFirebase(userId: String): Boolean = withContext(Dispatchers.IO) {
        startLiveListeners(userId)
        true
    }

    suspend fun uploadAllLocalDataToFirestore(userId: String) = withContext(Dispatchers.IO) {
        startLiveListeners(userId)
    }
}
