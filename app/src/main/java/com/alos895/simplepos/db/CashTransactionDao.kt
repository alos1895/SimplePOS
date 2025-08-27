package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.TransactionEntity

@Dao
interface CashTransactionDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM cash_transactions WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    suspend fun getTransactionsForDateRange(startDate: Long, endDate: Long): List<TransactionEntity>
    @Query("SELECT * FROM cash_transactions WHERE date == :date ORDER BY date DESC")
    suspend fun getTransactionsForDate(date: Long): List<TransactionEntity>

    @Query("SELECT * FROM cash_transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("DELETE FROM cash_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)
}