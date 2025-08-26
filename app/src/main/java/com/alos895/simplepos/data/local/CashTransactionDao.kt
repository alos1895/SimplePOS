package com.alos895.simplepos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.model.CashTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransactionEntity)

    @Query("SELECT * FROM cash_transactions WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getTransactionsForDateRange(startDate: Long, endDate: Long): Flow<List<CashTransactionEntity>>

    @Query("SELECT * FROM cash_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<CashTransactionEntity>>

    // Podríamos añadir métodos para actualizar o borrar si son necesarios más adelante
}
