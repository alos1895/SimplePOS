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

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository(application)
    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders
    private val _selectedDate = MutableStateFlow<Date?>(getToday())
    val selectedDate: StateFlow<Date?> = _selectedDate

    fun loadOrders() {
        viewModelScope.launch {
            _orders.value = repository.getOrders()
        }
    }

    fun setSelectedDate(date: Date?) {
        _selectedDate.value = date
    }

    fun ordersBySelectedDate(orders: List<OrderEntity>, selectedDate: Date?): List<OrderEntity> {
        if (selectedDate == null) return orders
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val selectedDay = sdf.format(selectedDate)
        return orders.filter {
            sdf.format(Date(it.timestamp)) == selectedDay
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun getCartItems(order: OrderEntity): List<CartItem> {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(order.itemsJson, type)
    }

    fun getUser(order: OrderEntity): User {
        val gson = Gson()
        return gson.fromJson(order.userJson, User::class.java)
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
        val sameDay = orders.value
            .filter { sdf.format(Date(it.timestamp)) == orderDay }
            .sortedBy { it.timestamp }
        val index = sameDay.indexOfFirst { it.id == order.id }
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
        //TODO: Preguntar a Monica si quiere mostrar el nombre del cliente
        //sb.appendLine("Cliente: ${user.nombre}")
        sb.appendLine("Teléfono: ${user.telefono}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
        }
        if (dessertItems.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            dessertItems.forEach { item ->
                sb.appendLine("${item.cantidad}x ${item.postre.nombre}   $${"%.2f".format(item.subtotal)}")
            }
        }
        sb.appendLine("-------------------------------")
        if (order.isDeliveried) {
            sb.appendLine("Servicio a domicilio: $${"%.2f".format(order.deliveryServicePrice.toDouble())}")
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${"%.2f".format(order.total)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

    fun buildCocinaTicket(order: OrderEntity): String {
        val cartItems = getCartItems(order)
        val sb = StringBuilder()
        sb.appendLine("ORDEN PARA COCINA")
        sb.appendLine("Hora: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.timestamp))}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
            sb.appendLine("Ingredientes:___ NOTAS___")
            item.pizza.ingredientesBaseIds.forEach { ingredienteId ->
                MenuData.ingredientes.find { it.id == ingredienteId }?.let { ingrediente ->
                    sb.appendLine("- ${ingrediente.nombre}")
                }
            }
            sb.appendLine()
        }
        sb.appendLine("-------------------------------")
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
