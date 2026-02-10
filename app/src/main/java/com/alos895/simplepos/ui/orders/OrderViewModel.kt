package com.alos895.simplepos.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.model.PaymentPart
import com.alos895.simplepos.model.DeliveryType
import com.alos895.simplepos.model.User
import com.google.gson.Gson
import com.alos895.simplepos.ui.common.CartItemFormatter
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

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository(application)
    private val gson = Gson()
    // For storing the full list of orders fetched from the repository
    private val _rawOrders = MutableStateFlow<List<OrderEntity>>(emptyList())

    // For managing the selected date for filtering
    private val _selectedDate = MutableStateFlow(getToday()) // Default to today
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()

    // The publicly exposed list of orders, filtered by selectedDate
    val orders: StateFlow<List<OrderEntity>> = combine(
        _rawOrders,
        _selectedDate
    ) { rawOrders, date ->
        if (date == null) {
            rawOrders // Show all if no date is selected
        } else {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val selectedDay = sdf.format(date)
            rawOrders.filter { order ->
                sdf.format(Date(order.timestamp)) == selectedDay
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            val currentDate : Long = _selectedDate.value.time
            _rawOrders.value = repository.getOrdersByDate(currentDate)
        }
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadOrders()
    }

    fun clearPaymentBreakdown(orderId: Long) {
        viewModelScope.launch {
            repository.clearPaymentBreakdown(orderId)
            loadOrders()
        }
    }

    fun deleteOrderLogical(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrderLogical(orderId)
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

        // Busca si ya existe un pago con ese método
        val existing = paymentParts.find { it.method == method }
        if (existing != null) {
            // reemplaza usando copy (inmutabilidad)
            val updated = existing.copy(amount = amount)
            paymentParts.remove(existing)
            paymentParts.add(updated)
        } else {
            // agrega nuevo
            paymentParts.add(PaymentPart(method, amount))
        }

        // actualiza el JSON
        order.paymentBreakdownJson = gson.toJson(paymentParts)

        viewModelScope.launch {
            repository.updateOrder(order)
            loadOrders()
        }
    }

    fun updateOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.updateOrder(order)
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
        if (orderEntity.dailyOrderNumber > 0) {
            return orderEntity.dailyOrderNumber
        }
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val orderDay = sdf.format(Date(orderEntity.timestamp))
        // Filter from _rawOrders as it contains all orders for potential daily numbering
        val sameDayOrders = _rawOrders.value
            .filter { !it.isDeleted && sdf.format(Date(it.timestamp)) == orderDay }
            .sortedBy { it.timestamp }
        val index = sameDayOrders.indexOfFirst { it.id == orderEntity.id }
        return if (index >= 0) index + 1 else 0
    }

    fun getDeliverySummary(order: OrderEntity): String {
        val trimmedAddress = order.deliveryAddress.trim()
        if (trimmedAddress.isNotEmpty()) {
            return trimmedAddress
        }

        return getDeliveryTypeLabel(order)
    }

    fun getDeliveryTypeLabel(order: OrderEntity): String {
        return when (order.deliveryType) {
            DeliveryType.PASAN -> "PASAN"
            DeliveryType.CAMINANDO -> "CAMINANDO"
            DeliveryType.TOTODO -> "TOTODO"
            DeliveryType.DOMICILIO -> "ENVIO"
        }
    }

    fun getDeliveryDetail(order: OrderEntity): String? {
        val summary = getDeliverySummary(order)
        val typeLabel = getDeliveryTypeLabel(order)
        return summary.takeIf { it.isNotBlank() && it != typeLabel }
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
        sb.appendLine("Direccion: ${getDeliverySummary(order)}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            CartItemFormatter.toCustomerLines(item).forEach { line ->
                sb.appendLine(line)
            }
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
                        sb.appendLine(
                            "${item.cantidad}x ${item.postreOrExtra.nombre}   $${"%.2f".format(item.subtotal)}"
                        )
                    }
                }
            }
            appendItems("Postres:", postres)
            appendItems("Combos:", combos)
            appendItems("Bebidas:", bebidas)
            appendItems("Extras:", extras)
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
        sb.appendLine("Cliente: ${user?.nombre ?: "Cliente"} : ${getDeliveryTypeLabel(order)}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            CartItemFormatter.toKitchenLines(item).forEach { line ->
                sb.appendLine(line)
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
            val linesForTicket = CartItemFormatter.toCustomerLines(item)
            linesForTicket.firstOrNull()?.let { header ->
                sb.appendLine("- ${header}")
            }
            linesForTicket.drop(1).forEach { detail ->
                sb.appendLine("  ${detail}")
            }
        }
        val desserts = getDessertItems(order)
        if (desserts.isNotEmpty()) {
            val postres = desserts.filter { it.postreOrExtra.esPostre }
            val combos = desserts.filter { it.postreOrExtra.esCombo }
            val bebidas = desserts.filter { it.postreOrExtra.esBebida }
            val extras = desserts.filter {
                !it.postreOrExtra.esPostre && !it.postreOrExtra.esCombo && !it.postreOrExtra.esBebida
            }
            if (postres.isNotEmpty()) {
                sb.appendLine("Postres:")
                postres.forEach { item ->
                    sb.appendLine("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (combos.isNotEmpty()) {
                sb.appendLine("Combos:")
                combos.forEach { item ->
                    sb.appendLine("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (bebidas.isNotEmpty()) {
                sb.appendLine("Bebidas:")
                bebidas.forEach { item ->
                    sb.appendLine("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
            if (extras.isNotEmpty()) {
                sb.appendLine("Extras:")
                extras.forEach { item ->
                    sb.appendLine("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
                }
            }
        }
        if (order.comentarios.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            sb.appendLine("Comentarios:")
            sb.appendLine(order.comentarios)
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("Envío/Entrega: ${getDeliverySummary(order)}")
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
