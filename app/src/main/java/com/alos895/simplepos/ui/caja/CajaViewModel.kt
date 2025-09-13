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
import java.text.Normalizer
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

    fun generateCsvDetailed(orders: List<OrderEntity>, transactions: List<TransactionEntity>
    ): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val gson = Gson()

        // Cabeceras
        sb.appendLine("Orden,Lista Pizzas,Lista Extras,Lista Postres, Envio Total,Postres Total,Total")

        orders.forEach { order ->
            // Deserializar pizzas
            val itemsType = object : TypeToken<List<CartItem>>() {}.type
            val items: List<CartItem> = try {
                gson.fromJson(order.itemsJson ?: "[]", itemsType)
            } catch (e: Exception) {
                emptyList()
            }

            // Deserializar postres/extras
            val dessertsType = object : TypeToken<List<CartItemPostre>>() {}.type
            val desserts: List<CartItemPostre> = try {
                gson.fromJson(order.dessertsJson ?: "[]", dessertsType)
            } catch (e: Exception) {
                emptyList()
            }

            // Generar strings legibles
            val pizzasList = items.joinToString("; ") { item ->
                val cantidad = item.cantidad ?: 0
                val nombre = quitarAcentos(item.pizza?.nombre ?: "Desconocida")
                val tamano = quitarAcentos(item.tamano?.nombre ?: "Sin tamaño")
                "$cantidad x $nombre ($tamano)"
            }

            val postresList = desserts.filter { it.postreOrExtra?.esPostre == true }
                .joinToString("; ") { d ->
                    val cantidad = d.cantidad ?: 0
                    val nombre = quitarAcentos(d.postreOrExtra?.nombre ?: "Desconocido")
                    "$cantidad x $nombre"
                }

            val extrasList = desserts.filter { it.postreOrExtra?.esPostre == false }
                .joinToString("; ") { d ->
                    val cantidad = d.cantidad ?: 0
                    val nombre = quitarAcentos(d.postreOrExtra?.nombre ?: "Desconocido")
                    "$cantidad x $nombre"
                }


            // Calcular totales
            val postresTotal = desserts.filter { it.postreOrExtra.esPostre == true }
                .sumOf { it.subtotal ?: 0.0 }
            val envioTotal = order.deliveryServicePrice ?: 0.0
            val total = order.total ?: 0.0

            sb.appendLine(
                listOf(
                    order.id?.toString() ?: "Sin ID",
                    pizzasList,
                    extrasList,
                    postresList,
                    envioTotal,
                    postresTotal,
                    total
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    fun quitarAcentos(texto: String): String {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "") // elimina los diacríticos
    }

    fun saveCsvToFile(context: Context, csvContent: String, fileName: String = "caja_export_${Date().time}.csv"): File {
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
        var deliverysTOTODO = 0
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
        var totalDescuentosTOTODO = 0.0

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
            if (order.isTOTODO) {
                deliverysTOTODO++
            }

            totalCaja += order.total
            totalSoloOrdenes += order.total
            totalDescuentosTOTODO += order.descuentoTOTODO

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
            deliverysTOTODO = deliverysTOTODO,
            totalCaja = totalCaja - totalDescuentosTOTODO,
            ingresosPizzas = pizzaRevenue,
            ingresosPostres = postreRevenue,
            ingresosExtras = extraRevenue,
            ingresosEnvios = deliveryRevenue,
            ingresosCapturados = totalIngresosCapturados,
            egresosCapturados = totalGastosCapturados,
            totalOrdenesEfectivo = totalOrdenesEfectivo,
            totalOrdenesTarjeta = totalOrdenesTarjeta,
            totalEfectivoCaja = totalOrdenesEfectivo + totalIngresosCapturados - totalGastosCapturados,
            ordenesNoPagadas = (totalOrdenesEfectivo + totalOrdenesTarjeta - totalSoloOrdenes),
            totalDescuentosTOTODO = totalDescuentosTOTODO
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
        sb.appendLine("Envios: ${dailyStats.envios}")
        // Pizzas
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("PIZZAS")
        sb.appendLine("Chicas: ${dailyStats.pizzasChicas}")
        sb.appendLine("Medianas: ${dailyStats.pizzasMedianas}")
        sb.appendLine("Grandes: ${dailyStats.pizzasGrandes}")
        // Postres y Extras
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("POSTRES Y EXTRAS")
        sb.appendLine("Postres: ${dailyStats.postres}")
        sb.appendLine("Extras: ${dailyStats.extras}")
        // Transacciones
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("RESUMEN ORDENES")
        sb.appendLine("Ordenes No pagadas: ${dailyStats.ordenesNoPagadas}")
        sb.appendLine("Ordenes Efectivo: ${dailyStats.totalOrdenesEfectivo}")
        sb.appendLine("Ordenes Tarjeta: ${dailyStats.totalOrdenesTarjeta}")
        // Ingresos
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("INGRESOS POR VENTAS")
        sb.appendLine("Pizzas: ${dailyStats.ingresosPizzas}")
        sb.appendLine("Postres: ${dailyStats.ingresosPostres}")
        sb.appendLine("Extras: ${dailyStats.ingresosExtras}")
        sb.appendLine("Envios: ${dailyStats.ingresosEnvios}")
        // Totales
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("TOTALES")
        sb.appendLine("Ingresos manuales: ${dailyStats.ingresosCapturados}")
        sb.appendLine("Gastos manuales: ${dailyStats.egresosCapturados}")
        //TODO: Mover este calculo al viewmodel
        val totalEfectivoCaja = dailyStats.totalOrdenesEfectivo + dailyStats.ingresosCapturados - dailyStats.egresosCapturados - dailyStats.totalDescuentosTOTODO
        sb.appendLine("TOTAL EFECTIVO: ${dailyStats.totalEfectivoCaja}")
        sb.appendLine("TOTAL EN CAJA: ${dailyStats.totalCaja}")
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
