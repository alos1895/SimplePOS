package com.alos895.simplepos.ui.orders

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.launch
import com.alos895.simplepos.model.OrderEntity

@Composable
fun OrderListScreen(
    orderViewModel: OrderViewModel = viewModel(),
    bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel()
) {
    val orders by orderViewModel.orders.collectAsState()
    val selectedDate by orderViewModel.selectedDate.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    var selectedOrder by remember { mutableStateOf<OrderEntity?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { orderViewModel.loadOrders() }

    // DatePickerDialog setup
    val calendar = Calendar.getInstance()
    selectedDate?.let { calendar.time = it }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCal = Calendar.getInstance()
            pickedCal.set(year, month, dayOfMonth, 0, 0, 0)
            orderViewModel.setSelectedDate(pickedCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Filtros y lista de órdenes (izquierda)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedDate?.let {
                            "Filtrando: ${SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)}"
                        } ?: "Todas las órdenes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Elegir día")
                    }
                    Button(onClick = { orderViewModel.setSelectedDate(OrderViewModel.getToday()) }) {
                        Text("Hoy")
                    }
                }
                
                // Sección CAJA
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CAJA", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val dailyStats = orderViewModel.getDailyStats(selectedDate)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Chicas: ${dailyStats.pizzasChicas}", style = MaterialTheme.typography.bodySmall)
                                Text("Medianas: ${dailyStats.pizzasMedianas}", style = MaterialTheme.typography.bodySmall)
                                Text("Grandes: ${dailyStats.pizzasGrandes}", style = MaterialTheme.typography.bodySmall)
                                Text("Totales Pizzas: ${dailyStats.pizzas}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("Órdenes: ${dailyStats.ordenes}", style = MaterialTheme.typography.bodyMedium)
                                Text("Envíos: ${dailyStats.envios}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ingresos: $${"%.2f".format(dailyStats.ingresos)}", 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(orderViewModel.ordersBySelectedDate(orders, selectedDate)) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { selectedOrder = order }
                                .background(
                                    if (selectedOrder?.id == order.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Orden #${orderViewModel.getDailyOrderNumber(order)} - ${orderViewModel.getUser(order).nombre}" , style = MaterialTheme.typography.titleMedium)
                                Text("Total: $${"%.2f".format(order.total)}")
                                Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}")
                            }
                        }
                    }
                }
                
            }
            // Detalle y acciones de la orden seleccionada (derecha)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                if (selectedOrder != null) {
                    val order = selectedOrder!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text("Detalle de la orden", style = MaterialTheme.typography.titleLarge) }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item { Text("Orden #${orderViewModel.getDailyOrderNumber(order)}") }
                        item { Text("Nombre: ${orderViewModel.getUser(order)?.nombre ?: "Desconocido"}") }
                        item { Text("Total: $${"%.2f".format(order.total)}") }
                        item { Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}") }
                        item { HorizontalDivider(thickness = 1.dp, color = Color.Gray) }
                        item { Text("Items:") }
                        items(orderViewModel.getCartItems(order)) { item ->
                            Text("- ${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
                        }
                        if (orderViewModel.getDessertItems(order).isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text("Postres:") }
                            items(orderViewModel.getDessertItems(order)) { item ->
                                Text("- ${item.cantidad}x ${item.postreOrExtra.nombre}")
                            }
                        }
                        if (order.comentarios.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text("Comentarios:", style = MaterialTheme.typography.titleMedium) }
                            item { Text(order.comentarios, style = MaterialTheme.typography.bodyMedium) }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                        item { HorizontalDivider(thickness = 1.dp, color = Color.Gray) }
                        if (order.isDeliveried) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text("Envío: ${order.deliveryAddress}") }
                        } else {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text("Pasan o Caminando!") }
                        }
                        item {
                            Button(
                                onClick = {
                                    isPrinting = true
                                    val cocinaTicket = orderViewModel.buildCocinaTicket(order)
                                    bluetoothPrinterViewModel.print(cocinaTicket) { success, message ->
                                        lastMessage = message
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                        isPrinting = false
                                    }
                                },
                                enabled = !isPrinting,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isPrinting) "Imprimiendo..." else "Imprimir cocina")
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item {
                            Button(
                                onClick = {
                                    isPrinting = true
                                    val ticket = orderViewModel.buildOrderTicket(order)
                                    bluetoothPrinterViewModel.print(ticket) { success, message ->
                                        lastMessage = message
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                        isPrinting = false
                                    }
                                },
                                enabled = !isPrinting,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isPrinting) "Imprimiendo..." else "Imprimir Cliente")
                            }
                        }
                        if (lastMessage.isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text(lastMessage) }
                        }
                    }
                } else {
                    Text("Selecciona una orden para ver detalles.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
