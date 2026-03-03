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

data class MetricsDayRow(
    val dayMillis: Long,
    val orders: Int = 0,
    val netSales: Double = 0.0,
    val manualIncome: Double = 0.0,
    val manualExpense: Double = 0.0,
    val basesCreated: Int = 0,
    val basesUsed: Int = 0
)

data class ItemMetric(
    val name: String,
    val units: Int,
    val revenue: Double
)

data class PizzaMixMetric(
    val name: String,
    val units: Int
)

data class AdvancedMetrics(
    val topPizza: ItemMetric? = null,
    val topSize: PizzaMixMetric? = null,
    val topPizzaBySize: PizzaMixMetric? = null,
    val leastPizzaBySize: PizzaMixMetric? = null,
    val topDrink: ItemMetric? = null,
    val topDessert: ItemMetric? = null,
    val topCombo: ItemMetric? = null,
    val topDeliveryService: PizzaMixMetric? = null,
    val bottomDeliveryService: PizzaMixMetric? = null
)

data class MetricsUiState(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val rows: List<MetricsDayRow> = emptyList(),
    val advanced: AdvancedMetrics = AdvancedMetrics(),
    val isLoading: Boolean = false
) {
    val totalSales: Double = rows.sumOf { it.netSales }
    val totalOrders: Int = rows.sumOf { it.orders }
    val avgTicket: Double = if (totalOrders > 0) totalSales / totalOrders else 0.0
}

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)
    private val pizzaBaseDao = AppDatabase.getDatabase(application).pizzaBaseDao()

    private val bebidaNames = MenuData.bebidaOptions
        .map { it.nombre.trim().lowercase(Locale.getDefault()) }
        .toSet()

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

    fun setStartDate(millis: Long) {
        loadRange(millis, _uiState.value.endDateMillis)
    }

    fun setEndDate(millis: Long) {
        loadRange(_uiState.value.startDateMillis, millis)
    }

    private fun loadRange(startMillis: Long, endMillisInclusive: Long) {
        val normalizedStart = startOfDay(minOf(startMillis, endMillisInclusive))
        val normalizedEndInclusive = startOfDay(maxOf(startMillis, endMillisInclusive))
        val normalizedEndExclusive = normalizedEndInclusive + DAY_MS

        _uiState.value = _uiState.value.copy(
            startDateMillis = normalizedStart,
            endDateMillis = normalizedEndInclusive,
            isLoading = true
        )

        viewModelScope.launch {
            val orders = orderRepository.getOrdersByDateRange(normalizedStart, normalizedEndExclusive)
            val txs = transactionsRepository.getTransactionsByDateRange(normalizedStart, normalizedEndExclusive)
            val createdBases = pizzaBaseDao.getPizzaBasesByCreatedAtRange(normalizedStart, normalizedEndExclusive)
            val usedBases = pizzaBaseDao.getPizzaBasesByUsedAtRange(normalizedStart, normalizedEndExclusive)

            val groupedOrders = orders.groupBy { startOfDay(it.timestamp) }
            val groupedTx = txs.groupBy { startOfDay(it.date) }
            val groupedBasesCreated = createdBases.groupBy { startOfDay(it.createdAt) }
            val groupedBasesUsed = usedBases.groupBy { startOfDay(it.usedAt ?: 0L) }

            val days = mutableListOf<Long>()
            var cursor = normalizedStart
            while (cursor < normalizedEndExclusive) {
                days.add(cursor)
                cursor += DAY_MS
            }

            val rows = days.map { day ->
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

            _uiState.value = _uiState.value.copy(
                rows = rows,
                advanced = calculateAdvancedMetrics(orders),
                isLoading = false
            )
        }
    }

    private fun calculateAdvancedMetrics(orders: List<OrderEntity>): AdvancedMetrics {
        val pizzaByFlavor = mutableMapOf<String, Int>()
        val pizzaBySize = mutableMapOf<String, Int>()
        val pizzaBySizeAndFlavor = mutableMapOf<String, Int>()
        val drinks = mutableMapOf<String, ItemMetric>()
        val desserts = mutableMapOf<String, ItemMetric>()
        val combos = mutableMapOf<String, ItemMetric>()
        val deliveryServices = mutableMapOf<String, Int>()

        orders.forEach { order ->
            getCartItems(order).forEach { item ->
                val size = item.sizeLabel().ifBlank { "Sin tamaño" }
                val flavor = item.flavorLabel()
                val units = item.cantidad
                val subtotal = item.subtotal

                pizzaByFlavor[flavor] = (pizzaByFlavor[flavor] ?: 0) + units
                pizzaBySize[size] = (pizzaBySize[size] ?: 0) + units

                val sizeFlavorKey = "$size • $flavor"
                pizzaBySizeAndFlavor[sizeFlavorKey] = (pizzaBySizeAndFlavor[sizeFlavorKey] ?: 0) + units
            }

            getDessertItems(order).forEach { item ->
                val name = item.postreOrExtra.nombre
                val units = item.cantidad
                val subtotal = item.subtotal
                when {
                    item.postreOrExtra.esCombo -> {
                        combos[name] = (combos[name] ?: ItemMetric(name, 0, 0.0)).let {
                            it.copy(units = it.units + units, revenue = it.revenue + subtotal)
                        }
                    }
                    isBebidaItem(item) -> {
                        drinks[name] = (drinks[name] ?: ItemMetric(name, 0, 0.0)).let {
                            it.copy(units = it.units + units, revenue = it.revenue + subtotal)
                        }
                    }
                    item.postreOrExtra.esPostre -> {
                        desserts[name] = (desserts[name] ?: ItemMetric(name, 0, 0.0)).let {
                            it.copy(units = it.units + units, revenue = it.revenue + subtotal)
                        }
                    }
                }
            }

            val deliveryName = when (order.deliveryType) {
                DeliveryType.PASAN -> "Pasan"
                DeliveryType.CAMINANDO -> "Caminando"
                DeliveryType.TOTODO -> "TOTODO"
                DeliveryType.DOMICILIO -> "Domicilio"
            }
            deliveryServices[deliveryName] = (deliveryServices[deliveryName] ?: 0) + 1
        }

        return AdvancedMetrics(
            topPizza = pizzaByFlavor.maxByOrNull { it.value }?.let { ItemMetric(it.key, it.value, 0.0) },
            topSize = pizzaBySize.maxByOrNull { it.value }?.let { PizzaMixMetric(it.key, it.value) },
            topPizzaBySize = pizzaBySizeAndFlavor.maxByOrNull { it.value }?.let { PizzaMixMetric(it.key, it.value) },
            leastPizzaBySize = pizzaBySizeAndFlavor.minByOrNull { it.value }?.let { PizzaMixMetric(it.key, it.value) },
            topDrink = drinks.maxByOrNull { it.value.units }?.value,
            topDessert = desserts.maxByOrNull { it.value.units }?.value,
            topCombo = combos.maxByOrNull { it.value.units }?.value,
            topDeliveryService = deliveryServices.maxByOrNull { it.value }?.let { PizzaMixMetric(it.key, it.value) },
            bottomDeliveryService = deliveryServices.minByOrNull { it.value }?.let { PizzaMixMetric(it.key, it.value) }
        )
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

    private fun CartItem.sizeLabel(): String {
        return sizeName ?: tamano?.nombre ?: ""
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

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
