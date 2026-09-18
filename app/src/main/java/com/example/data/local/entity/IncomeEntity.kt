package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val amount: Double,
    val source: String, // e.g. Salary, Business, Freelance, Gift, Investment, etc.
    val accountId: Long = 1,
    val paymentMethod: String = "Bank Transfer",
    val note: String = "",
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
