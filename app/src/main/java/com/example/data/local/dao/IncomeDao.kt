package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY date DESC, time DESC, id DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, time DESC")
    fun getIncomesByDateRange(startDate: String, endDate: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id LIMIT 1")
    fun getIncomeById(id: Long): Flow<IncomeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("SELECT * FROM incomes WHERE syncStatus = 'PENDING'")
    suspend fun getPendingIncomes(): List<IncomeEntity>

    @Query("UPDATE incomes SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
