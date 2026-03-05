package com.alos895.simplepos.ui.admin

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.db.entity.PizzaBaseEntity
import com.alos895.simplepos.ui.metrics.MetricsScreen
import java.text.SimpleDateFormat
import java.util.*

private enum class AdminOption {
    HOME, MENU, INVENTORY, METRICS
}

@Composable
fun AdminScreen() {
    var selectedOption by remember { mutableStateOf(AdminOption.HOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminHome(
    onOpenMenuAdmin: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenMetrics: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminOptionCard(
                title = "Menú y Productos",
                subtitle = "Gestiona pizzas, ingredientes y extras.",
                icon = Icons.Default.RestaurantMenu,
                color = MaterialTheme.colorScheme.primary,
                onClick = onOpenMenuAdmin
            )
            AdminOptionCard(
                title = "Control de Inventario",
                subtitle = "Registro y seguimiento de bases de pizza.",
                icon = Icons.Default.Inventory,
                color = Color(0xFFE65100),
                onClick = onOpenInventory
            )
            AdminOptionCard(
                title = "Métricas y Análisis",
                subtitle = "Reportes de ventas y rendimiento.",
                icon = Icons.Default.BarChart,
                color = Color(0xFF2E7D32),
                onClick = onOpenMetrics
            )
        }
    }
}

@Composable
private fun AdminOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    
    val availableSmall = bases.count { it.usedAt == null && it.size.equals("chica", ignoreCase = true) }
    val availableMedium = bases.count { it.usedAt == null && it.size.equals("mediana", ignoreCase = true) }
    val availableLarge = bases.count { 
        it.usedAt == null && (it.size.equals("extra grande", ignoreCase = true) || it.size.equals("grande", ignoreCase = true))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Inventario de Bases", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            StockOverviewRow(availableSmall, availableMedium, availableLarge)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Cargar Bases") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Registros") })
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DatePickerSelector(selectedDateMillis) { selectedDateMillis = it }
                    
                    AddPizzaBasesForm(
                        selectedDateMillis = selectedDateMillis,
                        currentBases = dailyBases,
                        onSave = viewModel::replacePizzaBasesForDate
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    RemoveBrokenBaseCard(
                        onRemoveSmall = { viewModel.discardOneUnusedBase("chica") },
                        onRemoveMedium = { viewModel.discardOneUnusedBase("mediana") },
                        onRemoveLarge = { viewModel.discardOneUnusedBase("grande") }
                    )
                }
            } else {
                if (dailyBases.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay registros para este día", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Producción del ${selectedDateMillis.toUiDayDate()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(dailyBases, key = { it.id }) { base ->
                            PizzaBaseItem(base) { viewModel.markAsUsed(base.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockOverviewRow(small: Int, medium: Int, large: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StockItem("CHICA", small)
            StockItem("MEDIANA", medium)
            StockItem("GRANDE", large)
        }
    }
}

@Composable
private fun StockItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AddPizzaBasesForm(
    selectedDateMillis: Long,
    currentBases: List<PizzaBaseEntity>,
    onSave: (Long, Int, Int, Int) -> Unit
) {
    val isLocked = currentBases.isNotEmpty()
    val initialSmall = currentBases.count { it.size.equals("chica", ignoreCase = true) }
    val initialMedium = currentBases.count { it.size.equals("mediana", ignoreCase = true) }
    val initialLarge = currentBases.count { 
        it.size.equals("extra grande", ignoreCase = true) || it.size.equals("grande", ignoreCase = true) 
    }

    var smallInput by remember(selectedDateMillis) { mutableStateOf(initialSmall.toString()) }
    var mediumInput by remember(selectedDateMillis) { mutableStateOf(initialMedium.toString()) }
    var largeInput by remember(selectedDateMillis) { mutableStateOf(initialLarge.toString()) }
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Captura de Producción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InventoryField("Chica", smallInput, !isLocked, Modifier.weight(1f)) { smallInput = it }
                InventoryField("Mediana", mediumInput, !isLocked, Modifier.weight(1f)) { mediumInput = it }
                InventoryField("Grande", largeInput, !isLocked, Modifier.weight(1f)) { largeInput = it }
            }

            if (isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Día bloqueado. Ya se registró producción.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar Producción")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirmar Registro") },
            text = { 
                val s = smallInput.ifBlank { "0" }
                val m = mediumInput.ifBlank { "0" }
                val l = largeInput.ifBlank { "0" }
                Text("¿Confirmas la carga de $s chicas, $m medianas y $l grandes para el ${selectedDateMillis.toUiDayDate()}?") 
            },
            confirmButton = {
                Button(onClick = {
                    onSave(selectedDateMillis, smallInput.toIntOrNull() ?: 0, mediumInput.toIntOrNull() ?: 0, largeInput.toIntOrNull() ?: 0)
                    showConfirm = false
                }) { Text("Confirmar") }
            },
            dismissButton = { OutlinedButton(onClick = { showConfirm = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun InventoryField(label: String, value: String, enabled: Boolean, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 3) onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label, fontSize = 12.sp) },
        enabled = enabled,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
    )
}

