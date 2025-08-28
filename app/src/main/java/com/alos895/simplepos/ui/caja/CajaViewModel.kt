package com.alos895.simplepos.ui.caja

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import com.alos895.simplepos.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CajaViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)
    private val gson = Gson()

    private val _selectedDate = MutableStateFlow(getToday())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // Flujos públicos para órdenes y transacciones
    private val _ordersForDate = MutableStateFlow<List<OrderEntity>>(emptyList())
    val ordersForDate: StateFlow<List<OrderEntity>> = _ordersForDate.asStateFlow()

    private val _transactionsForDate = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactionsForDate: StateFlow<List<TransactionEntity>> = _transactionsForDate.asStateFlow()

    val dailyStats: StateFlow<DailyStats> = combine(
        _selectedDate,
        _ordersForDate,
        _transactionsForDate
    ) { date, orders, transactions ->
        calculateDailyStatsInternal(date, orders, transactions)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DailyStats()
    )

    init {
        loadDataForSelectedDate()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadDataForSelectedDate()
    }

    fun refreshCajaData() {
        loadDataForSelectedDate()
    }

    private fun loadDataForSelectedDate() {
        viewModelScope.launch {
            val currentTime = _selectedDate.value.time
            _ordersForDate.value = orderRepository.getOrdersByDate(currentTime)
            _transactionsForDate.value = transactionsRepository.getTransactionsByDate(currentTime)
        }
    }

    // ---------------- CSV y compartición ----------------

    fun generateCsv(orders: List<OrderEntity>, transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Cabeceras de órdenes
        sb.appendLine("Tipo,ID,Fecha,Total,Detalle,Pago,Efectivo,Transferencia,Delivery")
        orders.forEach { order ->
            val paymentParts: List<PaymentPart> = try {
                gson.fromJson(order.paymentBreakdownJson, object : TypeToken<List<PaymentPart>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }

            val efectivo = paymentParts.filter { it.method == PaymentMethod.EFECTIVO }.sumOf { it.amount }
            val tarjeta = paymentParts.filter { it.method == PaymentMethod.TRANSFERENCIA }.sumOf { it.amount }

            sb.appendLine(
                listOf(
                    "ORDEN",
                    order.id,
                    sdf.format(Date(order.timestamp)),
                    order.total,
                    order.itemsJson.replace(",", ";"),
                    order.paymentBreakdownJson.replace(",", ";"),
                    efectivo,
                    tarjeta,
                    order.deliveryServicePrice
                ).joinToString(",")
            )
        }

        // Cabeceras de transacciones
        sb.appendLine()
        sb.appendLine("TipoTransaccion,ID,Fecha,Tipo,Gasto/Ingreso,Monto,Detalle")
        transactions.forEach { tx ->
            sb.appendLine(
                listOf(
                    "TRANSACCION",
                    tx.id,
                    sdf.format(Date(tx.date)),
                    tx.type.name,
                    if (tx.type == TransactionType.INGRESO) "Ingreso" else "Gasto",
                    tx.amount,
                    tx.concept.replace(",", ";")
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun saveCsvToFile(context: Context, csvContent: String, fileName: String = "caja_export.csv"): File {
        val file = File(context.cacheDir, fileName)
        file.writeText(csvContent)
        return file
    }

    fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir reporte CSV"))
    }

    // ---------------- Helpers ----------------

    private fun calculateDailyStatsInternal(date: Date, orders: List<OrderEntity>, transactions: List<TransactionEntity>): DailyStats {
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

        val paymentPartListType = object : TypeToken<List<PaymentPart>>() {}.type

        orders.filter { !it.isDeleted }.forEach { order ->
            val cartItems = getCartItems(order)
            val dessertItems = getDessertItems(order)

            cartItems.forEach { item ->
                totalPizzas += item.cantidad
                pizzaRevenue += item.subtotal
                when (item.tamano.nombre.lowercase(Locale.getDefault())) {
                    "chica" -> totalChicas += item.cantidad
                    "mediana" -> totalMedianas += item.cantidad
                    "grande", "extra grande" -> totalGrandes += item.cantidad
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
            } catch (_: Exception) {}
        }

        transactions.forEach { tx ->
            when (tx.type) {
                TransactionType.INGRESO -> {
                    totalIngresosCapturados += tx.amount
                    totalCaja += tx.amount
                }
                TransactionType.GASTO -> {
                    totalGastosCapturados += tx.amount
                    totalCaja -= tx.amount
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
            ordenesNoPagadas = (totalOrdenesEfectivo + totalOrdenesTarjeta - totalSoloOrdenes)
        )
    }

    private fun getCartItems(order: OrderEntity): List<CartItem> {
        return try {
            gson.fromJson(order.itemsJson, object : TypeToken<List<CartItem>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getDessertItems(order: OrderEntity): List<CartItemPostre> {
        return try {
            gson.fromJson(order.dessertsJson, object : TypeToken<List<CartItemPostre>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildCajaReport(dailyStats: DailyStats): String {
        val sdfReportDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfReportTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val reportDateStr = sdfReportDate.format(_selectedDate.value)

        val sb = StringBuilder()
        val formatAmount = { amount: Double -> "$${"%,.2f".format(amount)}" }

        sb.appendLine("REPORTE DE CAJA: $reportDateStr")
        sb.appendLine("Hora Gen.: ${sdfReportTime.format(Date())}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("RESUMEN DE ÓRDENES")
        sb.appendLine("Órdenes totales: ${dailyStats.ordenes}")
        sb.appendLine("Órdenes no pagadas: ${formatAmount(dailyStats.ordenesNoPagadas)}")
        sb.appendLine()
        sb.appendLine("TOTAL EN CAJA: ${formatAmount(dailyStats.totalCaja)}")
        sb.appendLine("Total en efectivo en caja: ${formatAmount(dailyStats.totalEfectivoCaja)}")
        return sb.toString()
    }

    companion object {
        fun getToday(): Date = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
