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

    fun replacePizzaBasesForDate(
        selectedDateMillis: Long,
        smallCount: Int,
        mediumCount: Int,
        largeCount: Int,
        extraLargeCount: Int
    ) {
        val total = smallCount + mediumCount + largeCount + extraLargeCount

        viewModelScope.launch {
            runCatching {
                val dayCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfDay = dayCalendar.timeInMillis
                val endOfDay = startOfDay + 86_399_999L

                pizzaBaseDao.deleteByCreatedAtRange(startOfDay, endOfDay)

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
                repeat(extraLargeCount) {
                    pizzaBaseDao.insertPizzaBase(
                        PizzaBaseEntity(size = "extra grande", createdAt = selectedDateMillis)
                    )
                }
            }.onSuccess {
                _events.emit(AdminInventoryEvent.Success("Se actualizó el día con $total bases"))
            }.onFailure { error ->
                _events.emit(
                    AdminInventoryEvent.Error(
                        error.message ?: "No se pudieron actualizar las bases"
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
