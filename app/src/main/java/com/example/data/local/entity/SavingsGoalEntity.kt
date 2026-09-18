package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: String = "" // YYYY-MM-DD
)
