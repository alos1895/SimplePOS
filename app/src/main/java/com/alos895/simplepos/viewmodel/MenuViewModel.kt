package com.alos895.simplepos.viewmodel

import androidx.lifecycle.ViewModel
import com.alos895.simplepos.data.MenuRepository
import com.alos895.simplepos.model.Pizza
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MenuViewModel : ViewModel() {
    private val repository = MenuRepository()
    private val _pizzas = MutableStateFlow<List<Pizza>>(emptyList())
    val pizzas: StateFlow<List<Pizza>> = _pizzas

    init {
        loadMenu()
    }

    private fun loadMenu() {
        _pizzas.value = repository.getPizzas()
    }
} 