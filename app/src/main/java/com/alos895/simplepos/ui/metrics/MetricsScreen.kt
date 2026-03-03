package com.alos895.simplepos.ui.metrics

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MetricsScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    fun openStartPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.startDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.setStartDate(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.endDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.setEndDate(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Regresar") }
        Text("Métricas avanzadas", style = MaterialTheme.typography.headlineSmall)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            AssistChip(onClick = { viewModel.setQuickRange(7) }, label = { Text("7 días") })
            AssistChip(onClick = { viewModel.setQuickRange(14) }, label = { Text("14 días") })
            AssistChip(onClick = { viewModel.setQuickRange(30) }, label = { Text("30 días") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { openStartPicker() }) {
                Text("Inicio: ${uiState.startDateMillis.toUiDate()}")
            }
            TextButton(onClick = { openEndPicker() }) {
                Text("Fin: ${uiState.endDateMillis.toUiDate()}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Resumen intervalo", style = MaterialTheme.typography.titleMedium)
                Text("Órdenes: ${uiState.totalOrders}")
                Text("Ventas netas: ${formatMoney(uiState.totalSales)}")
                Text("Ticket promedio: ${formatMoney(uiState.avgTicket)}")
                if (uiState.isLoading) Text("Actualizando métricas...")
            }
        }

        AdvancedMetricsCard(uiState.advanced)
        MetricsTable(rows = uiState.rows)
    }
}

@Composable
private fun AdvancedMetricsCard(metrics: AdvancedMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Top y Bottom de ventas", style = MaterialTheme.typography.titleMedium)
            Text("Pizza más vendida: ${metrics.topPizza.toLabel()}")
            Text("Tamaño más vendido: ${metrics.topSize.toLabel()}")
            Text("Tamaño + sabor más vendido: ${metrics.topPizzaBySize.toLabel()}")
            Text("Tamaño + sabor menos vendido: ${metrics.leastPizzaBySize.toLabel()}")
            Text("Bebida más vendida: ${metrics.topDrink.toLabel()}")
            Text("Postre más vendido: ${metrics.topDessert.toLabel()}")
            Text("Combo más vendido: ${metrics.topCombo.toLabel()}")
            Text("Servicio entrega más usado: ${metrics.topDeliveryService.toLabel()}")
            Text("Servicio entrega menos usado: ${metrics.bottomDeliveryService.toLabel()}")
        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
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

private fun Long.toUiDate(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))

private fun formatMoney(amount: Double): String = "$${"%,.2f".format(amount)}"

private fun ItemMetric?.toLabel(): String =
    this?.let { "${it.name} (${it.units})" } ?: "Sin datos"

private fun PizzaMixMetric?.toLabel(): String =
    this?.let { "${it.name} (${it.units})" } ?: "Sin datos"
