package com.alos895.simplepos.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.PizzaBaseEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AdminInventoryEvent {
    data class Success(val message: String) : AdminInventoryEvent()
    data class Error(val message: String) : AdminInventoryEvent()
}

class AdminInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val pizzaBaseDao = AppDatabase.getDatabase(application).pizzaBaseDao()
    private val _events = MutableSharedFlow<AdminInventoryEvent>()
    val events = _events.asSharedFlow()

    val pizzaBases: StateFlow<List<PizzaBaseEntity>> = pizzaBaseDao.getPizzaBases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPizzaBase(size: String) {
        val normalizedSize = size.trim().lowercase()
        if (normalizedSize !in setOf("chica", "mediana", "grande")) {
            viewModelScope.launch {
                _events.emit(AdminInventoryEvent.Error("Selecciona un tamaño válido"))
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                pizzaBaseDao.insertPizzaBase(
                    PizzaBaseEntity(size = normalizedSize)
                )
            }.onSuccess {
                _events.emit(AdminInventoryEvent.Success("Base $normalizedSize registrada"))
            }.onFailure { error ->
                _events.emit(
                    AdminInventoryEvent.Error(
                        error.message ?: "No se pudo registrar la base"
                    )
                )
            }
        }
    }

    fun markAsUsed(baseId: Long) {
        viewModelScope.launch {
            runCatching { pizzaBaseDao.markAsUsed(baseId) }
                .onFailure {
                    _events.emit(AdminInventoryEvent.Error("No se pudo marcar la base como usada"))
                }
        }
    }
}
