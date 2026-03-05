package com.alos895.simplepos.ui.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.db.entity.PizzaBaseEntity
import com.alos895.simplepos.ui.metrics.MetricsScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AdminOption {
    HOME,
    MENU,
    INVENTORY,
    METRICS
}

@Composable
fun AdminScreen() {
    var selectedOption by remember { mutableStateOf(AdminOption.HOME) }

    when (selectedOption) {
        AdminOption.HOME -> AdminHome(
            onOpenMenuAdmin = { selectedOption = AdminOption.MENU },
            onOpenInventory = { selectedOption = AdminOption.INVENTORY },
            onOpenMetrics = { selectedOption = AdminOption.METRICS }
        )

        AdminOption.MENU -> AdminMenuContainer(onBack = { selectedOption = AdminOption.HOME })
        AdminOption.INVENTORY -> InventoryScreen(onBack = { selectedOption = AdminOption.HOME })
        AdminOption.METRICS -> MetricsScreen(onBack = { selectedOption = AdminOption.HOME })
    }
}

@Composable
private fun AdminHome(
    onOpenMenuAdmin: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenMetrics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Panel de administración",
            style = MaterialTheme.typography.titleLarge
        )

        AdminOptionCard(
            title = "Administración de menú",
            subtitle = "Abrir la vista actual para editar pizzas, ingredientes y extras.",
            onClick = onOpenMenuAdmin
        )

        AdminOptionCard(
            title = "Inventario",
            subtitle = "Registrar bases de pizza y controlar su uso.",
            onClick = onOpenInventory
        )

        AdminOptionCard(
            title = "Métricas",
            subtitle = "Analizar ventas e inventario por intervalos y días.",
            onClick = onOpenMetrics
        )
    }
}

@Composable
private fun AdminOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdminMenuContainer(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        BackButton(onBack = onBack)
        AdminMenuScreen()
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Regresar")
        Text(text = "Regresar")
    }
}

