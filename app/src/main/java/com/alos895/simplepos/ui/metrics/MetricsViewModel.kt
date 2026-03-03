package com.alos895.simplepos.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.TransactionType
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.DeliveryType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    val secondaryText: String? = null
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
    private val gson = Gson()
    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)
    private val pizzaBaseDao = AppDatabase.getDatabase(application).pizzaBaseDao()

    private val bebidaNames = MenuData.bebidaOptions
        .map { it.nombre.trim().lowercase(Locale.getDefault()) }
        .toSet()

    private val ingredientNameById = MenuData.ingredientes.associate { it.id to it.nombre }

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
                val orders = orderRepository.getOrdersByDateRange(normalizedStart, normalizedEndExclusive)
                val txs = transactionsRepository.getTransactionsByDateRange(normalizedStart, normalizedEndExclusive)
                val createdBases = pizzaBaseDao.getPizzaBasesByCreatedAtRange(normalizedStart, normalizedEndExclusive)
                val usedBases = pizzaBaseDao.getPizzaBasesByUsedAtRange(normalizedStart, normalizedEndExclusive)

                val trend = buildTrendRows(normalizedStart, normalizedEndExclusive, orders, txs, createdBases, usedBases)

                val previousRange = getPreviousRange(normalizedStart, normalizedEndInclusive)
                val previousOrders = orderRepository.getOrdersByDateRange(previousRange.first, previousRange.second)

                val kpis = buildKpis(orders, previousOrders)
                val categoryAll = buildCategoryMetrics(orders)
                val tops = categoryAll.mapValues { it.value.sortedByDescending { item -> item.quantity }.take(5) }
                    .filterValues { it.isNotEmpty() }
                val bottoms = categoryAll
                    .filterValues { it.size >= 8 }
                    .mapValues { (_, list) -> list.sortedBy { it.quantity }.take(5) }
                val inventory = buildInventoryInsights(orders, createdBases, usedBases)

                val status = when {
                    orders.isEmpty() -> MetricsStatus.EMPTY
                    orders.size < 5 -> MetricsStatus.LOW_DATA
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

    private fun buildKpis(currentOrders: List<OrderEntity>, previousOrders: List<OrderEntity>): List<KpiItem> {
        val totalOrders = currentOrders.size
        val currentSales = currentOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousSales = previousOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousCount = previousOrders.size
        val currentTicket = if (totalOrders > 0) currentSales / totalOrders else 0.0
        val previousTicket = if (previousCount > 0) previousSales / previousCount else 0.0

        return listOf(
            KpiItem("Órdenes", totalOrders.toString(), percentDeltaLabel(totalOrders.toDouble(), previousCount.toDouble())),
            KpiItem("Ventas netas", formatMoney(currentSales), percentDeltaLabel(currentSales, previousSales)),
            KpiItem("Ticket promedio", formatMoney(currentTicket), percentDeltaLabel(currentTicket, previousTicket))
        )
    }

    private fun buildCategoryMetrics(orders: List<OrderEntity>): Map<String, List<TopItem>> {
        val pizzaMap = mutableMapOf<String, TopItem>()
        val extrasMap = mutableMapOf<String, TopItem>()
        val comboMap = mutableMapOf<String, TopItem>()
        val postreMap = mutableMapOf<String, TopItem>()
        val bebidaMap = mutableMapOf<String, TopItem>()
        val deliveryMap = mutableMapOf<String, TopItem>()

        orders.forEach { order ->
            getCartItems(order).forEach { item ->
                val name = item.flavorLabel()
                val current = pizzaMap[name] ?: TopItem(name, 0, 0.0)
                pizzaMap[name] = current.copy(
                    quantity = current.quantity + item.cantidad,
                    sales = current.sales + item.subtotal
                )
            }

            getDessertItems(order).forEach { item ->
                val name = item.postreOrExtra.nombre
                val currentMap = when {
                    item.postreOrExtra.esCombo -> comboMap
                    isBebidaItem(item) -> bebidaMap
                    item.postreOrExtra.esPostre -> postreMap
                    else -> extrasMap
                }
                val current = currentMap[name] ?: TopItem(name, 0, 0.0)
                currentMap[name] = current.copy(
                    quantity = current.quantity + item.cantidad,
                    sales = current.sales + item.subtotal
                )
            }

            val delivery = when (order.deliveryType) {
                DeliveryType.PASAN -> "Pasan"
                DeliveryType.CAMINANDO -> "Caminando"
                DeliveryType.TOTODO -> "TOTODO"
                DeliveryType.DOMICILIO -> "Domicilio"
            }
            val currentDelivery = deliveryMap[delivery] ?: TopItem(delivery, 0, 0.0)
            deliveryMap[delivery] = currentDelivery.copy(quantity = currentDelivery.quantity + 1, sales = 0.0)
        }

        return linkedMapOf(
            "Pizzas" to pizzaMap.values.toList(),
            "Extras" to extrasMap.values.toList(),
            "Combos" to comboMap.values.toList(),
            "Postres" to postreMap.values.toList(),
            "Bebidas" to bebidaMap.values.toList(),
            "Entrega" to deliveryMap.values.toList()
        ).filterValues { it.isNotEmpty() }
    }

    private fun buildInventoryInsights(
        orders: List<OrderEntity>,
        createdBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>,
        usedBases: List<com.alos895.simplepos.db.entity.PizzaBaseEntity>
    ): InventoryInsightsData {
        val ingredientUsage = mutableMapOf<String, TopItem>()

        orders.forEach { order ->
            getCartItems(order).forEach { item ->
                item.pizza?.ingredientesBaseIds?.forEach { ingredientId ->
                    val name = ingredientNameById[ingredientId] ?: "Ingrediente $ingredientId"
                    val current = ingredientUsage[name] ?: TopItem(name, 0, 0.0)
                    ingredientUsage[name] = current.copy(quantity = current.quantity + item.cantidad)
                }
            }
        }

        val topConsumption = ingredientUsage.values
            .sortedByDescending { it.quantity }
            .take(5)

        val availableBySize = createdBases.groupBy { it.size.lowercase(Locale.getDefault()) }
            .mapValues { (_, list) -> list.count { it.usedAt == null } }

        val stockAlerts = availableBySize
            .filterValues { it <= 3 }
            .map { (size, available) ->
                val severity = if (available == 0) "alta" else "media"
                InventoryAlert(
                    title = "Stock bajo de base $size",
                    detail = "Disponibles: $available",
                    severity = severity
                )
            }
            .ifEmpty {
                listOf(InventoryAlert("Stock estable", "No hay alertas críticas", "baja"))
            }

        val unusedOldBases = createdBases.count {
            it.usedAt == null && abs(today - startOfDay(it.createdAt)) > DAY_MS
        }

        val shrinkage = if (unusedOldBases > 0) {
            listOf("$unusedOldBases bases sin usar de días anteriores (posible merma).")
        } else {
            listOf("Sin señales claras de merma en bases para este periodo.")
        }

        val actions = mutableListOf<String>()
        if (stockAlerts.any { it.severity == "alta" || it.severity == "media" }) {
            actions += "Reordenar producción de bases por tamaño con menor disponibilidad."
        }
        if (topConsumption.isNotEmpty()) {
            actions += "Aumentar compra de: ${topConsumption.take(3).joinToString { it.name }}."
        }
        if (unusedOldBases > 0) {
            actions += "Revisar planeación diaria para reducir merma de bases no usadas."
        }
        if (actions.isEmpty()) {
            actions += "Mantener ritmo actual y volver a revisar métricas en 48h."
        }

        return InventoryInsightsData(
            consumptionTop5 = topConsumption,
            stockAlerts = stockAlerts,
            shrinkageNotes = shrinkage,
            recommendedActions = actions
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
        return "$sign${"%.1f".format(delta)}% vs periodo anterior"
    }

    private fun getCartItems(order: OrderEntity): List<CartItem> = try {
        gson.fromJson(order.itemsJson, object : TypeToken<List<CartItem>>() {}.type)
    } catch (_: Exception) {
        emptyList()
    }

    private fun getDessertItems(order: OrderEntity): List<CartItemPostre> = try {
        gson.fromJson(order.dessertsJson, object : TypeToken<List<CartItemPostre>>() {}.type)
    } catch (_: Exception) {
        emptyList()
    }

    private fun isBebidaItem(item: CartItemPostre): Boolean {
        if (item.postreOrExtra.esBebida) return true
        val name = item.postreOrExtra.nombre.trim().lowercase(Locale.getDefault())
        return name in bebidaNames
    }

    private fun CartItem.flavorLabel(): String {
        return if (portions.isNotEmpty()) {
            portions.joinToString(" + ") { "${it.fraction.label} ${it.pizzaName}" }
        } else {
            pizza?.nombre ?: "Pizza desconocida"
        }
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

    private fun formatMoney(amount: Double): String = "$${"%,.2f".format(amount)}"
}
