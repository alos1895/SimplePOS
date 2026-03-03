package com.alos895.simplepos.ui.metrics

import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository(application)
    private val transactionsRepository = TransactionsRepository(application)
    private val pizzaBaseDao = AppDatabase.getDatabase(application).pizzaBaseDao()

    private val _rows = MutableStateFlow<List<MetricsDayRow>>(emptyList())
    val rows: StateFlow<List<MetricsDayRow>> = _rows.asStateFlow()

    fun loadRange(startMillis: Long, endMillisInclusive: Long) {
        val normalizedStart = startOfDay(minOf(startMillis, endMillisInclusive))
        val normalizedEnd = startOfDay(maxOf(startMillis, endMillisInclusive)) + DAY_MS

        viewModelScope.launch {
            val orders = orderRepository.getOrdersByDateRange(normalizedStart, normalizedEnd)
            val txs = transactionsRepository.getTransactionsByDateRange(normalizedStart, normalizedEnd)
            val createdBases = pizzaBaseDao.getPizzaBasesByCreatedAtRange(normalizedStart, normalizedEnd)
            val usedBases = pizzaBaseDao.getPizzaBasesByUsedAtRange(normalizedStart, normalizedEnd)

            val groupedOrders = orders.groupBy { startOfDay(it.timestamp) }
            val groupedTx = txs.groupBy { startOfDay(it.date) }
            val groupedBasesCreated = createdBases.groupBy { startOfDay(it.createdAt) }
            val groupedBasesUsed = usedBases.groupBy { startOfDay(it.usedAt ?: 0L) }

            val days = mutableListOf<Long>()
            var cursor = normalizedStart
            while (cursor < normalizedEnd) {
                days.add(cursor)
                cursor += DAY_MS
            }

            _rows.value = days.map { day ->
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

@Composable
fun MetricsScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsState()

    val today = remember { Calendar.getInstance().timeInMillis }
    var startDate by remember { mutableStateOf(today - (6 * 86_400_000L)) }
    var endDate by remember { mutableStateOf(today) }

    LaunchedEffect(startDate, endDate) {
        viewModel.loadRange(startDate, endDate)
    }

    fun openStartPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                startDate = picked.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = endDate }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                endDate = picked.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val totalSales = rows.sumOf { it.netSales }
    val totalOrders = rows.sumOf { it.orders }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Regresar") }
        Text("Métricas", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AssistChip(onClick = {
                endDate = today
                startDate = today - (6 * 86_400_000L)
            }, label = { Text("7 días") })
            AssistChip(onClick = {
                endDate = today
                startDate = today - (13 * 86_400_000L)
            }, label = { Text("14 días") })
            AssistChip(onClick = {
                endDate = today
                startDate = today - (29 * 86_400_000L)
            }, label = { Text("30 días") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { openStartPicker() }) { Text("Inicio: ${startDate.toUiDate()}") }
            TextButton(onClick = { openEndPicker() }) { Text("Fin: ${endDate.toUiDate()}") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Resumen intervalo", style = MaterialTheme.typography.titleMedium)
                Text("Órdenes: $totalOrders")
                Text("Ventas netas: ${formatMoney(totalSales)}")
                Text("Ticket promedio: ${formatMoney(if (totalOrders > 0) totalSales / totalOrders else 0.0)}")
            }
        }

        MetricsTable(rows = rows)
    }
}

@Composable
private fun MetricsTable(rows: List<MetricsDayRow>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fecha")
            Text("Ord")
            Text("Ventas")
            Text("Ing")
            Text("Gas")
            Text("Bases")
            Text("Usadas")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows.sortedByDescending { it.dayMillis }) { row ->
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row.dayMillis.toUiDate())
                        Text(row.orders.toString())
                        Text(formatMoney(row.netSales))
                        Text(formatMoney(row.manualIncome))
                        Text(formatMoney(row.manualExpense))
                        Text(row.basesCreated.toString())
                        Text(row.basesUsed.toString())
                    }
                }
            }
        }
    }
}

private fun Long.toUiDate(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))
private fun formatMoney(amount: Double): String = "$${"%,.2f".format(amount)}"
