package com.alos895.simplepos.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.User
import com.alos895.simplepos.model.DeliveryService
import com.google.gson.Gson
import kotlin.math.floor
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.PostreOrExtra

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    
    private val _dessertItems = MutableStateFlow<List<CartItemPostre>>(emptyList())
    val dessertItems: StateFlow<List<CartItemPostre>> = _dessertItems
    
    private val orderRepository = OrderRepository(application)

    private val _selectedDelivery = MutableStateFlow<DeliveryService?>(null)
    val selectedDelivery: StateFlow<DeliveryService?> = _selectedDelivery

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total

    private val _comentarios = MutableStateFlow("")
    val comentarios: StateFlow<String> = _comentarios

    init {
        // Inicializa el servicio a domicilio con el primero disponible
        _selectedDelivery.value = MenuData.deliveryOptions.first()
        // Observa cambios en carrito y servicio para actualizar el total
        viewModelScope.launch {
            cartItems.collect { items ->
                val deliveryPrice = _selectedDelivery.value?.price ?: 0
                val dessertsTotal = _dessertItems.value.sumOf { it.subtotal }
                _total.value = items.sumOf { it.subtotal } + dessertsTotal + deliveryPrice
            }
        }
        viewModelScope.launch {
            selectedDelivery.collect {
                val deliveryPrice = it?.price ?: 0
                val pizzasTotal = _cartItems.value.sumOf { item -> item.subtotal }
                val dessertsTotal = _dessertItems.value.sumOf { it.subtotal }
                _total.value = pizzasTotal + dessertsTotal + deliveryPrice
            }
        }
        viewModelScope.launch {
            dessertItems.collect { desserts ->
                val deliveryPrice = _selectedDelivery.value?.price ?: 0
                val pizzasTotal = _cartItems.value.sumOf { item -> item.subtotal }
                _total.value = pizzasTotal + desserts.sumOf { it.subtotal } + deliveryPrice
            }
        }
    }

    fun setDeliveryService(delivery: DeliveryService) {
        _selectedDelivery.value = delivery
        // El total se actualizará automáticamente por el colector
    }

    fun setComentarios(comentarios: String) {
        _comentarios.value = comentarios
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

    fun addDessertToCart(postreOrExtra: PostreOrExtra) {
        val current = _dessertItems.value.toMutableList()
        val index = current.indexOfFirst { it.postreOrExtra.id == postreOrExtra.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(cantidad = item.cantidad + 1)
        } else {
            current.add(CartItemPostre(postreOrExtra))
        }
        _dessertItems.value = current
    }

    fun removeDessertFromCart(postreOrExtra: PostreOrExtra) {
        val current = _dessertItems.value.toMutableList()
        val index = current.indexOfFirst { it.postreOrExtra.id == postreOrExtra.id }
        if (index >= 0) {
            val item = current[index]
            if (item.cantidad > 1) {
                current[index] = item.copy(cantidad = item.cantidad - 1)
            } else {
                current.removeAt(index)
            }
        }
        _dessertItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _dessertItems.value = emptyList()
    }

    fun buildTicket(): String {
        val cartItems = _cartItems.value
        val dessertItems = _dessertItems.value
        if (cartItems.isEmpty() && dessertItems.isEmpty()) {
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
        if (dessertItems.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            dessertItems.forEach { item ->
                sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}   $${"%.2f".format(item.subtotal)}")
                result += item.subtotal
            }
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${"%.2f".format(result)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

    fun saveOrder(user: User, deliveryAddress: String = "") {
        viewModelScope.launch {
            val gson = Gson()
            val itemsJson = gson.toJson(_cartItems.value)
            val dessertsJson = gson.toJson(_dessertItems.value)
            val userJson = gson.toJson(user)
            val deliveryPrice = _selectedDelivery.value?.price ?: 0
            val isDeliveried = deliveryPrice > 0 // Si hay precio, es entrega a domicilio
            // Ncesito agregar los tados del objeto d servicio a domicilio
            val currentDeliveryService = _selectedDelivery.value
            var isTOTODO = false
            var precioTOTODO = 0.0
            var descuentoTOTODO = 0.0
            // Si es todoo, actualizar precio TOTODO = precio final - .10%
            if (currentDeliveryService!!.isTOTODO) {
                isTOTODO = true
                precioTOTODO = calculateTOTODOPrice(total.value)
                descuentoTOTODO = total.value - precioTOTODO
            }

            val orderEntity = OrderEntity(
                itemsJson = itemsJson,
                total = total.value,
                timestamp = System.currentTimeMillis(),
                userJson = userJson,
                deliveryServicePrice = deliveryPrice,
                isDeliveried = isDeliveried,
                dessertsJson = dessertsJson,
                comentarios = comentarios.value,
                deliveryAddress = deliveryAddress,
                isTOTODO = isTOTODO,
                precioTOTODO = precioTOTODO,
                descuentoTOTODO = descuentoTOTODO
            )
            orderRepository.addOrder(orderEntity)
            clearCart()
        }
    }

    private fun calculateTOTODOPrice(total: Double): Double {
        val discounted = total * 0.9  // aplica 10% de descuento
        val decimals = discounted - discounted.toInt()

        return if (decimals < 0.5) {
            floor(discounted)
        } else {
            ceil(discounted)
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
