package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val amount: Double,
    val period: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY
    val startDate: String = "",
    val endDate: String = "",
    val warningThreshold: Float = 0.75f // 50%, 75%, 90%, 100%
)
