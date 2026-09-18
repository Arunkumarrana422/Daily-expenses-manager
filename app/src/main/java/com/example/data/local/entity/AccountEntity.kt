package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val name: String,
    val type: String, // "Cash", "Bank Account", "UPI", "Credit Card", "Wallet"
    val balance: Double = 0.0
)
