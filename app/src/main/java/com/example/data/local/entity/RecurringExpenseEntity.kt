package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String = "Bills",
    val frequency: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: String, // YYYY-MM-DD
    val enabled: Boolean = true
)
