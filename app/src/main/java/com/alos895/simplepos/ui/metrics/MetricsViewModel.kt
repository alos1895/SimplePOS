package com.alos895.simplepos.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private const val DAY_MS = 86_400_000L

enum class MetricsStatus {
    LOADING,
    CONTENT,
    EMPTY,
    LOW_DATA,
    ERROR
}

data class MetricsDayRow(
    val dayMillis: Long,
    val orders: Int,
    val netSales: Double,
    val manualIncome: Double,
    val manualExpense: Double,
    val basesCreated: Int,
    val basesUsed: Int
)

data class KpiItem(
    val label: String,
    val value: String,
    val secondaryText: String? = null,
    val isCurrency: Boolean = false
)

data class TopItem(
    val name: String,
    val quantity: Int,
    val sales: Double,
    val margin: Double? = null
)

data class InventoryAlert(
    val title: String,
    val detail: String,
    val severity: String
)

data class InventoryInsightsData(
    val consumptionTop5: List<TopItem> = emptyList(),
    val stockAlerts: List<InventoryAlert> = emptyList(),
    val shrinkageNotes: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList()
)

data class MetricsUiState(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val status: MetricsStatus = MetricsStatus.LOADING,
    val errorMessage: String? = null,
    val kpis: List<KpiItem> = emptyList(),
    val salesTrend: List<MetricsDayRow> = emptyList(),
    val topByCategory: Map<String, List<TopItem>> = emptyMap(),
    val bottomByCategory: Map<String, List<TopItem>> = emptyMap(),
    val inventoryInsights: InventoryInsightsData = InventoryInsightsData()
)

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val orderDao = db.orderDao()
    private val orderItemDao = db.orderItemDao()
    private val transactionsRepository = TransactionsRepository(application)
    private val pizzaBaseDao = db.pizzaBaseDao()

    private val today = startOfDay(System.currentTimeMillis())
    private val initialState = MetricsUiState(
        startDateMillis = today - (6 * DAY_MS),
        endDateMillis = today
    )

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    init {
        loadRange(initialState.startDateMillis, initialState.endDateMillis)
    }

    fun setQuickRange(days: Int) {
        val end = today
        val start = end - ((days - 1) * DAY_MS)
        loadRange(start, end)
    }

    fun setStartDate(millis: Long) = loadRange(millis, _uiState.value.endDateMillis)

    fun setEndDate(millis: Long) = loadRange(_uiState.value.startDateMillis, millis)

    private fun loadRange(startMillis: Long, endMillisInclusive: Long) {
        val normalizedStart = startOfDay(minOf(startMillis, endMillisInclusive))
        val normalizedEndInclusive = startOfDay(maxOf(startMillis, endMillisInclusive))
        val normalizedEndExclusive = normalizedEndInclusive + DAY_MS

        _uiState.value = _uiState.value.copy(
            startDateMillis = normalizedStart,
            endDateMillis = normalizedEndInclusive,
            status = MetricsStatus.LOADING,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val orders = orderDao.getOrdersForDateRange(normalizedStart, normalizedEndExclusive)
                val txs = transactionsRepository.getTransactionsByDateRange(normalizedStart, normalizedEndExclusive)
                val createdBases = pizzaBaseDao.getPizzaBasesByCreatedAtRange(normalizedStart, normalizedEndExclusive)
                val usedBases = pizzaBaseDao.getPizzaBasesByUsedAtRange(normalizedStart, normalizedEndExclusive)

                // Nuevas métricas desde la tabla normalizada
                val productStats = orderItemDao.getProductRankingByDateRange(normalizedStart, normalizedEndExclusive)
                
                val trend = buildTrendRows(normalizedStart, normalizedEndExclusive, orders, txs, createdBases, usedBases)

                val previousRange = getPreviousRange(normalizedStart, normalizedEndInclusive)
                val previousOrders = orderDao.getOrdersForDateRange(previousRange.first, previousRange.second)

                val kpis = buildKpis(orders, previousOrders, productStats)
                
                val tops = productStats.groupBy { it.type }
                    .mapValues { (_, list) -> 
                        list.map { TopItem(it.name, it.quantity, it.sales) }
                            .sortedByDescending { it.quantity }
                            .take(15)
                    }

                val bottoms = productStats.groupBy { it.type }
                    .filterValues { it.size >= 5 }
                    .mapValues { (_, list) -> 
                        list.map { TopItem(it.name, it.quantity, it.sales) }
                            .sortedBy { it.quantity }
                            .take(5)
                    }

                val inventory = buildInventoryInsights(productStats, createdBases, usedBases)

                val status = when {
                    orders.isEmpty() -> MetricsStatus.EMPTY
                    orders.size < 3 -> MetricsStatus.LOW_DATA
                    else -> MetricsStatus.CONTENT
                }

                _uiState.value = _uiState.value.copy(
                    status = status,
                    kpis = kpis,
                    salesTrend = trend,
                    topByCategory = tops,
                    bottomByCategory = bottoms,
                    inventoryInsights = inventory,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = MetricsStatus.ERROR,
                    errorMessage = e.message ?: "Error cargando métricas"
                )
            }
        }
    }

    private fun buildTrendRows(
        start: Long,
        endExclusive: Long,
        orders: List<OrderEntity>,
        txs: List<com.alos895.simplepos.db.entity.TransactionEntity>,
        createdBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>,
        usedBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>
    ): List<MetricsDayRow> {
        val groupedOrders = orders.groupBy { startOfDay(it.timestamp) }
        val groupedTx = txs.groupBy { startOfDay(it.date) }
        val groupedBasesCreated = createdBases.groupBy { startOfDay(it.createdAt) }
        val groupedBasesUsed = usedBases.groupBy { startOfDay(it.usedAt ?: 0L) }

        val days = mutableListOf<Long>()
        var cursor = start
        while (cursor < endExclusive) {
            days.add(cursor)
            cursor += DAY_MS
        }

        return days.map { day ->
            val dayOrders = groupedOrders[day].orEmpty()
            val dayTx = groupedTx[day].orEmpty()
            MetricsDayRow(
                dayMillis = day,
                orders = dayOrders.size,
                netSales = dayOrders.sumOf { it.total - it.descuentoTOTODO },
                manualIncome = dayTx.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount },
                manualExpense = dayTx.filter { it.type == TransactionType.GASTO }.sumOf { it.amount },
                basesCreated = groupedBasesCreated[day].orEmpty().size,
                basesUsed = groupedBasesUsed[day].orEmpty().size
            )
        }
    }

    private fun buildKpis(
        currentOrders: List<OrderEntity>, 
        previousOrders: List<OrderEntity>,
        productStats: List<com.alos895.simplepos.db.ProductStats>
    ): List<KpiItem> {
        val totalOrders = currentOrders.size
        val currentSales = currentOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousSales = previousOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousCount = previousOrders.size
        
        val currentTicket = if (totalOrders > 0) currentSales / totalOrders else 0.0
        val previousTicket = if (previousCount > 0) previousSales / previousCount else 0.0

        val totalPizzas = productStats.filter { it.type == "PIZZA" || it.type == "COMBINADA" }.sumOf { it.quantity }
        val pizzasPerOrder = if (totalOrders > 0) totalPizzas.toDouble() / totalOrders else 0.0

        val currentDeliveryRev = currentOrders.sumOf { it.deliveryServicePrice.toDouble() }
        
        return listOf(
            KpiItem("Órdenes", totalOrders.toString(), percentDeltaLabel(totalOrders.toDouble(), previousCount.toDouble())),
            KpiItem("Ventas netas", formatMoney(currentSales), percentDeltaLabel(currentSales, previousSales), true),
            KpiItem("Ticket prom.", formatMoney(currentTicket), percentDeltaLabel(currentTicket, previousTicket), true),
            KpiItem("Pizzas/Ticket", "%.1f".format(pizzasPerOrder)),
            KpiItem("Ingreso Envío", formatMoney(currentDeliveryRev), null, true)
        )
    }

    private fun buildInventoryInsights(
        productStats: List<com.alos895.simplepos.db.ProductStats>,
        createdBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>,
        usedBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>
    ): InventoryInsightsData {
        // En un futuro podrías mapear ingredientes a productos aquí
        val topPizzas = productStats.filter { it.type == "PIZZA" }
            .map { TopItem(it.name, it.quantity, it.sales) }
            .take(5)

        val availableBySize = createdBases.groupBy { it.size.lowercase(Locale.getDefault()) }
            .mapValues { (_, list) -> list.count { it.usedAt == null } }

        val stockAlerts = availableBySize
            .filterValues { it <= 5 }
            .map { (size, available) ->
                InventoryAlert(
                    title = "Stock bajo de base ${size.uppercase()}",
                    detail = "Quedan $available unidades listas.",
                    severity = if (available <= 2) "media" else "baja"
                )
            }

        return InventoryInsightsData(
            consumptionTop5 = topPizzas,
            stockAlerts = stockAlerts,
            recommendedActions = listOf("Operación normal basada en tabla normalizada.")
        )
    }

    private fun getPreviousRange(start: Long, endInclusive: Long): Pair<Long, Long> {
        val rangeDays = ((endInclusive - start) / DAY_MS) + 1
        val prevEndInclusive = start - DAY_MS
        val prevStart = prevEndInclusive - ((rangeDays - 1) * DAY_MS)
        return prevStart to (prevEndInclusive + DAY_MS)
    }

    private fun percentDeltaLabel(current: Double, previous: Double): String? {
        if (previous == 0.0) return null
        val delta = ((current - previous) / previous) * 100.0
        val sign = if (delta >= 0) "+" else ""
        return "$sign${"%.1f".format(delta)}% vs ant."
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun formatMoney(amount: Double): String = "$${"%,.0f".format(amount)}"
}
