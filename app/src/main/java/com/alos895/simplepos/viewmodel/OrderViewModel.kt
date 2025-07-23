package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.data.PizzeriaData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository(application)
    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders

    fun loadOrders() {
        viewModelScope.launch {
            _orders.value = repository.getOrders()
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

    fun buildOrderTicket(order: OrderEntity): String {
        val info = PizzeriaData.info
        val cartItems = getCartItems(order)
        val sb = StringBuilder()
        sb.appendLine(info.logoAscii)
        sb.appendLine(info.nombre)
        sb.appendLine(info.telefono)
        sb.appendLine(info.direccion)
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
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
            sb.appendLine("Ingredientes:")
            item.pizza.ingredientesBase.forEach { ingrediente ->
                sb.appendLine("- ${ingrediente.nombre}")
            }
            sb.appendLine()
        }
        sb.appendLine("-------------------------------")
        return sb.toString()
    }
}
