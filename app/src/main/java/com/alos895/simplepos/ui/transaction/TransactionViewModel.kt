package com.alos895.simplepos.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class TransactionViewModel(
    private val repository: TransactionsRepository
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

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
                val newTransaction = TransactionEntity(
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

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transactionId)
                loadTransactions()
            } catch (e: Exception) {
                _error.value = "Error al eliminar la transacción: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}