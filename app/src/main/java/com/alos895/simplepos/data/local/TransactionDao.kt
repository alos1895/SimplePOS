package com.alos895.simplepos.data.local

import androidx.room.*
import com.alos895.simplepos.model.Transaction
import java.util.Date

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM `Transaction` WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<Transaction>
}

