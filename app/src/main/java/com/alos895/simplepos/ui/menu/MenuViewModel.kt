package com.alos895.simplepos.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.model.ExtraType
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MenuRepository(AppDatabase.getDatabase(application))

    val pizzas: StateFlow<List<Pizza>> = repository.getPizzas()
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
}
