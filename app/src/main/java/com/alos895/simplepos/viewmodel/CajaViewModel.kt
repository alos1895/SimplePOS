package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import com.alos895.simplepos.model.DailyStats
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.google.gson.Gson
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

class CajaViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)

    private val _selectedDate = MutableStateFlow(getToday())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // Internal state for orders and transactions related to the selected date
    private val _ordersForDate = MutableStateFlow<List<OrderEntity>>(emptyList())
    private val _transactionsForDate = MutableStateFlow<List<TransactionEntity>>(emptyList())

    val dailyStats: StateFlow<DailyStats> = combine(
        _selectedDate,
        _ordersForDate,
        _transactionsForDate
    ) { date, orders, transactions ->
        calculateDailyStatsInternal(date, orders, transactions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DailyStats()
    )

    init {
        // Load data for the initial selected date (today)
        loadDataForSelectedDate()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadDataForSelectedDate() // Carga datos cuando la fecha cambia
    }

    fun refreshCajaData() {
        // This will reload data for the currently selected date
        loadDataForSelectedDate()
    }

    private fun loadDataForSelectedDate() {
        viewModelScope.launch {
            val currentDate : Long = _selectedDate.value.time
            _ordersForDate.value = orderRepository.getOrdersByDate(currentDate)
            _transactionsForDate.value = transactionsRepository.getTransactionsByDate(currentDate)
        }
    }
    
    // Helper to get CartItems from OrderEntity - needed for calculateDailyStatsInternal
    private fun getCartItems(order: OrderEntity): List<CartItem> {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(order.itemsJson, type) ?: emptyList()
    }

    // Helper to get DessertItems from OrderEntity - needed for calculateDailyStatsInternal
    private fun getDessertItems(order: OrderEntity): List<CartItemPostre> {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<CartItemPostre>>() {}.type
        return try {
            gson.fromJson(order.dessertsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }


    private fun calculateDailyStatsInternal(
        date: Date, // date parameter is for consistency, actual filtering happens in loadDataForSelectedDate
        orders: List<OrderEntity>,
        transactions: List<TransactionEntity>
    ): DailyStats {
        // The orders and transactions lists are already filtered for the selected date

        var totalPizzas = 0
        var totalChicas = 0
        var totalMedianas = 0
        var totalGrandes = 0
        var totalPostres = 0
        var totalExtras = 0
        var totalDelivery = 0
        var totalCaja = 0.0
        var pizzaRevenue = 0.0
        var postreRevenue = 0.0
        var extraRevenue = 0.0
        var deliveryRevenue = 0.0
        var totalIngresosCapturados = 0.0 // Renamed from totalIngresos to avoid confusion
        var totalGastosCapturados = 0.0 // Renamed from totalGastos to avoid confusion

        orders.forEach { order ->
            // No need to check for isDeleted or date here, as list is pre-filtered
            val cartItems = getCartItems(order) // Use helper
            val dessertItems = getDessertItems(order) // Use helper

            cartItems.forEach { item ->
                totalPizzas += item.cantidad
                pizzaRevenue += item.subtotal
                when (item.tamano.nombre.lowercase(Locale.getDefault())) {
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
            totalCaja += order.total // Summing up order totals
        }

        // Process transactions, which are already for the selected date
        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INGRESO -> {
                    totalIngresosCapturados += transaction.amount
                    totalCaja += transaction.amount // Add manual income to caja
                }
                TransactionType.GASTO -> {
                    totalGastosCapturados += transaction.amount
                    totalCaja -= transaction.amount // Subtract manual expenses from caja
                }
            }
        }

        return DailyStats(
            pizzas = totalPizzas,
            pizzasChicas = totalChicas,
            pizzasMedianas = totalMedianas,
            pizzasGrandes = totalGrandes,
            postres = totalPostres,
            extras = totalExtras,
            ordenes = orders.size,
            envios = totalDelivery,
            totalCaja = totalCaja,
            ingresosPizzas = pizzaRevenue,
            ingresosPostres = postreRevenue,
            ingresosExtras = extraRevenue,
            ingresosEnvios = deliveryRevenue,
            ingresosCapturados = totalIngresosCapturados,
            egresosCapturados = totalGastosCapturados
        )
    }

    fun buildCajaReport(dailyStats: DailyStats): String {
        val sdfReportDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfReportTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val reportDateStr = sdfReportDate.format(_selectedDate.value) // Use the selected date for the report title
        
        val sb = StringBuilder()
        sb.appendLine("REPORTE DE CAJA: $reportDateStr")
        sb.appendLine("Hora Gen.: ${sdfReportTime.format(Date())}") // Generation time
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
        sb.appendLine("Ingresos por Ventas:")
        sb.appendLine("  Pizzas: $${"%.2f".format(dailyStats.ingresosPizzas)}")
        sb.appendLine("  Postres: $${"%.2f".format(dailyStats.ingresosPostres)}")
        sb.appendLine("  Extras: $${"%.2f".format(dailyStats.ingresosExtras)}")
        sb.appendLine("  Envíos: $${"%.2f".format(dailyStats.ingresosEnvios)}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Movimientos Manuales:")
        sb.appendLine("  Otros Ingresos: $${"%.2f".format(dailyStats.ingresosCapturados)}")
        sb.appendLine("  Gastos: $${"%.2f".format(dailyStats.egresosCapturados)}")
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL EN CAJA: $${"%.2f".format(dailyStats.totalCaja)}")
        sb.appendLine("-------------------------------")
        sb.appendLine("¡Gracias por su trabajo!")
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
