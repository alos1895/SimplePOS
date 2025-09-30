package com.alos895.simplepos.ui.menu

import androidx.lifecycle.ViewModel
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MenuViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {
    private val _pizzas = MutableStateFlow<List<Pizza>>(emptyList())
    val pizzas: StateFlow<List<Pizza>> = _pizzas.asStateFlow()

    private val _ingredientes = MutableStateFlow<List<Ingrediente>>(emptyList())
    val ingredientes: StateFlow<List<Ingrediente>> = _ingredientes.asStateFlow()

    private val _postres = MutableStateFlow<List<PostreOrExtra>>(emptyList())
    val postres: StateFlow<List<PostreOrExtra>> = _postres.asStateFlow()

    private val _deliveryOptions = MutableStateFlow<List<DeliveryService>>(emptyList())
    val deliveryOptions: StateFlow<List<DeliveryService>> = _deliveryOptions.asStateFlow()

    init {
        loadMenu()
    }

    private fun loadMenu() {
        _pizzas.value = menuRepository.getPizzas()
        _ingredientes.value = menuRepository.getIngredientes()
        _postres.value = menuRepository.getPostresOrExtras()
        _deliveryOptions.value = menuRepository.getDeliveryOptions()
    }
}