package com.alos895.simplepos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.local.TransactionDao
import com.alos895.simplepos.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class TransactionViewModel(private val transactionDao: TransactionDao) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    fun loadTransactions(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _transactions.value = transactionDao.getTransactionsByDateRange(startDate, endDate)
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.insertTransaction(transaction)
            loadTransactions(transaction.date, transaction.date) // Refrescar datos
        }
    }

    fun removeTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transaction)
            loadTransactions(transaction.date, transaction.date) // Refrescar datos
        }
    }
}

