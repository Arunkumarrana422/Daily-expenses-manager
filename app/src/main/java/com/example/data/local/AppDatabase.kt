package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExpenseEntity::class,
        IncomeEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        RecurringExpenseEntity::class,
        SavingsGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_manager_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()
            val budgetDao = database.budgetDao()
            val savingsGoalDao = database.savingsGoalDao()
            val recurringDao = database.recurringExpenseDao()
            val expenseDao = database.expenseDao()
            val incomeDao = database.incomeDao()

            if (categoryDao.count() == 0) {
                val defaultExpenseCategories = listOf(
                    CategoryEntity(name = "Food", icon = "🍔", color = 0xFFFF7043, type = "EXPENSE"),
                    CategoryEntity(name = "Grocery", icon = "🛒", color = 0xFF4CAF50, type = "EXPENSE"),
                    CategoryEntity(name = "Transport", icon = "🚗", color = 0xFF29B6F6, type = "EXPENSE"),
                    CategoryEntity(name = "Fuel", icon = "⛽", color = 0xFFFFB300, type = "EXPENSE"),
                    CategoryEntity(name = "Rent", icon = "🏠", color = 0xFFAB47BC, type = "EXPENSE"),
                    CategoryEntity(name = "Electricity", icon = "💡", color = 0xFFFFA726, type = "EXPENSE"),
                    CategoryEntity(name = "Mobile", icon = "📱", color = 0xFF26A69A, type = "EXPENSE"),
                    CategoryEntity(name = "Internet", icon = "🌐", color = 0xFF5C6BC0, type = "EXPENSE"),
                    CategoryEntity(name = "Education", icon = "🎓", color = 0xFF42A5F5, type = "EXPENSE"),
                    CategoryEntity(name = "Health", icon = "💊", color = 0xFFEF5350, type = "EXPENSE"),
                    CategoryEntity(name = "Shopping", icon = "🛍", color = 0xFFEC407A, type = "EXPENSE"),
                    CategoryEntity(name = "Travel", icon = "✈", color = 0xFF26C6DA, type = "EXPENSE"),
                    CategoryEntity(name = "Entertainment", icon = "🎮", color = 0xFF7E57C2, type = "EXPENSE"),
                    CategoryEntity(name = "EMI", icon = "💳", color = 0xFF8D6E63, type = "EXPENSE"),
                    CategoryEntity(name = "Other", icon = "📦", color = 0xFF78909C, type = "EXPENSE"),
                    // Income categories
                    CategoryEntity(name = "Salary", icon = "💼", color = 0xFF2E7D32, type = "INCOME"),
                    CategoryEntity(name = "Business", icon = "🏢", color = 0xFF1565C0, type = "INCOME"),
                    CategoryEntity(name = "Freelance", icon = "💻", color = 0xFF00897B, type = "INCOME"),
                    CategoryEntity(name = "Agriculture", icon = "🌾", color = 0xFF558B2F, type = "INCOME"),
                    CategoryEntity(name = "Pocket Money", icon = "🪙", color = 0xFFF9A825, type = "INCOME"),
                    CategoryEntity(name = "Gift", icon = "🎁", color = 0xFFAD1457, type = "INCOME"),
                    CategoryEntity(name = "Investment", icon = "📈", color = 0xFF6A1B9A, type = "INCOME"),
                    CategoryEntity(name = "Other Income", icon = "📦", color = 0xFF455A64, type = "INCOME")
                )
                categoryDao.insertCategories(defaultExpenseCategories)
            }

            if (accountDao.count() == 0) {
                val defaultAccounts = listOf(
                    AccountEntity(name = "Cash", type = "Cash", balance = 0.0),
                    AccountEntity(name = "Bank Account", type = "Bank Account", balance = 0.0),
                    AccountEntity(name = "UPI", type = "UPI", balance = 0.0)
                )
                accountDao.insertAccounts(defaultAccounts)
            }
        }
    }
}
