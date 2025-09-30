package com.alos895.simplepos.ui.orders

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.ui.orders.OrderViewModel
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.User
import com.google.gson.Gson

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
    var showEditDialog by remember { mutableStateOf(false) }

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Columna con información de la orden
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Orden #${orderViewModel.getDailyOrderNumber(order)} - ${orderViewModel.getUser(order)?.nombre ?: "Cliente"}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if(order.isTOTODO) {
                                        Text("Total Cliente: $${String.format(Locale.US, "%.2f", order.total)}")
                                        Text("Total TOTODO: $${String.format(Locale.US, "%.2f", order.precioTOTODO)}")
                                    } else {
                                        Text("Total: $${String.format(Locale.US, "%.2f", order.total)}")
                                    }
                                    Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}")
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    if(orderViewModel.isOrderPaid(order)) {
                                        Text(
                                            text = "Pagada",
                                            color = Color(0xFF4CAF50), // Verde
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "Pendiente",
                                            color = Color(0xFFF44336), // Rojo
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (order.isDeliveried) {
                                        Icon(
                                            imageVector = Icons.Filled.Motorcycle,
                                            contentDescription = "Para llevar",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    if (order.isTOTODO) {
                                        Icon(
                                            imageVector = Icons.Filled.AddBusiness,
                                            contentDescription = "TOTODO",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
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
                selectedOrder?.let { order ->

                    var efectivoInput by remember {
                        mutableStateOf(orderViewModel.getPaymentAmount(order, PaymentMethod.EFECTIVO).toString())
                    }
                    var tarjetaInput by remember {
                        mutableStateOf(orderViewModel.getPaymentAmount(order, PaymentMethod.TRANSFERENCIA).toString())
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text("Detalle de la orden", style = MaterialTheme.typography.titleLarge) }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item { Text("Orden #${orderViewModel.getDailyOrderNumber(order)} --- Nombre: ${orderViewModel.getUser(order)?.nombre ?: "Desconocido"}") }
                        item { Text("Total: $${String.format(Locale.US, "%.2f", order.total)}") }
                        item { Text("Fecha: ${orderViewModel.formatDate(order.timestamp)}") }
                        item { HorizontalDivider(thickness = 1.dp, color = Color.Gray) }
                        item { Text("Items:") }
                        items(orderViewModel.getCartItems(order)) { item ->
                            Text("- ${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}")
                        }
                        if (orderViewModel.getDessertItems(order).isNotEmpty()) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item { Text("Extras:") }
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
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item { Text("Tipo de envío: ${orderViewModel.getDeliverySummary(order)}") }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isPrinting) "Imprimiendo..." else "Imprimir cocina")
                                }

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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isPrinting) "Imprimiendo..." else "Imprimir Cliente")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // Botones para llenar automáticamente los inputs
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        order.paymentBreakdownJson = "[]"
                                        orderViewModel.updatePayment(order, order.total, PaymentMethod.EFECTIVO)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Pago en efectivo registrado")
                                        }
                                        selectedOrder = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pagar en Efectivo")
                                }

                                Button(
                                    onClick = {
                                        order.paymentBreakdownJson = "[]"
                                        orderViewModel.updatePayment(order, order.total, PaymentMethod.TRANSFERENCIA)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Pago con tarjeta registrado")
                                        }
                                        selectedOrder = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pagar con Tarjeta")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            Button(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Editar orden")
                            }
                        }



                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item {
                            Button(
                                onClick = {
                                    orderToDelete = order
                                    showDeleteDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Borrar Orden", color = Color.White)
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

    if (showEditDialog && selectedOrder != null) {
        EditOrderDialog(
            order = selectedOrder!!,
            orderViewModel = orderViewModel,
            onDismiss = { showEditDialog = false },
            onOrderUpdated = { updatedOrder ->
                selectedOrder = updatedOrder
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderDialog(
    order: OrderEntity,
    orderViewModel: OrderViewModel,
    onDismiss: () -> Unit,
    onOrderUpdated: (OrderEntity) -> Unit = {}
) {
    val gson = remember { Gson() }
    val baseDeliveryOptions = remember { MenuData.deliveryOptions }
    val initialUser = remember(order.id) {
        orderViewModel.getUser(order) ?: User(id = order.id, nombre = "", telefono = "")
    }
    var nombreCliente by remember(order.id) { mutableStateOf(initialUser.nombre) }
    var comentarios by remember(order.id) { mutableStateOf(order.comentarios) }
    var direccion by remember(order.id) { mutableStateOf(order.deliveryAddress) }
    var deliveryMenuExpanded by remember { mutableStateOf(false) }
    val matchedDelivery = remember(order.id) {
        baseDeliveryOptions.firstOrNull { option ->
            option.price == order.deliveryServicePrice && (
                if (order.isDeliveried) !option.pickUp else true
            )
        }
    }
    val customDelivery = remember(order.id) {
        if (matchedDelivery == null && order.deliveryServicePrice != 0) {
            DeliveryService(
                price = order.deliveryServicePrice,
                description = "",
                zona = "Personalizado",
                pickUp = !order.isDeliveried
            )
        } else {
            null
        }
    }
    val deliveryOptions = remember(order.id, customDelivery) {
        if (customDelivery != null) baseDeliveryOptions + customDelivery else baseDeliveryOptions
    }
    var selectedDelivery by remember(order.id) {
        mutableStateOf(matchedDelivery ?: customDelivery ?: baseDeliveryOptions.first())
    }
    val requiresAddress = selectedDelivery.price > 0

    fun formatDeliveryLabel(delivery: DeliveryService): String {
        return if (delivery.price > 0) "${delivery.zona} - $${delivery.price}" else delivery.zona
    }

    fun buildUpdatedOrder(): OrderEntity {
        val updatedDeliveryPrice = selectedDelivery.price
        val updatedIsDeliveried = updatedDeliveryPrice > 0
        val sanitizedAddress = if (updatedIsDeliveried) direccion.trim() else ""
        val updatedUser = initialUser.copy(nombre = nombreCliente)
        val adjustedTotal = order.total - order.deliveryServicePrice + updatedDeliveryPrice
        return order.copy(
            comentarios = comentarios,
            deliveryAddress = sanitizedAddress,
            deliveryServicePrice = updatedDeliveryPrice,
            isDeliveried = updatedIsDeliveried,
            userJson = gson.toJson(updatedUser),
            total = adjustedTotal
        )
    }

    val confirmEnabled = !requiresAddress || direccion.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Orden #${order.id}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nombreCliente,
                    onValueChange = { nombreCliente = it },
                    label = { Text("Nombre del cliente") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comentarios,
                    onValueChange = { comentarios = it },
                    label = { Text("Comentarios") }
                )

                Text("Tipo de entrega", style = MaterialTheme.typography.titleSmall)

                ExposedDropdownMenuBox(
                    expanded = deliveryMenuExpanded,
                    onExpandedChange = { deliveryMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = formatDeliveryLabel(selectedDelivery),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servicio a domicilio") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = deliveryMenuExpanded,
                        onDismissRequest = { deliveryMenuExpanded = false }
                    ) {
                        deliveryOptions.forEach { deliveryOption ->
                            DropdownMenuItem(
                                text = { Text(formatDeliveryLabel(deliveryOption)) },
                                onClick = {
                                    selectedDelivery = deliveryOption
                                    if (deliveryOption.price == 0) {
                                        direccion = ""
                                    }
                                    deliveryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (requiresAddress) {
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección de entrega") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedOrder = buildUpdatedOrder()
                    orderViewModel.updateOrder(updatedOrder)
                    onOrderUpdated(updatedOrder)
                    onDismiss()
                },
                enabled = confirmEnabled
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
