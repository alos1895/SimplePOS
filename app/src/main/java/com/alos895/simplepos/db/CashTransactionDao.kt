package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.CashTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashTransactionDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTransaction(transaction: CashTransactionEntity)

    @Query("SELECT * FROM cash_transactions WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    suspend fun getTransactionsForDateRange(startDate: Long, endDate: Long): List<CashTransactionEntity>

    @Query("SELECT * FROM cash_transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<CashTransactionEntity>

    @Query("DELETE FROM cash_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)
}