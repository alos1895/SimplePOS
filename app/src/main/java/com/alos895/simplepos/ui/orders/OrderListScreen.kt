package com.alos895.simplepos.ui.orders

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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
import java.util.Locale
import kotlinx.coroutines.launch
import com.alos895.simplepos.model.OrderEntity
import java.util.Date

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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<OrderEntity?>(null) }
    val listState = rememberLazyListState()

    // Load orders once when the screen is first composed
    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }

    val calendar = Calendar.getInstance()
    selectedDate?.let { calendar.time = it } // Use the selectedDate from ViewModel

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
                            "Filtrando: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)}"
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

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    // 'orders' StateFlow from ViewModel is already filtered by selectedDate
                    items(orders, key = { order -> order.id }) { order ->
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
                                Text(
                                    text = "Orden #${orderViewModel.getDailyOrderNumber(order)} - ${orderViewModel.getUser(order)?.nombre ?: "Cliente"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("Total: $${String.format(Locale.US, "%.2f", order.total)}")
                                Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        orderToDelete = order
                                        showDeleteDialog = true
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Borrar orden")
                                    }
                                }
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
                selectedOrder?.let { order -> // Use selectedOrder directly
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text("Detalle de la orden", style = MaterialTheme.typography.titleLarge) }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item { Text("Orden #${orderViewModel.getDailyOrderNumber(order)}") }
                        item { Text("Nombre: ${orderViewModel.getUser(order)?.nombre ?: "Desconocido"}") }
                        item { Text("Total: $${String.format(Locale.US, "%.2f", order.total)}") }
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
                                    bluetoothPrinterViewModel.print(cocinaTicket) { _, message ->
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
                                    bluetoothPrinterViewModel.print(ticket) { _, message ->
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
                } ?: run {
                    Text("Selecciona una orden para ver detalles.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showDeleteDialog && orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                orderToDelete = null 
            },
            title = { Text("Confirmar borrado") },
            text = { Text("¿Seguro que deseas borrar esta orden? Esta acción no elimina la orden físicamente, solo la oculta.") },
            confirmButton = {
                Button(onClick = {
                    orderToDelete?.let { orderToActuallyDelete ->
                        orderViewModel.deleteOrderLogical(orderToActuallyDelete.id)
                        val deleteTicket = orderViewModel.buildDeleteTicket(orderToActuallyDelete)
                        bluetoothPrinterViewModel.print(deleteTicket) { _, message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                    if (selectedOrder?.id == orderToDelete?.id) selectedOrder = null
                    showDeleteDialog = false
                    orderToDelete = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Orden borrada (lógicamente)")
                    }
                }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    orderToDelete = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
