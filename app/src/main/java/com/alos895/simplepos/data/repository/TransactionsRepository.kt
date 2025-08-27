package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.Room
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.model.OrderEntity
import kotlinx.coroutines.flow.Flow

class TransactionsRepository (context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "simplepos.db"
    ).build()

    private val transactionDao = db.cashTransactionDao()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
    suspend fun getAllTransactions(): List<TransactionEntity> {
        // TODO Investiate if is necesario as List<TransactionEntity>
        return transactionDao.getAllTransactions()
    }

    suspend fun getTransactionsByDate(date: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsForDate(date)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransaction(transactionId)
    }
}