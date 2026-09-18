package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String = "restaurant",
    val categoryColor: Long = 0xFF3949AB,
    val paymentMethod: String = "Cash",
    val accountId: Long = 1,
    val note: String = "",
    val date: String, // Format: YYYY-MM-DD
    val time: String, // Format: HH:mm
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
