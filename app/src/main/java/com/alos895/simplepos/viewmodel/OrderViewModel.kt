package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.CartItemPostre
// No longer needs TransactionsRepository or DailyStats related imports here if only CajaViewModel handles them

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository(application)
    // private val repositoryTransaction = TransactionsRepository(application) // Removed if only CajaVM uses it

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders

    // _selectedDate, _dailyStats, _transactions (for daily stats), calculateDailyStats, refreshAllData, buildCajaReport were moved to CajaViewModel

    init {
        loadOrders() // Loads all non-deleted orders, or as per your repository's default getOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            // This should fetch all relevant orders for general display, not just for a specific date (unless this VM is only for a specific date view)
            // Assuming repository.getOrders() gets all non-deleted orders.
            _orders.value = repository.getOrders() 
        }
    }

    fun deleteOrderLogical(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrderLogical(orderId)
            loadOrders() // Refresh the list of all orders
        }
    }

    // ordersBySelectedDate was primarily for dailyStats, CajaViewModel handles its own date filtering now.
    // If you need a similar function for other purposes in OrderViewModel, you can keep/adapt it.

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun getCartItems(order: OrderEntity): List<CartItem> {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(order.itemsJson, type) ?: emptyList()
    }

    fun getUser(order: OrderEntity): User? { // Made User nullable as GSON can return null
        return try {
            val gson = Gson()
            gson.fromJson(order.userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getDessertItems(order: OrderEntity): List<CartItemPostre> {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<CartItemPostre>>() {}.type
        return try {
            gson.fromJson(order.dessertsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDailyOrderNumber(order: OrderEntity): Int {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val orderDay = sdf.format(Date(order.timestamp))
        // _orders.value now contains all orders loaded by this ViewModel.
        // Ensure this logic is correct if _orders is not guaranteed to be sorted or contain all orders for the day.
        // This might be better if OrderRepository provides a method to get daily order number directly from DB for robustness.
        val sameDayOrders = _orders.value
            .filter { !it.isDeleted && sdf.format(Date(it.timestamp)) == orderDay }
            .sortedBy { it.timestamp }
        val index = sameDayOrders.indexOfFirst { it.id == order.id }
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
        sb.appendLine("Direccion: ${if (order.isDeliveried) order.deliveryAddress else "Recoge en tienda"}")
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
        sb.appendLine("Cliente: ${user?.nombre ?: "Cliente"} - ${order.deliveryAddress.takeIf { it.isNotBlank() && order.isDeliveried } ?: "Pasan/Caminando"}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre.uppercase(Locale.getDefault())}")
            // Assuming MenuData.ingredientes is accessible and correct
            item.pizza.ingredientesBaseIds.forEach { ingredienteId ->
                MenuData.ingredientes.find { it.id == ingredienteId }?.let { ingrediente ->
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
}
