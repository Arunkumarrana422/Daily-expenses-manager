package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "local_user",
    val name: String,
    val icon: String, // Emoji or icon key
    val color: Long,
    val type: String = "EXPENSE" // "EXPENSE" or "INCOME"
)
