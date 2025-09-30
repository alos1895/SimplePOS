package com.alos895.simplepos.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.model.PaymentPart
import com.alos895.simplepos.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val gson = Gson()
    private val _rawOrders = MutableStateFlow<List<OrderEntity>>(emptyList())
    private val _selectedDate = MutableStateFlow(getToday())
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()

    private val _deliveryOptions = MutableStateFlow<List<DeliveryService>>(emptyList())
    val deliveryOptions: StateFlow<List<DeliveryService>> = _deliveryOptions.asStateFlow()

    private val ingredientesPorId by lazy {
        menuRepository.getIngredientes().associateBy { it.id }
    }

    val orders: StateFlow<List<OrderEntity>> = combine(
        _rawOrders,
        _selectedDate
    ) { rawOrders, date ->
        if (date == null) {
            rawOrders
        } else {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val selectedDay = sdf.format(date)
            rawOrders.filter { order ->
                sdf.format(Date(order.timestamp)) == selectedDay
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        _deliveryOptions.value = menuRepository.getDeliveryOptions()
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            val currentDate: Long = _selectedDate.value.time
            _rawOrders.value = orderRepository.getOrdersByDate(currentDate)
        }
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadOrders()
    }

    fun clearPaymentBreakdown(orderId: Long) {
        viewModelScope.launch {
            orderRepository.clearPaymentBreakdown(orderId)
            loadOrders()
        }
    }

    fun deleteOrderLogical(orderId: Long) {
        viewModelScope.launch {
            orderRepository.deleteOrderLogical(orderId)
            loadOrders()
        }
    }

    fun isOrderPaid(order: OrderEntity): Boolean {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<PaymentPart>>() {}.type
            val paymentParts: List<PaymentPart> = gson.fromJson(order.paymentBreakdownJson, type) ?: emptyList()
            val totalPaid = paymentParts.sumOf { it.amount }
            totalPaid >= order.total
        } catch (e: Exception) {
            false
        }
    }

    fun getPaymentAmount(order: OrderEntity, method: PaymentMethod): Double {
        val type = object : TypeToken<List<PaymentPart>>() {}.type
        val paymentParts: List<PaymentPart> =
            gson.fromJson<List<PaymentPart>>(order.paymentBreakdownJson, type) ?: emptyList()
        return paymentParts.find { it.method == method }?.amount ?: 0.0
    }

    fun updatePayment(order: OrderEntity, amount: Double, method: PaymentMethod) {
        val type = object : TypeToken<List<PaymentPart>>() {}.type
        val paymentParts: MutableList<PaymentPart> =
            gson.fromJson<List<PaymentPart>>(order.paymentBreakdownJson, type)?.toMutableList()
                ?: mutableListOf()

        val existing = paymentParts.find { it.method == method }
        if (existing != null) {
            val updated = existing.copy(amount = amount)
            paymentParts.remove(existing)
            paymentParts.add(updated)
        } else {
            paymentParts.add(PaymentPart(method, amount))
        }

        order.paymentBreakdownJson = gson.toJson(paymentParts)

        viewModelScope.launch {
            orderRepository.updateOrder(order)
            loadOrders()
        }
    }

    fun updateOrder(order: OrderEntity) {
        viewModelScope.launch {
            orderRepository.updateOrder(order)
            loadOrders()
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun getCartItems(order: OrderEntity): List<CartItem> {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(order.itemsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUser(order: OrderEntity): User? {
        return try {
            val gson = Gson()
            gson.fromJson(order.userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getDessertItems(order: OrderEntity): List<CartItemPostre> {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<CartItemPostre>>() {}.type
            gson.fromJson(order.dessertsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDailyOrderNumber(orderEntity: OrderEntity): Int {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val orderDay = sdf.format(Date(orderEntity.timestamp))
        val sameDayOrders = _rawOrders.value
            .filter { !it.isDeleted && sdf.format(Date(it.timestamp)) == orderDay }
            .sortedBy { it.timestamp }
        val index = sameDayOrders.indexOfFirst { it.id == orderEntity.id }
        return if (index >= 0) index + 1 else 0
    }

    fun buildOrderTicket(order: OrderEntity): String {
        val info = PizzeriaData.info
        val cartItems = getCartItems(order)
        val dessertItems = getDessertItems(order)
        val user = getUser(order)
        val dailyNumber = getDailyOrderNumber(order)
        val sb = StringBuilder()
        sb.appendLine(info.logoAscii)
        sb.appendLine(info.nombre)
        sb.appendLine(info.telefono)
        sb.appendLine(info.direccion)
        if (dailyNumber > 0) {
            sb.appendLine("Orden #$dailyNumber")
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("Cliente: ${user?.nombre ?: "Cliente"}")
        sb.appendLine("Direccion: ${if (order.isDeliveried && order.deliveryAddress.isNotBlank()) order.deliveryAddress else "Recoge en tienda"}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine(
                "${item.cantidad} x ${item.pizza.nombre} ${item.tamano.nombre.uppercase(Locale.getDefault())}   $${"%.2f".format(item.subtotal)}"
            )
        }
        if (dessertItems.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            dessertItems.forEach { item ->
                sb.appendLine(
                    "${item.cantidad}x ${item.postreOrExtra.nombre}   $${"%.2f".format(item.subtotal)}"
                )
            }
        }
        sb.appendLine("-------------------------------")
        if (order.isDeliveried) {
            sb.appendLine("Servicio a domicilio: $${"%.2f".format(order.deliveryServicePrice.toDouble())}")
        }
        if (order.comentarios.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            sb.appendLine("COMENTARIOS:")
            sb.appendLine(order.comentarios)
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${"%.2f".format(order.total)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

    fun buildCocinaTicket(order: OrderEntity): String {
        val cartItems = getCartItems(order)
        val user = getUser(order)
        val sb = StringBuilder()
        sb.appendLine("ORDEN PARA COCINA")
        sb.appendLine(
            "Hora: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.timestamp))} - Orden: ${getDailyOrderNumber(order)}"
        )
        val destino = if (order.isDeliveried && order.deliveryAddress.isNotBlank()) {
            order.deliveryAddress
        } else {
            "Pasan/Caminando"
        }
        sb.appendLine("Cliente: ${user?.nombre ?: "Cliente"} - $destino")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre.uppercase(Locale.getDefault())}")
            item.pizza.ingredientesBaseIds.forEach { ingredienteId ->
                ingredientesPorId[ingredienteId]?.let { ingrediente ->
                    sb.appendLine("- ${ingrediente.nombre}")
                }
            }
            sb.appendLine()
        }
        sb.appendLine("-------------------------------")
        if (order.comentarios.isNotEmpty()) {
            sb.appendLine("COMENTARIOS:")
            sb.appendLine(order.comentarios)
            sb.appendLine("-------------------------------")
        }
        return sb.toString()
    }

    fun buildDeleteTicket(order: OrderEntity): String {
        val user = getUser(order)
        val sb = StringBuilder()
        sb.appendLine("TICKET DE ORDEN BORRADA")
        sb.appendLine("-------------------------------")
        sb.appendLine("Orden #${getDailyOrderNumber(order)}")
        sb.appendLine("Nombre: ${user?.nombre ?: "Desconocido"}")
        sb.appendLine("Fecha: ${formatDate(order.timestamp)}")
        sb.appendLine("Total: $${"%.2f".format(order.total)}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Items:")
        getCartItems(order).forEach { item ->
            sb.appendLine("- ${item.cantidad}x ${item.pizza.nombre} (${item.tamano.nombre})")
        }
        if (getDessertItems(order).isNotEmpty()) {
            sb.appendLine("Postres:")
            getDessertItems(order).forEach { item ->
                sb.appendLine("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
            }
        }
        if (order.comentarios.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            sb.appendLine("Comentarios:")
            sb.appendLine(order.comentarios)
        }
        if (order.isDeliveried) {
            sb.appendLine("-------------------------------")
            sb.appendLine("Envío a: ${order.deliveryAddress}")
        } else {
            sb.appendLine("-------------------------------")
            sb.appendLine("Recogida en tienda")
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("¡Orden eliminada correctamente!")
        return sb.toString()
    }

    companion object {
        fun getToday(): Date {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }
    }
}
