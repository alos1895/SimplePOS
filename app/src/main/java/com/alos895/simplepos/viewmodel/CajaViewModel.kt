package com.alos895.simplepos.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import com.alos895.simplepos.model.DailyStats
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.model.PaymentPart
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
    val gson = Gson()
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
        date: Date,
        orders: List<OrderEntity>,
        transactions: List<TransactionEntity>
    ): DailyStats {
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
        var totalIngresosCapturados = 0.0
        var totalGastosCapturados = 0.0
        var totalOrdenesEfectivo = 0.0
        var totalOrdenesTarjeta = 0.0
        var totalSoloOrdenes = 0.0

        val gson = Gson()
        val paymentPartListType = object : com.google.gson.reflect.TypeToken<List<PaymentPart>>() {}.type

        orders.filter { !it.isDeleted }.forEach { order ->
            val cartItems = getCartItems(order)
            val dessertItems = getDessertItems(order)

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

            totalCaja += order.total
            totalSoloOrdenes += order.total

            try {
                val paymentParts: List<PaymentPart>? = gson.fromJson(order.paymentBreakdownJson, paymentPartListType)
                paymentParts?.forEach { part ->
                    when (part.method) {
                        PaymentMethod.EFECTIVO -> totalOrdenesEfectivo += part.amount
                        PaymentMethod.TRANSFERENCIA -> totalOrdenesTarjeta += part.amount
                    }
                }
            } catch (e: Exception) {
                Log.e("CajaViewModel", "Error parsing paymentBreakdownJson", e)
            }
        }

        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INGRESO -> {
                    totalIngresosCapturados += transaction.amount
                    totalCaja += transaction.amount
                }
                TransactionType.GASTO -> {
                    totalGastosCapturados += transaction.amount
                    totalCaja -= transaction.amount
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
            egresosCapturados = totalGastosCapturados,
            totalOrdenesEfectivo = totalOrdenesEfectivo,
            totalOrdenesTarjeta = totalOrdenesTarjeta,
            totalEfectivoCaja = totalOrdenesEfectivo + totalIngresosCapturados - totalGastosCapturados,
            ordenesNoPagadas = (totalOrdenesEfectivo + totalOrdenesTarjeta - totalSoloOrdenes )
        )
    }


    fun buildCajaReport(dailyStats: DailyStats): String {
        val sdfReportDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfReportTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val reportDateStr = sdfReportDate.format(_selectedDate.value) // Use the selected date for the report title
        
        val sb = StringBuilder()
        val formatAmount = { amount: Double -> "$${"%,.2f".format(amount)}" }
        sb.appendLine("REPORTE DE CAJA: $reportDateStr")
        sb.appendLine("Hora Gen.: ${sdfReportTime.format(Date())}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("RESUMEN DE ÓRDENES")
        sb.appendLine("Órdenes totales: ${dailyStats.ordenes}")
        sb.appendLine("Órdenes no pagadas: ${formatAmount(dailyStats.ordenesNoPagadas)}")
        sb.appendLine()
        sb.appendLine("PIZZAS")
        sb.appendLine("  Chicas   : ${dailyStats.pizzasChicas}")
        sb.appendLine("  Medianas : ${dailyStats.pizzasMedianas}")
        sb.appendLine("  Grandes  : ${dailyStats.pizzasGrandes}")
        sb.appendLine("  Total    : ${dailyStats.pizzas}")
        sb.appendLine()
        sb.appendLine("POSTRES Y EXTRAS")
        sb.appendLine("  Postres  : ${dailyStats.postres}")
        sb.appendLine("  Extras   : ${dailyStats.extras}")
        sb.appendLine()
        sb.appendLine("ENVIOS")
        sb.appendLine("  Total envíos: ${dailyStats.envios}")
        sb.appendLine()
        sb.appendLine("INGRESOS POR VENTAS")
        sb.appendLine("  Pizzas    : ${formatAmount(dailyStats.ingresosPizzas)}")
        sb.appendLine("  Postres   : ${formatAmount(dailyStats.ingresosPostres)}")
        sb.appendLine("  Extras    : ${formatAmount(dailyStats.ingresosExtras)}")
        sb.appendLine("  Envíos    : ${formatAmount(dailyStats.ingresosEnvios)}")
        sb.appendLine()
        sb.appendLine("PAGOS POR MÉTODO")
        sb.appendLine("  Efectivo     : ${formatAmount(dailyStats.totalOrdenesEfectivo)}")
        sb.appendLine("  Transferencia: ${formatAmount(dailyStats.totalOrdenesTarjeta)}")
        sb.appendLine()
        sb.appendLine("MOVIMIENTOS MANUALES")
        sb.appendLine("  Ingresos : ${formatAmount(dailyStats.ingresosCapturados)}")
        sb.appendLine("  Gastos   : ${formatAmount(dailyStats.egresosCapturados)}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("TOTAL EN CAJA: ${formatAmount(dailyStats.totalCaja)}")
        sb.appendLine("Total en efectivo en caja")
        sb.appendLine(formatAmount(dailyStats.totalEfectivoCaja))
        sb.appendLine("--------------------------------------------------")
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
