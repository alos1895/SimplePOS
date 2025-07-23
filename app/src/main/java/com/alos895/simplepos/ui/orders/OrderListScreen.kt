package com.alos895.simplepos.ui.orders

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.CartViewModel
import com.alos895.simplepos.viewmodel.CartViewModelFactory
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.Order
import com.alos895.simplepos.data.PizzeriaData

fun parseCartItems(itemsJson: String): List<CartItem> {
    val gson = Gson()
    val type = object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type
    return gson.fromJson(itemsJson, type)
}

@Composable
fun OrderListScreen(
    orderViewModel: OrderViewModel = viewModel(),
    bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel()
) {
    val context = LocalContext.current
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModelFactory(context.applicationContext as Application))
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
                        val cartItems = parseCartItems(order.itemsJson)
                        cartItems.forEach { item ->
                            Text("- ${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                isPrinting = true
                                val ticket = buildOrderTicketEntity(order)
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
                                val cocinaTicket = buildCocinaTicket(order)
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

// Utilidad para construir el ticket de una orden específica desde OrderEntity
fun buildOrderTicketEntity(order: OrderEntity): String {
    val info = PizzeriaData.info
    val cartItems = parseCartItems(order.itemsJson)
    val sb = StringBuilder()
    sb.appendLine(info.logoAscii)
    sb.appendLine(info.nombre)
    sb.appendLine(info.telefono)
    sb.appendLine(info.direccion)
    sb.appendLine("-------------------------------")
    cartItems.forEach { item ->
        sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
    }
    sb.appendLine("-------------------------------")
    sb.appendLine("TOTAL: $${"%.2f".format(order.total)}")
    sb.appendLine("¡Gracias por su compra!")
    return sb.toString()
}

// Ticket para cocina: solo hora, pizzas y ingredientes
fun buildCocinaTicket(order: OrderEntity): String {
    val cartItems = parseCartItems(order.itemsJson)
    val sb = StringBuilder()
    sb.appendLine("ORDEN PARA COCINA")
    sb.appendLine("Hora: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.timestamp))}")
    sb.appendLine("-------------------------------")
    cartItems.forEach { item ->
        sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
        sb.appendLine("Ingredientes:")
        item.pizza.ingredientesBase.forEach { ingrediente ->
            sb.appendLine("- ${ingrediente.nombre}")
        }
        sb.appendLine()
    }
    sb.appendLine("-------------------------------")
    return sb.toString()
}
