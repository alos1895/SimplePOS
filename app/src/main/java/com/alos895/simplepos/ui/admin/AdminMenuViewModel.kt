package com.alos895.simplepos.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.AdminPizza
import com.alos895.simplepos.data.repository.BaseInventoryRepository
import com.alos895.simplepos.data.repository.DailyBaseInventory
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.data.repository.PizzaUpsert
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.model.ExtraType
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.PostreOrExtra
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

sealed class AdminActionEvent {
    data class Success(val message: String) : AdminActionEvent()
    data class Error(val message: String) : AdminActionEvent()
}

class AdminMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MenuRepository(AppDatabase.getDatabase(application))
    private val baseInventoryRepository = BaseInventoryRepository(AppDatabase.getDatabase(application))
    private val _events = MutableSharedFlow<AdminActionEvent>()
    val events = _events.asSharedFlow()

    val ingredientes: StateFlow<List<Ingrediente>> = repository.getIngredientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pizzas: StateFlow<List<AdminPizza>> = repository.getAdminPizzas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val postres: StateFlow<List<PostreOrExtra>> = repository.getExtras(ExtraType.POSTRE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val extras: StateFlow<List<PostreOrExtra>> = repository.getExtras(ExtraType.EXTRA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val combos: StateFlow<List<PostreOrExtra>> = repository.getExtras(ExtraType.COMBO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bebidas: StateFlow<List<PostreOrExtra>> = repository.getExtras(ExtraType.BEBIDA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dailyInventory: StateFlow<List<DailyBaseInventory>> = baseInventoryRepository.observeDailySummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun saveIngredient(ingrediente: Ingrediente) {
        val action = if (ingrediente.id == 0) "creado" else "actualizado"
        launchAdminAction("Ingrediente $action: ${ingrediente.nombre}") {
            repository.upsertIngredient(ingrediente)
        }
    }

    fun deleteIngredient(ingrediente: Ingrediente) {
        launchAdminAction("Ingrediente eliminado: ${ingrediente.nombre}") {
            repository.deleteIngredient(ingrediente)
        }
    }

    fun savePizza(pizza: PizzaUpsert) {
        val action = if (pizza.id == null) "creada" else "actualizada"
        launchAdminAction("Pizza $action: ${pizza.nombre}") {
            repository.upsertPizza(pizza)
        }
    }

    fun deletePizza(pizza: AdminPizza) {
        launchAdminAction("Pizza eliminada: ${pizza.nombre}") {
            repository.deletePizza(pizza)
        }
    }

    fun saveExtra(extra: PostreOrExtra, type: ExtraType) {
        val entityName = when (type) {
            ExtraType.POSTRE -> "Postre"
            ExtraType.EXTRA -> "Extra"
            ExtraType.COMBO -> "Combo"
            ExtraType.BEBIDA -> "Bebida"
        }
        val action = if (extra.id == 0) "creado" else "actualizado"
        launchAdminAction("$entityName $action: ${extra.nombre}") {
            repository.upsertExtra(extra, type)
        }
    }

    fun deleteExtra(extra: PostreOrExtra, type: ExtraType) {
        val entityName = when (type) {
            ExtraType.POSTRE -> "Postre"
            ExtraType.EXTRA -> "Extra"
            ExtraType.COMBO -> "Combo"
            ExtraType.BEBIDA -> "Bebida"
        }
        launchAdminAction("$entityName eliminado: ${extra.nombre}") {
            repository.deleteExtra(extra, type)
        }
    }

    fun addInventory(dateKey: String, chica: Int, mediana: Int, grande: Int) {
        val normalizedDate = dateKey.trim()
        val isValidDate = runCatching {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            formatter.parse(normalizedDate)
        }.isSuccess

        if (!isValidDate) {
            viewModelScope.launch {
                _events.emit(AdminActionEvent.Error("Fecha inválida. Usa formato yyyy-MM-dd"))
            }
            return
        }

        if (chica <= 0 && mediana <= 0 && grande <= 0) {
            viewModelScope.launch {
                _events.emit(AdminActionEvent.Error("Ingresa al menos una base a agregar"))
            }
            return
        }
        launchAdminAction("Inventario actualizado para $normalizedDate") {
            baseInventoryRepository.addBases(normalizedDate, chica, mediana, grande)
        }
    }

    private fun launchAdminAction(successMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    _events.emit(AdminActionEvent.Success(successMessage))
                }
                .onFailure { error ->
                    val message = error.message?.takeIf { it.isNotBlank() }
                        ?: "Error inesperado al guardar cambios"
                    _events.emit(AdminActionEvent.Error(message))
                }
        }
    }
}
