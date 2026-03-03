package com.alos895.simplepos.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class MetricsDayRow(
    val dayMillis: Long,
    val orders: Int = 0,
    val netSales: Double = 0.0,
    val manualIncome: Double = 0.0,
    val manualExpense: Double = 0.0,
    val basesCreated: Int = 0,
    val basesUsed: Int = 0
)

data class MetricsUiState(
    val startDateMillis: Long,
    val endDateMillis: Long,
    val rows: List<MetricsDayRow> = emptyList(),
    val isLoading: Boolean = false
) {
    val totalSales: Double = rows.sumOf { it.netSales }
    val totalOrders: Int = rows.sumOf { it.orders }
    val avgTicket: Double = if (totalOrders > 0) totalSales / totalOrders else 0.0
}

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)
    private val pizzaBaseDao = AppDatabase.getDatabase(application).pizzaBaseDao()

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

            _uiState.value = _uiState.value.copy(rows = rows, isLoading = false)
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

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
