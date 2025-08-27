package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.Room
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.TransactionEntity

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
        return transactionDao.getAllTransactions()
    }

    suspend fun getTransactionsByDate(date: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsForDay(date)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransaction(transactionId)
    }
}