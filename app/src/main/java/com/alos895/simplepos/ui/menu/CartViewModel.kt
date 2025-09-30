package com.alos895.simplepos.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.model.User
import com.google.gson.Gson
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CartViewModel(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _dessertItems = MutableStateFlow<List<CartItemPostre>>(emptyList())
    val dessertItems: StateFlow<List<CartItemPostre>> = _dessertItems.asStateFlow()

    private val _selectedDelivery = MutableStateFlow<DeliveryService?>(null)
    val selectedDelivery: StateFlow<DeliveryService?> = _selectedDelivery.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems.asStateFlow()

    private val _comentarios = MutableStateFlow("")
    val comentarios: StateFlow<String> = _comentarios.asStateFlow()

    private val ingredientesPorId by lazy {
        menuRepository.getIngredientes().associateBy { it.id }
    }

    private val defaultDelivery = menuRepository.getDefaultDelivery()

    init {
        _selectedDelivery.value = defaultDelivery

        viewModelScope.launch {
            combine(_cartItems, _dessertItems, _selectedDelivery) { pizzas, desserts, delivery ->
                val pizzasTotal = pizzas.sumOf { it.subtotal }
                val dessertsTotal = desserts.sumOf { it.subtotal }
                val deliveryPrice = delivery?.price ?: 0
                val itemCount = pizzas.sumOf { it.cantidad } + desserts.sumOf { it.cantidad }
                Pair(pizzasTotal + dessertsTotal + deliveryPrice, itemCount)
            }.collect { (totalAmount, itemsCount) ->
                _total.value = totalAmount
                _totalItems.value = itemsCount
            }
        }
    }

    fun setDeliveryService(delivery: DeliveryService) {
        _selectedDelivery.value = delivery
    }

    fun resetDeliverySelection() {
        _selectedDelivery.value = defaultDelivery
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

    fun buildCocinaTicket(
        user: User,
        deliveryAddress: String,
        deliveryService: DeliveryService?,
        timestamp: Long
    ): String {
        val cartItems = _cartItems.value
        val desserts = _dessertItems.value
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hora = formatter.format(Date(timestamp))
        val entrega = deliveryService ?: _selectedDelivery.value
        val destino = when {
            entrega?.price ?: 0 > 0 -> if (deliveryAddress.isNotBlank()) deliveryAddress else entrega?.zona ?: "Domicilio"
            entrega?.pickUp == true -> entrega.zona
            else -> "Pasan/Caminando"
        }
        val clienteNombre = user.nombre.ifBlank { "Cliente" }

        val sb = StringBuilder()
        sb.appendLine("ORDEN PARA COCINA")
        sb.appendLine("Hora: $hora")
        sb.appendLine("Cliente: $clienteNombre - $destino")
        sb.appendLine("-------------------------------")

        if (cartItems.isEmpty()) {
            sb.appendLine("Sin productos")
        } else {
            cartItems.forEach { item ->
                sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre.uppercase(Locale.getDefault())}")
                item.pizza.ingredientesBaseIds.forEach { ingredienteId ->
                    ingredientesPorId[ingredienteId]?.let { ingrediente ->
                        sb.appendLine("- ${ingrediente.nombre}")
                    }
                }
                sb.appendLine()
            }
        }

        if (desserts.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            sb.appendLine("Extras:")
            desserts.forEach { item ->
                sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}")
            }
        }

        val comentariosActuales = _comentarios.value
        if (comentariosActuales.isNotBlank()) {
            sb.appendLine("-------------------------------")
            sb.appendLine("COMENTARIOS:")
            sb.appendLine(comentariosActuales)
        }

        return sb.toString()
    }

    fun saveOrder(user: User, deliveryAddress: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val gson = Gson()
            val itemsJson = gson.toJson(_cartItems.value)
            val dessertsJson = gson.toJson(_dessertItems.value)
            val userJson = gson.toJson(user)
            val currentDeliveryService = _selectedDelivery.value
            val deliveryPrice = currentDeliveryService?.price ?: 0
            val isDeliveried = deliveryPrice > 0
            var isTOTODO = false
            var precioTOTODO = 0.0
            var descuentoTOTODO = 0.0
            if (currentDeliveryService?.isTOTODO == true) {
                isTOTODO = true
                precioTOTODO = calculateTOTODOPrice(_total.value)
                descuentoTOTODO = _total.value - precioTOTODO
            }

            val orderEntity = OrderEntity(
                itemsJson = itemsJson,
                total = _total.value,
                timestamp = timestamp,
                userJson = userJson,
                deliveryServicePrice = deliveryPrice,
                isDeliveried = isDeliveried,
                dessertsJson = dessertsJson,
                comentarios = _comentarios.value,
                deliveryAddress = deliveryAddress,
                isTOTODO = isTOTODO,
                precioTOTODO = precioTOTODO,
                descuentoTOTODO = descuentoTOTODO
            )
            orderRepository.addOrder(orderEntity)
            clearCart()
            resetDeliverySelection()
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
