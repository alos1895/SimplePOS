package com.alos895.simplepos.ui.metrics

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetricsScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = uiState.topByCategory.keys.toList()
    if (selectedCategory == null && categories.isNotEmpty()) selectedCategory = categories.first()
    if (selectedCategory !in categories) selectedCategory = categories.firstOrNull()

    fun openStartPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.startDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
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
                val picked = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                viewModel.setEndDate(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MetricsHeader(
                onBack = onBack,
                startDateMillis = uiState.startDateMillis,
                endDateMillis = uiState.endDateMillis,
                onPickStartDate = ::openStartPicker,
                onPickEndDate = ::openEndPicker
            )
        }

        item {
            PeriodChips(onRangeSelected = viewModel::setQuickRange)
        }

        when (uiState.status) {
            MetricsStatus.LOADING -> item { EmptyState("Cargando métricas...") }
            MetricsStatus.ERROR -> item { EmptyState(uiState.errorMessage ?: "Error cargando métricas") }
            MetricsStatus.EMPTY -> item { EmptyState("Sin ventas en el rango seleccionado") }
            else -> {
                item {
                    KpiGrid(items = uiState.kpis)
                }

                item {
                    SalesTrendChart(rows = uiState.salesTrend)
                }

                stickyHeader {
                    SectionHeader("Top y Bottom")
                }

                item {
                    if (uiState.status == MetricsStatus.LOW_DATA) {
                        EmptyState("Pocos datos en el rango. Los rankings pueden no ser representativos.")
                    }
                }

                item {
                    CategorySelector(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }

                item {
                    val currentCategory = selectedCategory
                    if (currentCategory == null) {
                        EmptyState("No hay categorías con datos")
                    } else {
                        val topItems = uiState.topByCategory[currentCategory].orEmpty()
                        val bottomItems = uiState.bottomByCategory[currentCategory].orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TopList(title = "Top 5 • $currentCategory", items = topItems)
                            if (bottomItems.isNotEmpty()) {
                                TopList(title = "Bottom 5 • $currentCategory", items = bottomItems)
                            }
                        }
                    }
                }

                stickyHeader {
                    SectionHeader("Inventario")
                }

                item {
                    InventoryInsights(data = uiState.inventoryInsights)
                }
            }
        }
    }
}

@Composable
fun MetricsHeader(
    onBack: () -> Unit,
    startDateMillis: Long,
    endDateMillis: Long,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("← Regresar") }
        Text("Métricas", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${startDateMillis.toUiDate()} - ${endDateMillis.toUiDate()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPickStartDate) { Text("Inicio") }
            TextButton(onClick = onPickEndDate) { Text("Fin") }
        }
    }
}

@Composable
fun PeriodChips(onRangeSelected: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        AssistChip(onClick = { onRangeSelected(7) }, label = { Text("7 días") })
        AssistChip(onClick = { onRangeSelected(14) }, label = { Text("14 días") })
        AssistChip(onClick = { onRangeSelected(30) }, label = { Text("30 días") })
    }
}

@Composable
fun KpiGrid(items: List<KpiItem>) {
    BoxWithConstraints {
        val columns = if (maxWidth > 700.dp) 3 else 2
        val rows = items.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { item ->
                        KpiCard(item = item, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(item: KpiItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            item.secondaryText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SalesTrendChart(rows: List<MetricsDayRow>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tendencia de ventas por día", style = MaterialTheme.typography.titleMedium)
            if (rows.isEmpty()) {
                EmptyState("No hay datos para graficar")
            } else {
                val maxValue = remember(rows) { rows.maxOf { it.netSales }.takeIf { it > 0.0 } ?: 1.0 }
                val barColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val barWidth = size.width / (rows.size * 1.5f)
                    rows.forEachIndexed { index, row ->
                        val ratio = (row.netSales / maxValue).toFloat()
                        val left = index * (barWidth * 1.5f)
                        drawLine(
                            color = barColor,
                            start = androidx.compose.ui.geometry.Offset(left + barWidth, size.height),
                            end = androidx.compose.ui.geometry.Offset(left + barWidth, size.height - (size.height * ratio)),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun CategorySelector(categories: List<String>, selected: String?, onSelect: (String) -> Unit) {
    if (categories.isEmpty()) {
        EmptyState("Sin categorías con datos")
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        categories.forEach { category ->
            AssistChip(
                onClick = { onSelect(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun TopList(title: String, items: List<TopItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                EmptyState("Sin datos para esta categoría")
            } else {
                items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${item.name}")
                        Text("${item.quantity} • ${formatMoney(item.sales)}")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InventoryInsights(data: InventoryInsightsData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Consumo de insumos", style = MaterialTheme.typography.titleSmall)
                    if (data.consumptionTop5.isEmpty()) {
                        Text("Sin datos")
                    } else {
                        data.consumptionTop5.forEach { Text("• ${it.name}: ${it.quantity}") }
                    }
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Alertas de stock", style = MaterialTheme.typography.titleSmall)
                    data.stockAlerts.forEach { Text("• ${it.title}: ${it.detail}") }
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Merma / Ajustes", style = MaterialTheme.typography.titleSmall)
                    data.shrinkageNotes.forEach { Text("• $it") }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Acciones recomendadas", style = MaterialTheme.typography.titleMedium)
                data.recommendedActions.forEach { Text("• $it") }
            }
        }
    }
}

private fun Long.toUiDate(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))

private fun formatMoney(amount: Double): String = "$${"%,.2f".format(amount)}"
