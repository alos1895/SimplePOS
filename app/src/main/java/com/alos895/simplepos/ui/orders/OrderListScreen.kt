package com.alos895.simplepos.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.CartViewModel
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun OrderListScreen(
    orderViewModel: OrderViewModel = viewModel(),
    bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel()
) {
    val orders by orderViewModel.orders.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { orderViewModel.loadOrders() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            items(orders) { order ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Orden #${order.id}")
                        Text("Total: $${"%.2f".format(order.total)}")
                        Text("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(order.timestamp))}")
                        Text("Items:")
                        order.items.forEach { item ->
                            Text("- ${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                isPrinting = true
                                val ticket = buildOrderTicket(order)
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
                            Text(if (isPrinting) "Imprimiendo..." else "Imprimir")
                        }
                        if (lastMessage.isNotEmpty()) {
                            Text(lastMessage)
                        }
                    }
                }
            }
        }
    }
}

// Utilidad para construir el ticket de una orden específica
fun buildOrderTicket(order: com.alos895.simplepos.model.Order): String {
    val info = com.alos895.simplepos.data.PizzeriaData.info
    val sb = StringBuilder()
    sb.appendLine(info.logoAscii)
    sb.appendLine(info.nombre)
    sb.appendLine(info.telefono)
    sb.appendLine(info.direccion)
    sb.appendLine("-------------------------------")
    order.items.forEach { item ->
        sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
    }
    sb.appendLine("-------------------------------")
    sb.appendLine("TOTAL: $${"%.2f".format(order.total)}")
    sb.appendLine("¡Gracias por su compra!")
    return sb.toString()
}
