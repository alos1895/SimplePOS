package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.Order
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.User
import com.alos895.simplepos.model.DeliveryService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.alos895.simplepos.data.datasource.MenuData

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    private val orderRepository = OrderRepository(application)

    private val _selectedDelivery = MutableStateFlow<DeliveryService?>(null)
    val selectedDelivery: StateFlow<DeliveryService?> = _selectedDelivery

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total

    init {
        // Inicializa el servicio a domicilio con el primero disponible
        _selectedDelivery.value = MenuData.deliveryOptions.first()
        // Observa cambios en carrito y servicio para actualizar el total
        viewModelScope.launch {
            cartItems.collect { items ->
                val deliveryPrice = _selectedDelivery.value?.price ?: 0
                _total.value = items.sumOf { it.subtotal } + deliveryPrice
            }
        }
        viewModelScope.launch {
            selectedDelivery.collect {
                val deliveryPrice = it?.price ?: 0
                _total.value = _cartItems.value.sumOf { item -> item.subtotal } + deliveryPrice
            }
        }
    }

    fun setDeliveryService(delivery: DeliveryService) {
        _selectedDelivery.value = delivery
        // El total se actualizará automáticamente por el colector
    }

    fun addToCart(pizza: Pizza, tamano: TamanoPizza) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.pizza.nombre == pizza.nombre && it.tamano.nombre == tamano.nombre }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(cantidad = item.cantidad + 1)
        } else {
            current.add(CartItem(pizza, tamano))
        }
        _cartItems.value = current
    }

    fun removeFromCart(pizza: Pizza, tamano: TamanoPizza) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.pizza.nombre == pizza.nombre && it.tamano.nombre == tamano.nombre }
        if (index >= 0) {
            val item = current[index]
            if (item.cantidad > 1) {
                current[index] = item.copy(cantidad = item.cantidad - 1)
            } else {
                current.removeAt(index)
            }
        }
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun buildTicket(): String {
        val cartItems = _cartItems.value
        if (cartItems.isEmpty()) {
            return "El carrito está vacío."
        }
        var result = 0.0
        val info = PizzeriaData.info
        val sb = StringBuilder()
        sb.appendLine(info.logoAscii)
        sb.appendLine(info.nombre)
        sb.appendLine(info.telefono)
        sb.appendLine(info.direccion)
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
            result += item.subtotal
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${"%.2f".format(result)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

    fun saveOrder(user: User) {
        viewModelScope.launch {
            val gson = Gson()
            val itemsJson = gson.toJson(_cartItems.value)
            val userJson = gson.toJson(user)
            val deliveryPrice = _selectedDelivery.value?.price ?: 0
            val isDeliveried = deliveryPrice > 0 // Si hay precio, es entrega a domicilio
            val orderEntity = OrderEntity(
                itemsJson = itemsJson,
                total = total.value,
                timestamp = System.currentTimeMillis(),
                userJson = userJson,
                deliveryServicePrice = deliveryPrice,
                isDeliveried = isDeliveried
            )
            orderRepository.addOrder(orderEntity)
            clearCart()
        }
    }
}

// Factory para CartViewModel
class CartViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