@Composable
private fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: AdminInventoryViewModel = viewModel()
) {
    val bases by viewModel.pizzaBases.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDateMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminInventoryEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AdminInventoryEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val dailyBases = remember(bases, selectedDateMillis) {
        bases.filter { it.createdAt.isSameDayAs(selectedDateMillis) }
    }
    val isDateLocked = dailyBases.isNotEmpty()
    val smallCount = dailyBases.count { it.size.equals("chica", ignoreCase = true) }
    val mediumCount = dailyBases.count { it.size.equals("mediana", ignoreCase = true) }
    val largeCount = dailyBases.count {
        it.size.equals("extra grande", ignoreCase = true) || it.size.equals("grande", ignoreCase = true)
    }

    val nowMillis = remember { Calendar.getInstance().timeInMillis }
    val usedToday = bases.count { used ->
        used.usedAt?.isSameDayAs(nowMillis) == true
    }

    val totalSmallCount = bases.count { it.size.equals("chica", ignoreCase = true) }
    val totalMediumCount = bases.count { it.size.equals("mediana", ignoreCase = true) }
    val totalLargeCount = bases.count {
        it.size.equals("extra grande", ignoreCase = true) || it.size.equals("grande", ignoreCase = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton(onBack = onBack)
            Text(
                text = "Inventario de bases",
                style = MaterialTheme.typography.titleLarge
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Captura") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Registros del día") })
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AddPizzaBasesForm(
                            selectedDateMillis = selectedDateMillis,
                            currentSmallCount = smallCount,
                            currentMediumCount = mediumCount,
                            currentLargeCount = largeCount,
                            onDateChange = { selectedDateMillis = it },
                            onSave = viewModel::replacePizzaBasesForDate,
                            isLocked = isDateLocked,
                            modifier = Modifier.weight(1f)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DailyTotalsCard(
                                selectedDateMillis = selectedDateMillis,
                                smallCount = smallCount,
                                mediumCount = mediumCount,
                                largeCount = largeCount,
                                usedToday = usedToday,
                                modifier = Modifier.fillMaxWidth()
                            )

                            TotalTotalsCard(
                                totalSmallCount = totalSmallCount,
                                totalMediumCount = totalMediumCount,
                                totalLargeCount = totalLargeCount,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    RemoveBrokenBaseCard(
                        onRemoveSmall = { viewModel.discardOneUnusedBase("chica") },
                        onRemoveMedium = { viewModel.discardOneUnusedBase("mediana") },
                        onRemoveLarge = { viewModel.discardOneUnusedBase("grande") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                if (dailyBases.isEmpty()) {
                    Text(
                        text = "No hay registros para ${selectedDateMillis.toUiDayDate()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dailyBases, key = { it.id }) { base ->
                            PizzaBaseItem(
                                base = base,
                                onMarkUsed = { viewModel.markAsUsed(base.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPizzaBasesForm(
    selectedDateMillis: Long,
    currentSmallCount: Int,
    currentMediumCount: Int,
    currentLargeCount: Int,
    onDateChange: (Long) -> Unit,
    onSave: (Long, Int, Int, Int) -> Unit,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    var smallInput by remember(selectedDateMillis) { mutableStateOf(currentSmallCount.toString()) }
    var mediumInput by remember(selectedDateMillis) { mutableStateOf(currentMediumCount.toString()) }
    var largeInput by remember(selectedDateMillis) { mutableStateOf(currentLargeCount.toString()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDateMillis, currentSmallCount, currentMediumCount, currentLargeCount) {
        smallInput = currentSmallCount.toString()
        mediumInput = currentMediumCount.toString()
        largeInput = currentLargeCount.toString()
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DatePickerSelector(
                selectedDateMillis = selectedDateMillis,
                onDateSelected = onDateChange
            )

            OutlinedTextField(
                value = smallInput,
                onValueChange = { smallInput = it.filter(Char::isDigit) },
                label = { Text("Cantidad bases chicas") },
                enabled = !isLocked,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mediumInput,
                onValueChange = { mediumInput = it.filter(Char::isDigit) },
                label = { Text("Cantidad bases medianas") },
                enabled = !isLocked,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = largeInput,
                onValueChange = { largeInput = it.filter(Char::isDigit) },
                label = { Text("Cantidad bases grandes") },
                enabled = !isLocked,
                modifier = Modifier.fillMaxWidth()
            )

            if (isLocked) {
                Text(
                    text = "Las bases de este día ya fueron capturadas y no se pueden modificar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isLocked
            ) {
                Text("Guardar cambios")
            }
        }
    }

    if (showConfirmDialog) {
        val small = smallInput.toIntOrNull() ?: 0
        val medium = mediumInput.toIntOrNull() ?: 0
        val large = largeInput.toIntOrNull() ?: 0

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar captura") },
            text = {
                Text(
                    "Las bases quedarán así para este día: $small chicas, $medium medianas y $large grandes. " +
                        "Una vez capturadas no se podrán modificar. ¿Deseas confirmar?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(selectedDateMillis, small, medium, large)
                    showConfirmDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}


@Composable
private fun RemoveBrokenBaseCard(
    onRemoveSmall: () -> Unit,
    onRemoveMedium: () -> Unit,
    onRemoveLarge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Descartar base dañada (no usada)",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRemoveSmall) { Text("-1 Chica") }
                Button(onClick = onRemoveMedium) { Text("-1 Mediana") }
                Button(onClick = onRemoveLarge) { Text("-1 Grande") }
            }
        }
    }
}

@Composable
private fun DatePickerSelector(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    val dialog = remember(selectedDateMillis) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(picked.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Día de carga",
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dialog.show() }) {
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Seleccionar fecha")
                Text(text = "  ${selectedDateMillis.toUiDayDate()}")
            }
            Button(onClick = {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onDateSelected(today)
            }) {
                Text("Hoy")
            }
        }
    }
}

@Composable
private fun DailyTotalsCard(
    selectedDateMillis: Long,
    smallCount: Int,
    mediumCount: Int,
    largeCount: Int,
    usedToday: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen del día ${selectedDateMillis.toUiDayDate()}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Chicas: $smallCount")
            Text(text = "Medianas: $mediumCount")
            Text(text = "Extra grandes: $largeCount")
            Text(text = "Usadas hoy: $usedToday")
        }
    }
}

@Composable
private fun TotalTotalsCard(
    totalSmallCount: Int,
    totalMediumCount: Int,
    totalLargeCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen total",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Chicas: $totalSmallCount")
            Text(text = "Medianas: $totalMediumCount")
            Text(text = "Extra grandes: $totalLargeCount")
        }
    }
}

@Composable
private fun PizzaBaseItem(
    base: PizzaBaseEntity,
    onMarkUsed: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Base ${base.size.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Creación: ${base.createdAt.toUiDate()}" )
            Text(text = "Uso: ${base.usedAt?.toUiDate() ?: "Pendiente"}")

            if (base.usedAt == null) {
                Row {
                    Button(onClick = onMarkUsed) {
                        Text("Marcar como usada")
                    }
                }
            }
        }
    }
}

private fun Long.toUiDate(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

private fun Long.toUiDayDate(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(this))
}

private fun Long.isSameDayAs(other: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = this@isSameDayAs }
    val c2 = Calendar.getInstance().apply { timeInMillis = other }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
