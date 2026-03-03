package com.alos895.simplepos.ui.metrics

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MetricsScreen(
    onBack: () -> Unit,
    viewModel: MetricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = uiState.topByCategory.keys.toList()
    if (selectedCategory == null && categories.isNotEmpty()) selectedCategory = categories.first()
    if (selectedCategory != null && selectedCategory !in categories && categories.isNotEmpty()) {
        selectedCategory = categories.first()
    }

    fun openPicker(current: Long, onPick: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply { 
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onPick(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Análisis de Negocio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                DateRangeHeader(
                    startDate = uiState.startDateMillis,
                    endDate = uiState.endDateMillis,
                    onStartClick = { openPicker(uiState.startDateMillis, viewModel::setStartDate) },
                    onEndClick = { openPicker(uiState.endDateMillis, viewModel::setEndDate) }
                )
            }

            item {
                QuickPeriodSelector(
                    selectedDays = guessDays(uiState.startDateMillis, uiState.endDateMillis),
                    onSelect = viewModel::setQuickRange
                )
            }

            when (uiState.status) {
                MetricsStatus.LOADING -> {
                    item { LoadingState() }
                }
                MetricsStatus.ERROR -> {
                    item { ErrorState(uiState.errorMessage ?: "Ocurrió un error inesperado") }
                }
                MetricsStatus.EMPTY -> {
                    item { EmptyState("No hay datos", "No se encontraron ventas para el periodo seleccionado.") }
                }
                else -> {
                    item {
                        KpiSection(uiState.kpis)
                    }

                    item {
                        SalesTrendSection(uiState.salesTrend)
                    }

                    stickyHeader {
                        SectionDivider("Rendimiento por Categoría")
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
                        if (currentCategory != null) {
                            RankingSection(
                                topItems = uiState.topByCategory[currentCategory].orEmpty(),
                                bottomItems = uiState.bottomByCategory[currentCategory].orEmpty()
                            )
                        }
                    }

                    stickyHeader {
                        SectionDivider("Inventario y Operaciones")
                    }

                    item {
                        InventoryInsightsSection(uiState.inventoryInsights)
                    }

                    if (uiState.status == MetricsStatus.LOW_DATA) {
                        item {
                            WarningBanner("Datos limitados: El análisis se basa en pocas órdenes.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangeHeader(
    startDate: Long,
    endDate: Long,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DateChip(label = "Desde", date = startDate, onClick = onStartClick)
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            DateChip(label = "Hasta", date = endDate, onClick = onEndClick)
        }
    }
}

@Composable
fun DateChip(label: String, date: Long, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(
            date.toUiDate(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun QuickPeriodSelector(selectedDays: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        val options = listOf(7 to "7 días", 14 to "14 días", 30 to "Mes", 90 to "Trimestre")
        items(options) { (days, label) ->
            FilterChip(
                selected = selectedDays == days,
                onClick = { onSelect(days) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun KpiSection(kpis: List<KpiItem>) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        kpis.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { kpi ->
                    KpiCard(kpi, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun KpiCard(item: KpiItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                item.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.secondaryText?.let {
                Spacer(Modifier.height(4.dp))
                val isPositive = it.contains("+")
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SalesTrendSection(rows: List<MetricsDayRow>) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Moving, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Tendencia de Ventas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            
            if (rows.isEmpty()) {
                Box(Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay datos de tendencia", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                SalesChart(rows)
            }
        }
    }
}

@Composable
fun SalesChart(rows: List<MetricsDayRow>) {
    val maxSales = remember(rows) { rows.maxOf { it.netSales }.takeIf { it > 0 } ?: 1.0 }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val spacing = 20f
            val barWidth = (canvasWidth - (spacing * (rows.size + 1))) / rows.size

            rows.forEachIndexed { index, row ->
                val x = spacing + index * (barWidth + spacing)
                val barHeight = (row.netSales / maxSales * canvasHeight).toFloat()
                
                // Draw Bar
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f))),
                    topLeft = Offset(x, canvasHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Orders indicator (small dot)
                val ordersRatio = (row.orders.toDouble() / rows.maxOf { it.orders.coerceAtLeast(1) }.toDouble()).toFloat()
                drawCircle(
                    color = secondaryColor,
                    radius = 4f,
                    center = Offset(x + barWidth / 2, canvasHeight - (ordersRatio * canvasHeight))
                )
            }
        }
        
        if (rows.size <= 10) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                rows.forEachIndexed { index, row ->
                    if (index % (rows.size / 3 + 1) == 0 || index == rows.size - 1) {
                        Text(
                            row.dayMillis.toShortUiDate(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionDivider(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun CategorySelector(categories: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun RankingSection(topItems: List<TopItem>, bottomItems: List<TopItem>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RankingCard(title = "Más Vendidos", items = topItems, icon = Icons.Default.Leaderboard, color = MaterialTheme.colorScheme.primary)
        if (bottomItems.isNotEmpty()) {
            RankingCard(title = "Menos Vendidos", items = bottomItems, icon = Icons.Default.QueryStats, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun RankingCard(title: String, items: List<TopItem>, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            
            val maxQty = items.maxOfOrNull { it.quantity } ?: 1
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${item.quantity} u.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val progress by animateFloatAsState(
                            targetValue = item.quantity.toFloat() / maxQty.toFloat(),
                            animationSpec = tween(1000)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryInsightsSection(data: InventoryInsightsData) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        data.stockAlerts.forEach { alert ->
            val alertColor = when(alert.severity) {
                "alta" -> MaterialTheme.colorScheme.error
                "media" -> Color(0xFFF57C00) 
                else -> MaterialTheme.colorScheme.primary
            }
            Surface(
                color = alertColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, alertColor.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if(alert.severity == "alta") Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(alert.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = alertColor)
                        Text(alert.detail, style = MaterialTheme.typography.bodySmall, color = alertColor.copy(alpha = 0.8f))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightCard(
                title = "Insumos Top",
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f)
            ) {
                if (data.consumptionTop5.isEmpty()) {
                    Text("Sin datos", style = MaterialTheme.typography.bodySmall)
                } else {
                    data.consumptionTop5.take(3).forEach {
                        Text("• ${it.name}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            
            InsightCard(
                title = "Acciones",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            ) {
                data.recommendedActions.take(2).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun InsightCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Calculando métricas...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorState(error: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun WarningBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

private fun Long.toUiDate(): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
private fun Long.toShortUiDate(): String = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(this))

private fun guessDays(start: Long, end: Long): Int {
    val diff = end - start
    val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    return when {
        days in 6..8 -> 7
        days in 13..15 -> 14
        days in 27..32 -> 30
        days in 85..95 -> 90
        else -> 0
    }
}
