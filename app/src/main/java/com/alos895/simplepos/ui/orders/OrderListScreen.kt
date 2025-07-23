package com.alos895.simplepos.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.alos895.simplepos.model.OrderEntity

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
                        Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}")
                        Text("Items:")
                        orderViewModel.getCartItems(order).forEach { item ->
                            Text("- ${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                            Text(if (isPrinting) "Imprimiendo..." else "Imprimir")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                        if (lastMessage.isNotEmpty()) {
                            Text(lastMessage)
                        }
                    }
                }
            }
        }
    }
}
