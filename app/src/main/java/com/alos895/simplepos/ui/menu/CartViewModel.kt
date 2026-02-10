package com.alos895.simplepos.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPortion
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.DeliveryType
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.model.User
import com.alos895.simplepos.model.sizeLabel
import com.alos895.simplepos.ui.common.CartItemFormatter
import com.google.gson.Gson
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
        _selectedDelivery.value = MenuData.deliveryOptions.first()
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
                val dessertsTotal = _dessertItems.value.sumOf { dessert -> dessert.subtotal }
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
    }

    fun setComentarios(comentarios: String) {
        _comentarios.value = comentarios
    }

    fun addToCart(pizza: Pizza, tamano: TamanoPizza) {
        updateCartItems { current ->
            current.add(
                CartItem(
                    pizza = pizza,
                    tamano = tamano,
                    sizeName = tamano.nombre,
                    unitPrice = tamano.precioBase,
                    cantidad = 1
                )
            )
        }
    }

    fun removeFromCart(pizza: Pizza, tamano: TamanoPizza) {
        updateCartItems { current ->
            val index = current.indexOfFirst {
                !it.isCombo && it.pizza?.nombre == pizza.nombre && it.tamano?.nombre == tamano.nombre
            }
            if (index >= 0) {
                current.removeAt(index)
            }
        }
    }

    fun addComboToCart(sizeName: String, portions: List<CartItemPortion>) {
        val normalizedSize = sizeName.trim()
        updateCartItems { current ->
            val price = calculateComboPrice(normalizedSize, portions)
            current.add(
                CartItem(
                    sizeName = normalizedSize,
                    unitPrice = price,
                    portions = portions,
                    cantidad = 1 // Combos are always added as new items with quantity 1
                )
            )
        }
    }

    fun incrementItem(itemId: String) {
        updateCartItems { current ->
            val index = current.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = current[index]
                if (item.isCombo) {
                    // For combos, add a new instance with the same configuration
                    val price = calculateComboPrice(item.sizeLabel, item.portions)
                    current.add(
                        CartItem(
                            id = UUID.randomUUID().toString(), // Ensure new ID for distinct combo
                            sizeName = item.sizeLabel,
                            unitPrice = price,
                            portions = item.portions,
                            cantidad = 1,
                            isGolden = item.isGolden
                        )
                    )
                } else {
                    // For regular pizzas, increment quantity
                    current[index] = item.copy(cantidad = item.cantidad + 1)
                }
            }
        }
    }

    fun decrementItem(itemId: String) {
        updateCartItems { current ->
            val index = current.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = current[index]
                if (item.cantidad > 1) {
                    current[index] = item.copy(cantidad = item.cantidad - 1)
                } else {
                    current.removeAt(index)
                }
            }
        }
    }

    fun removeItem(itemId: String) {
        updateCartItems { current ->
            current.removeAll { it.id == itemId }
        }
    }

    fun toggleGolden(itemId: String) {
        updateCartItems { current ->
            val index = current.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = current[index]
                current[index] = item.copy(isGolden = !item.isGolden)
            }
        }
    }

    fun updateItemPrice(itemId: String, newPrice: Double) {
        updateCartItems { current ->
            val index = current.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = current[index]
                current[index] = item.copy(unitPrice = newPrice)
            }
        }
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
            CartItemFormatter.toCustomerLines(item).forEach { line ->
                sb.appendLine(line)
            }
            result += item.subtotal
        }
        if (dessertItems.isNotEmpty()) {
            val postres = dessertItems.filter { it.postreOrExtra.esPostre }
            val combos = dessertItems.filter { it.postreOrExtra.esCombo }
            val bebidas = dessertItems.filter { it.postreOrExtra.esBebida }
            val extras = dessertItems.filter {
                !it.postreOrExtra.esPostre && !it.postreOrExtra.esCombo && !it.postreOrExtra.esBebida
            }
            sb.appendLine("-------------------------------")
            fun appendItems(title: String, items: List<CartItemPostre>) {
                if (items.isNotEmpty()) {
                    sb.appendLine(title)
                    items.forEach { item ->
                        sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}   $${String.format(Locale.getDefault(), "%.2f", item.subtotal)}")
                        result += item.subtotal
                    }
                }
            }
            appendItems("Postres:", postres)
            appendItems("Combos:", combos)
            appendItems("Bebidas:", bebidas)
            appendItems("Extras:", extras)
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${String.format(Locale.getDefault(), "%.2f", result)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

    fun buildCocinaTicket(
        user: User,
        deliveryAddress: String,
        deliveryService: DeliveryService?,
        timestamp: Long,
        dailyOrderNumber: Int
    ): String {
        // Si la orden no tiene pizzas, no se imprime el ticket
        val cartItems = _cartItems.value
        val desserts = _dessertItems.value
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hora = formatter.format(Date(timestamp))
        val entrega = deliveryService ?: _selectedDelivery.value
        val deliveryType = entrega?.type ?: DeliveryType.PASAN
        val deliveryTypeLabel = getDeliveryTypeLabel(deliveryType)
        val trimmedAddress = deliveryAddress.trim()
        val detail = when {
            trimmedAddress.isNotEmpty() && deliveryType != DeliveryType.TOTODO -> trimmedAddress
            deliveryType == DeliveryType.DOMICILIO && entrega?.zona?.isNotBlank() == true -> entrega.zona
            else -> null
        }?.takeIf { it.isNotBlank() && it != deliveryTypeLabel }
        val detailLabel = if (trimmedAddress.isNotEmpty()) "Dirección" else "Detalle"
        val clienteNombre = user.nombre.ifBlank { "Cliente" }

        val sb = StringBuilder()
        sb.appendLine("ORDEN PARA COCINA")
        sb.appendLine("Hora: $hora - Orden: $dailyOrderNumber")
        sb.appendLine("Cliente: $clienteNombre : $deliveryTypeLabel")
        sb.appendLine("-------------------------------")

        if (cartItems.isEmpty()) {
            sb.appendLine("Sin productos")
        } else {
            cartItems.forEach { item ->
                CartItemFormatter.toKitchenLines(item).forEach { line ->
                    sb.appendLine(line)
                }
                sb.appendLine()
            }
        }

        if (desserts.isNotEmpty()) {
            val postres = desserts.filter { it.postreOrExtra.esPostre }
            val combos = desserts.filter { it.postreOrExtra.esCombo }
            val bebidas = desserts.filter { it.postreOrExtra.esBebida }
            val extras = desserts.filter {
                !it.postreOrExtra.esPostre && !it.postreOrExtra.esCombo && !it.postreOrExtra.esBebida
            }
            sb.appendLine("-------------------------------")
            if (postres.isNotEmpty()) {
                sb.appendLine("Postres:")
                postres.forEach { item ->
                    sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (combos.isNotEmpty()) {
                sb.appendLine("Combos:")
                combos.forEach { item ->
                    sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (bebidas.isNotEmpty()) {
                sb.appendLine("Bebidas:")
                bebidas.forEach { item ->
                    sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (extras.isNotEmpty()) {
                sb.appendLine("Extras:")
                extras.forEach { item ->
                    sb.appendLine("${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
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

    suspend fun saveOrder(
        user: User,
        deliveryAddress: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): OrderEntity {
        val gson = Gson()
        val itemsJson = gson.toJson(_cartItems.value)
        val dessertsJson = gson.toJson(_dessertItems.value)
        val userJson = gson.toJson(user)
        val currentDeliveryService = _selectedDelivery.value
        val deliveryType = currentDeliveryService?.type ?: DeliveryType.PASAN
        val deliveryPrice = currentDeliveryService?.price ?: 0
        val isDeliveried = deliveryType == DeliveryType.DOMICILIO
        val isWalkingDelivery = deliveryType == DeliveryType.CAMINANDO
        val isTOTODO = deliveryType == DeliveryType.TOTODO
        val precioTOTODO: Double
        val descuentoTOTODO: Double
        if (isTOTODO) {
            precioTOTODO = calculateTOTODOPrice(total.value)
            descuentoTOTODO = total.value - precioTOTODO
        } else {
            precioTOTODO = 0.0
            descuentoTOTODO = 0.0
        }

        val resolvedDeliveryAddress = when {
            deliveryAddress.isNotBlank() -> deliveryAddress
            currentDeliveryService != null &&
                (deliveryType == DeliveryType.DOMICILIO || deliveryType == DeliveryType.CAMINANDO) ->
                currentDeliveryService.zona
            else -> ""
        }

        val orderEntity = OrderEntity(
            itemsJson = itemsJson,
            total = total.value,
            timestamp = timestamp,
            userJson = userJson,
            deliveryServicePrice = deliveryPrice,
            isDeliveried = isDeliveried,
            isWalkingDelivery = isWalkingDelivery,
            dessertsJson = dessertsJson,
            comentarios = comentarios.value,
            deliveryAddress = resolvedDeliveryAddress,
            deliveryType = deliveryType,
            isTOTODO = isTOTODO,
            precioTOTODO = precioTOTODO,
            descuentoTOTODO = descuentoTOTODO
        )

        return orderRepository.addOrder(orderEntity)
    }

    private fun getDeliveryTypeLabel(type: DeliveryType): String {
        return when (type) {
            DeliveryType.PASAN -> "PASAN"
            DeliveryType.CAMINANDO -> "CAMINANDO"
            DeliveryType.TOTODO -> "TOTODO"
            DeliveryType.DOMICILIO -> "ENVIO"
        }
    }

    private fun calculateComboPrice(sizeName: String, portions: List<CartItemPortion>): Double {
        val candidates = portions.mapNotNull { portion ->
            MenuData.pizzas.firstOrNull { it.nombre == portion.pizzaName }
                ?.tamanos
                ?.firstOrNull { it.nombre.equals(sizeName, ignoreCase = true) }
                ?.precioBase
        }
        return candidates.maxOrNull() ?: 0.0
    }

    private fun updateCartItems(block: (MutableList<CartItem>) -> Unit) {
        val current = _cartItems.value.toMutableList()
        block(current)
        _cartItems.value = current // Directly assign the modified list
    }

    private fun calculateTOTODOPrice(total: Double): Double {
        val discounted = total * 0.9
        val decimals = discounted - discounted.toInt()
        return if (decimals < 0.5) {
            floor(discounted)
        } else {
            ceil(discounted)
        }
    }
}

class CartViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
