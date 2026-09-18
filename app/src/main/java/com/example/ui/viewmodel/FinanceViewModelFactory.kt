package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.remote.FirebaseSyncManager
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FinanceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val database = AppDatabase.getDatabase(context.applicationContext, appScope)
            val preferencesDataStore = UserPreferencesDataStore(context.applicationContext)
            val syncManager = FirebaseSyncManager(
                context.applicationContext,
                database.expenseDao(),
                database.incomeDao()
            )
            val repository = FinanceRepository(
                expenseDao = database.expenseDao(),
                incomeDao = database.incomeDao(),
                categoryDao = database.categoryDao(),
                budgetDao = database.budgetDao(),
                accountDao = database.accountDao(),
                recurringExpenseDao = database.recurringExpenseDao(),
                savingsGoalDao = database.savingsGoalDao(),
                preferencesDataStore = preferencesDataStore,
                syncManager = syncManager
            )
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