@Composable
private fun RemoveBrokenBaseCard(
    onRemoveSmall: () -> Unit,
    onRemoveMedium: () -> Unit,
    onRemoveLarge: () -> Unit
) {
    var pendingDiscardSize by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Descartar Bases Dañadas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            }
            Text("Usa estos botones para descontar del inventario bases que se rompieron o no se pueden usar.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscardButton("Chica", { pendingDiscardSize = "chica" }, Modifier.weight(1f))
                DiscardButton("Mediana", { pendingDiscardSize = "mediana" }, Modifier.weight(1f))
                DiscardButton("Grande", { pendingDiscardSize = "grande" }, Modifier.weight(1f))
            }
        }
    }

    if (pendingDiscardSize != null) {
        AlertDialog(
            onDismissRequest = { pendingDiscardSize = null },
            title = { Text("Confirmar Descarte") },
            text = { Text("¿Estás seguro de que deseas descontar 1 base ${pendingDiscardSize?.uppercase()}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        when(pendingDiscardSize) {
                            "chica" -> onRemoveSmall()
                            "mediana" -> onRemoveMedium()
                            "grande" -> onRemoveLarge()
                        }
                        pendingDiscardSize = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Descartar") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingDiscardSize = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DiscardButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Text("-1 $label", fontSize = 10.sp)
    }
}

@Composable
private fun PizzaBaseItem(base: PizzaBaseEntity, onMarkUsed: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Base ${base.size.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (base.usedAt != null) "Usada: ${base.usedAt.toUiDate()}" else "Disponible desde: ${base.createdAt.toUiDayDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (base.usedAt != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
            }
            if (base.usedAt == null) {
                IconButton(onClick = onMarkUsed) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = "Usar", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun DatePickerSelector(selectedDateMillis: Long, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val dateDialog = DatePickerDialog(context, { _, y, m, d ->
        val picked = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }
        onDateSelected(picked.timeInMillis)
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Fecha de Producción", style = MaterialTheme.typography.labelSmall)
            Text(selectedDateMillis.toUiDayDate(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row {
            TextButton(onClick = { dateDialog.show() }) {
                Icon(Icons.Default.EditCalendar, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Cambiar")
            }
            TextButton(onClick = { onDateSelected(Calendar.getInstance().timeInMillis) }) {
                Text("Hoy")
            }
        }
    }
}

@Composable
private fun AdminMenuContainer(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
            Text("Atrás", style = MaterialTheme.typography.titleMedium)
        }
        AdminMenuScreen()
    }
}

private fun Long.toUiDate(): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(this))
private fun Long.toUiDayDate(): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
private fun Long.isSameDayAs(other: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = this@isSameDayAs }
    val c2 = Calendar.getInstance().apply { timeInMillis = other }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
