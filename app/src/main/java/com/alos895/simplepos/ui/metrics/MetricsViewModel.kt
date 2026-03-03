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
import com.alos895.simplepos.model.sizeLabel
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
                val tops = categoryAll.mapValues { it.value.sortedByDescending { item -> item.quantity }.take(15) }
                    .filterValues { it.isNotEmpty() }
                val bottoms = categoryAll
                    .filterValues { it.size >= 5 }
                    .mapValues { (_, list) -> list.sortedBy { it.quantity }.take(5) }
                val inventory = buildInventoryInsights(orders, createdBases, usedBases)

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

    private fun buildKpis(currentOrders: List<OrderEntity>, previousOrders: List<OrderEntity>): List<KpiItem> {
        val totalOrders = currentOrders.size
        val currentSales = currentOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousSales = previousOrders.sumOf { it.total - it.descuentoTOTODO }
        val previousCount = previousOrders.size
        
        val currentTicket = if (totalOrders > 0) currentSales / totalOrders else 0.0
        val previousTicket = if (previousCount > 0) previousSales / previousCount else 0.0

        val currentPizzas = currentOrders.sumOf { order -> getCartItems(order).sumOf { it.cantidad } }
        val previousPizzas = previousOrders.sumOf { order -> getCartItems(order).sumOf { it.cantidad } }
        val currentPizzasPerOrder = if (totalOrders > 0) currentPizzas.toDouble() / totalOrders else 0.0
        val previousPizzasPerOrder = if (previousCount > 0) previousPizzas.toDouble() / previousCount else 0.0

        val currentDeliveryRev = currentOrders.sumOf { it.deliveryServicePrice.toDouble() }
        val previousDeliveryRev = previousOrders.sumOf { it.deliveryServicePrice.toDouble() }
        
        val currentDeliveryCount = currentOrders.count { it.deliveryType == DeliveryType.DOMICILIO }
        val previousDeliveryCount = previousOrders.count { it.deliveryType == DeliveryType.DOMICILIO }
        
        val currentAvgDelivery = if (currentDeliveryCount > 0) currentDeliveryRev / currentDeliveryCount else 0.0
        val previousAvgDelivery = if (previousDeliveryCount > 0) previousDeliveryRev / previousDeliveryCount else 0.0

        return listOf(
            KpiItem("Órdenes", totalOrders.toString(), percentDeltaLabel(totalOrders.toDouble(), previousCount.toDouble())),
            KpiItem("Ventas netas", formatMoney(currentSales), percentDeltaLabel(currentSales, previousSales), true),
            KpiItem("Ticket prom.", formatMoney(currentTicket), percentDeltaLabel(currentTicket, previousTicket), true),
            KpiItem("Pizzas/Ticket", "%.1f".format(currentPizzasPerOrder), percentDeltaLabel(currentPizzasPerOrder, previousPizzasPerOrder)),
            KpiItem("Envío prom.", formatMoney(currentAvgDelivery), percentDeltaLabel(currentAvgDelivery, previousAvgDelivery), true),
            KpiItem("Ingreso Envío", formatMoney(currentDeliveryRev), percentDeltaLabel(currentDeliveryRev, previousDeliveryRev), true)
        )
    }

    private fun buildCategoryMetrics(orders: List<OrderEntity>): Map<String, List<TopItem>> {
        val flavorMap = mutableMapOf<String, TopItem>()
        val sizeMap = mutableMapOf<String, TopItem>()
        val flavorSizeMap = mutableMapOf<String, TopItem>()
        val combinedMap = mutableMapOf<String, TopItem>()
        
        val extrasMap = mutableMapOf<String, TopItem>()
        val comboPromoMap = mutableMapOf<String, TopItem>()
        val postreMap = mutableMapOf<String, TopItem>()
        val bebidaMap = mutableMapOf<String, TopItem>()
        val deliveryTypeMap = mutableMapOf<String, TopItem>()

        orders.forEach { order ->
            getCartItems(order).forEach { item ->
                val flavor = item.flavorLabel()
                val size = item.sizeLabel
                val flavorSize = "$flavor ($size)"
                
                if (item.portions.size > 1) {
                    combinedMap[flavor] = (combinedMap[flavor] ?: TopItem(flavor, 0, 0.0)).let {
                        it.copy(quantity = it.quantity + item.cantidad, sales = it.sales + item.subtotal)
                    }
                } else {
                    flavorMap[flavor] = (flavorMap[flavor] ?: TopItem(flavor, 0, 0.0)).let {
                        it.copy(quantity = it.quantity + item.cantidad, sales = it.sales + item.subtotal)
                    }
                }
                
                sizeMap[size] = (sizeMap[size] ?: TopItem(size, 0, 0.0)).let {
                    it.copy(quantity = it.quantity + item.cantidad, sales = it.sales + item.subtotal)
                }

                flavorSizeMap[flavorSize] = (flavorSizeMap[flavorSize] ?: TopItem(flavorSize, 0, 0.0)).let {
                    it.copy(quantity = it.quantity + item.cantidad, sales = it.sales + item.subtotal)
                }
            }

            getDessertItems(order).forEach { item ->
                val name = item.postreOrExtra.nombre
                val targetMap = when {
                    item.postreOrExtra.esCombo -> comboPromoMap
                    isBebidaItem(item) -> bebidaMap
                    item.postreOrExtra.esPostre -> postreMap
                    else -> extrasMap
                }
                targetMap[name] = (targetMap[name] ?: TopItem(name, 0, 0.0)).let {
                    it.copy(quantity = it.quantity + item.cantidad, sales = it.sales + item.subtotal)
                }
            }

            val dType = order.deliveryType.name
            deliveryTypeMap[dType] = (deliveryTypeMap[dType] ?: TopItem(dType, 0, 0.0)).let {
                it.copy(quantity = it.quantity + 1, sales = it.sales + order.deliveryServicePrice)
            }
        }

        return linkedMapOf(
            "Pizzas" to flavorMap.values.toList(),
            "Combinadas" to combinedMap.values.toList(),
            "Tamaños" to sizeMap.values.toList(),
            "Sabor y Tamaño" to flavorSizeMap.values.toList(),
            "Bebidas" to bebidaMap.values.toList(),
            "Postres" to postreMap.values.toList(),
            "Combos" to comboPromoMap.values.toList(),
            "Extras" to extrasMap.values.toList(),
            "Logística" to deliveryTypeMap.values.toList()
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
            .filterValues { it <= 5 }
            .map { (size, available) ->
                val severity = when {
                    available == 0 -> "alta"
                    available <= 2 -> "media"
                    else -> "baja"
                }
                InventoryAlert(
                    title = "Stock bajo de base ${size.uppercase()}",
                    detail = "Quedan $available unidades listas.",
                    severity = severity
                )
            }
            .ifEmpty {
                listOf(InventoryAlert("Stock óptimo", "Todas las bases tienen inventario suficiente.", "baja"))
            }

        val unusedOldBases = createdBases.count {
            it.usedAt == null && abs(today - startOfDay(it.createdAt)) > DAY_MS
        }

        val shrinkage = if (unusedOldBases > 0) {
            listOf("$unusedOldBases bases preparadas ayer o antes aún no se han usado.")
        } else {
            listOf("Sin merma aparente en bases.")
        }

        val actions = mutableListOf<String>()
        if (stockAlerts.any { it.severity == "alta" || it.severity == "media" }) {
            actions += "Producir más bases de tamaño ${stockAlerts.filter { it.severity != "baja" }.joinToString { it.title.split(" ").last() }}."
        }
        if (topConsumption.isNotEmpty()) {
            actions += "Revisar stock de: ${topConsumption.take(2).joinToString { it.name }}."
        }
        if (unusedOldBases > 5) {
            actions += "Ajustar producción para reducir merma de bases."
        }
        if (actions.isEmpty()) actions += "Operación estable."

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
        if (previous == 0.0) return if (current > 0) "+100% vs ant." else null
        val delta = ((current - previous) / previous) * 100.0
        val sign = if (delta >= 0) "+" else ""
        return "$sign${"%.1f".format(delta)}% vs ant."
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
            pizza?.nombre ?: "Pizza"
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

    private fun formatMoney(amount: Double): String = "$${"%,.0f".format(amount)}"
}
