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

    fun addPizzaBasesForDate(
        selectedDateMillis: Long,
        smallCount: Int,
        mediumCount: Int,
        largeCount: Int
    ) {
        val total = smallCount + mediumCount + largeCount
        if (total <= 0) {
            viewModelScope.launch {
                _events.emit(AdminInventoryEvent.Error("Captura al menos una base"))
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                repeat(smallCount) {
                    pizzaBaseDao.insertPizzaBase(
                        PizzaBaseEntity(size = "chica", createdAt = selectedDateMillis)
                    )
                }
                repeat(mediumCount) {
                    pizzaBaseDao.insertPizzaBase(
                        PizzaBaseEntity(size = "mediana", createdAt = selectedDateMillis)
                    )
                }
                repeat(largeCount) {
                    pizzaBaseDao.insertPizzaBase(
                        PizzaBaseEntity(size = "grande", createdAt = selectedDateMillis)
                    )
                }
            }.onSuccess {
                _events.emit(AdminInventoryEvent.Success("Se guardaron $total bases"))
            }.onFailure { error ->
                _events.emit(
                    AdminInventoryEvent.Error(
                        error.message ?: "No se pudieron guardar las bases"
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
