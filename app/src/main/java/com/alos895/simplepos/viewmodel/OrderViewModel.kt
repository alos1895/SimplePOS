package com.alos895.simplepos.viewmodel

import android.app.Application
import android.util.Log
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
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.DailyStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OrderRepository(application)
    private val repositoryTransaction = TransactionsRepository(application)
    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders

    private val _selectedDate = MutableStateFlow<Date?>(getToday())
    val selectedDate: StateFlow<Date?> = _selectedDate

    private val _dailyStats = MutableStateFlow(DailyStats())
    val dailyStats: StateFlow<DailyStats> =
        combine(_orders, _selectedDate) { orders, selectedDate ->
            calculateDailyStats(orders, selectedDate)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DailyStats())

    fun loadOrders() {
        viewModelScope.launch {
            _orders.value = repository.getOrders()
        }
    }

    fun setSelectedDate(date: Date?) {
        _selectedDate.value = date
    }

    fun deleteOrderLogical(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrderLogical(orderId)
            loadOrders()
        }
    }

    fun ordersBySelectedDate(orders: List<OrderEntity>, selectedDate: Date?): List<OrderEntity> {
        if (selectedDate == null) return orders.filter { !it.isDeleted }
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val selectedDay = sdf.format(selectedDate)
        return orders.filter {
            !it.isDeleted && sdf.format(Date(it.timestamp)) == selectedDay
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

    private suspend fun calculateDailyStats(
        orders: List<OrderEntity>,
        selectedDate: Date?
    ): DailyStats {
        if (selectedDate == null) return DailyStats()

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val selectedDay = sdf.format(selectedDate)
        val dayOrders = orders.filter {
            !it.isDeleted && sdf.format(Date(it.timestamp)) == selectedDay
        }

        var totalPizzas = 0
        var totalChicas = 0
        var totalMedianas = 0
        var totalGrandes = 0
        var totalPostres = 0
        var totalExtras = 0
        var totalDelivery = 0
        var totalRevenue = 0.0
        var pizzaRevenue = 0.0
        var postreRevenue = 0.0
        var extraRevenue = 0.0
        var deliveryRevenue = 0.0
        var totalIngresos = 0.0
        var totalGastos = 0.0

        dayOrders.forEach { order ->
            val cartItems = getCartItems(order)
            val dessertItems = getDessertItems(order)

            cartItems.forEach { item ->
                totalPizzas += item.cantidad
                pizzaRevenue += item.subtotal
                when (item.tamano.nombre.lowercase()) {
                    "chica" -> totalChicas += item.cantidad
                    "mediana" -> totalMedianas += item.cantidad
                    "extra grande", "grande" -> totalGrandes += item.cantidad
                }
            }

            dessertItems.forEach { item ->
                if (item.postreOrExtra.esPostre) {
                    totalPostres += item.cantidad
                    postreRevenue += item.subtotal
                } else {
                    totalExtras += item.cantidad
                    extraRevenue += item.subtotal
                }
            }

            if (order.isDeliveried) {
                totalDelivery++
                deliveryRevenue += order.deliveryServicePrice
            }

            totalRevenue += order.total
        }

        repositoryTransaction.getTransactionsForDate(date = selectedDate.time).forEach {
            if (it.type.name == "INGRESO") {
                totalIngresos += it.amount
            } else if (it.type.name == "EGRESO") {
                totalGastos += it.amount
            }
        }

        return DailyStats(
            pizzas = totalPizzas,
            pizzasChicas = totalChicas,
            pizzasMedianas = totalMedianas,
            pizzasGrandes = totalGrandes,
            postres = totalPostres,
            extras = totalExtras,
            ordenes = dayOrders.size,
            envios = totalDelivery,
            ingresos = totalRevenue,
            ingresosPizzas = pizzaRevenue,
            ingresosPostres = postreRevenue,
            ingresosExtras = extraRevenue,
            ingresosEnvios = deliveryRevenue,
            ingresosCapturados = totalIngresos,
            egresosCapturados = totalGastos
        )
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
        sb.appendLine("Cliente: ${user.nombre}")
        sb.appendLine("Direccion: ${if (order.isDeliveried) order.deliveryAddress else ""}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine( //tamaño en mayusculas
                "${item.cantidad} x ${item.pizza.nombre} ${item.tamano.nombre.toUpperCase(Locale.getDefault())}   $${
                    "%.2f".format(
                        item.subtotal
                    )
                }"
            )
        }
        if (dessertItems.isNotEmpty()) {
            sb.appendLine("-------------------------------")
            dessertItems.forEach { item ->
                sb.appendLine(
                    "${item.cantidad}x ${item.postreOrExtra.nombre}   $${
                        "%.2f".format(
                            item.subtotal
                        )
                    }"
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
        val sb = StringBuilder()
        sb.appendLine("ORDEN PARA COCINA")
        sb.appendLine(
            "Hora: ${
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date(order.timestamp))
            } - Orden: ${getDailyOrderNumber(order)}"
        )
        // nombre y direccion o PASAN
        sb.appendLine("Cliente: ${getUser(order).nombre} - ${order.deliveryAddress.takeIf { it.isNotEmpty() } ?: "Pasan o Caminando!"}")
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre.toUpperCase(Locale.getDefault())}")
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

    fun buildCajaReport(dailyStats: DailyStats): String {
        val sb = StringBuilder()
        sb.appendLine("REPORTE DE CAJA :${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(_selectedDate.value ?: Date())}")
        sb.appendLine("Hora: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Ordenes: ${dailyStats.ordenes}")
        sb.appendLine("Pizzas:")
        sb.appendLine("  Chicas: ${dailyStats.pizzasChicas}")
        sb.appendLine("  Medianas: ${dailyStats.pizzasMedianas}")
        sb.appendLine("  Grandes: ${dailyStats.pizzasGrandes}")
        sb.appendLine("  Total: ${dailyStats.pizzas}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Postres: ${dailyStats.postres}")
        sb.appendLine("Extras: ${dailyStats.extras}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Envios: ${dailyStats.envios}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Ingresos:")
        sb.appendLine("  Pizzas: $${"%.2f".format(dailyStats.ingresosPizzas)}")
        sb.appendLine("  Postres: $${"%.2f".format(dailyStats.ingresosPostres)}")
        sb.appendLine("  Extras: $${"%.2f".format(dailyStats.ingresosExtras)}")
        sb.appendLine("  Envíos: $${"%.2f".format(dailyStats.ingresosEnvios)}")
        sb.appendLine("  Total: $${"%.2f".format(dailyStats.ingresos)}")
        sb.appendLine("-------------------------------")
        sb.appendLine("¡Gracias por su trabajo!")
        return sb.toString()
    }

    fun buildDeleteTicket(order: OrderEntity): String {
        val sb = StringBuilder()
        sb.appendLine("TICKET DE ORDEN BORRADA")
        sb.appendLine("-------------------------------")
        sb.appendLine("Orden #${getDailyOrderNumber(order)}")
        sb.appendLine("Nombre: ${getUser(order)?.nombre ?: "Desconocido"}")
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
