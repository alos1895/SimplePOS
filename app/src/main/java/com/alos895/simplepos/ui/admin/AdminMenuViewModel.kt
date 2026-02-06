package com.alos895.simplepos.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.AdminPizza
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.data.repository.PizzaUpsert
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.model.ExtraType
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.PostreOrExtra
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MenuRepository(AppDatabase.getDatabase(application))

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

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun saveIngredient(ingrediente: Ingrediente) {
        viewModelScope.launch {
            repository.upsertIngredient(ingrediente)
        }
    }

    fun deleteIngredient(ingrediente: Ingrediente) {
        viewModelScope.launch {
            repository.deleteIngredient(ingrediente)
        }
    }

    fun savePizza(pizza: PizzaUpsert) {
        viewModelScope.launch {
            repository.upsertPizza(pizza)
        }
    }

    fun deletePizza(pizza: AdminPizza) {
        viewModelScope.launch {
            repository.deletePizza(pizza)
        }
    }

    fun saveExtra(extra: PostreOrExtra, type: ExtraType) {
        viewModelScope.launch {
            repository.upsertExtra(extra, type)
        }
    }

    fun deleteExtra(extra: PostreOrExtra, type: ExtraType) {
        viewModelScope.launch {
            repository.deleteExtra(extra, type)
        }
    }
}
