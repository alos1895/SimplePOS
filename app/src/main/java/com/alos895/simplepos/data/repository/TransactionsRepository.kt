package com.alos895.simplepos.data.repository

import com.alos895.simplepos.db.CashTransactionDao
import com.alos895.simplepos.db.entity.TransactionEntity

class TransactionsRepository(private val transactionDao: CashTransactionDao) {

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