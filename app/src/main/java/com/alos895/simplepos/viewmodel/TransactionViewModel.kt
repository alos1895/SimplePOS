package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.CashTransactionDao // Asegúrate que es com.alos895.simplepos.db.CashTransactionDao
import com.alos895.simplepos.db.entity.CashTransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionsRepository(application)
    private val _transactions = MutableStateFlow<List<CashTransactionEntity>>(emptyList())
    val transactions: StateFlow<List<CashTransactionEntity>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _transactions.value = repository.getAllTransactions()
            } catch (e: Exception) {
                _error.value = "Error al cargar las transacciones: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Modificado para usar el enum TransactionType
    fun addTransaction(concept: String, amount: Double, type: TransactionType) {
        viewModelScope.launch {
            try {
                val newTransaction = CashTransactionEntity(
                    concept = concept,
                    amount = amount,
                    type = type,
                    date = Date().time
                )
                repository.insertTransaction(newTransaction)
                loadTransactions()
            } catch (e: Exception) {
                _error.value = "Error al guardar la transacción: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
