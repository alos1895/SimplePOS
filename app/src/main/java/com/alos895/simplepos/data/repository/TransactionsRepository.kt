package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.Room
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.CashTransactionEntity
import com.alos895.simplepos.model.OrderEntity
import kotlinx.coroutines.flow.Flow

class TransactionsRepository (context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "simplepos.db"
    ).build()

    private val transactionDao = db.cashTransactionDao()

    suspend fun insertTransaction(transaction: CashTransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
    suspend fun getAllTransactions(): List<CashTransactionEntity> {
        // TODO Investiate if is necesario as List<CashTransactionEntity>
        return transactionDao.getAllTransactions()
    }

    suspend fun getTransactionsForDate(date: Long): List<CashTransactionEntity> {
        return transactionDao.getTransactionsForDate(date)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransaction(transactionId)
    }
}