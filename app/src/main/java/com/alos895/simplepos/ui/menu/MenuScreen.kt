package com.alos895.simplepos.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
import com.alos895.simplepos.ui.menu.MenuViewModel
import com.alos895.simplepos.viewmodel.CartViewModel
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onPrintRequested: ((String) -> Unit)? = null,
    bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel()
) {
    val menuViewModel: MenuViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()

    val pizzas by menuViewModel.pizzas.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val dessertItems by cartViewModel.dessertItems.collectAsState()
    val comentarios by cartViewModel.comentarios.collectAsState()
    val selectedDelivery by cartViewModel.selectedDelivery.collectAsState(initial = MenuData.deliveryOptions.first())

    val deliveryOptions = MenuData.deliveryOptions

    var deliveryMenuExpanded by remember { mutableStateOf(false) }
    var showPizzas by remember { mutableStateOf(true) }
    var showComments by remember { mutableStateOf(false) }

    val total by remember(cartItems, dessertItems, selectedDelivery) {
        derivedStateOf {
            cartItems.sumOf { it.subtotal } + dessertItems.sumOf { it.subtotal } + (selectedDelivery?.price
                ?: 0)
        }
    }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)) {
            // Menú (izquierda)
            Column(modifier = Modifier
                .weight(1f)
                .padding(8.dp)) {
                Text("Menú", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPizzas = true; showComments = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showPizzas && !showComments) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Pizzas") }

                    Button(
                        onClick = { showPizzas = false; showComments = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showPizzas && !showComments) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Postres & Extras") }

                    Button(
                        onClick = { showPizzas = false; showComments = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showComments) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Comentarios") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showPizzas) {
                        items(pizzas) { pizza ->
                            var expanded by remember { mutableStateOf(false) }
                            var selectedTamano by remember { mutableStateOf(pizza.tamanos.first()) }
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(pizza.nombre, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        pizza.ingredientesBaseIds
                                            .mapNotNull { id -> MenuData.ingredientes.find { it.id == id }?.nombre }
                                            .joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TextField(
                                                value = "${selectedTamano.nombre} ($${
                                                    "%.2f".format(
                                                        selectedTamano.precioBase
                                                    ) })",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Tamaño") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                                        expanded
                                                    )
                                                },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                pizza.tamanos.forEach { tamano ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                "${tamano.nombre} ($${
                                                                    "%.2f".format(
                                                                        tamano.precioBase
                                                                    )
                                                                })"
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedTamano = tamano
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Button(onClick = {
                                            cartViewModel.addToCart(
                                                pizza,
                                                selectedTamano
                                            )
                                        }) {
                                            Text("Agregar")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (showComments) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Comentarios de la Orden",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = comentarios,
                                        onValueChange = { cartViewModel.setComentarios(it) },
                                        label = { Text("Escribe aquí los comentarios de la orden") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 5,
                                        maxLines = 8,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    if (comentarios.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Comentarios actuales:",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            comentarios,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(MenuData.postreOrExtras) { postre ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(postre.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text("$${"%.2f".format(postre.precio)}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(onClick = { cartViewModel.addDessertToCart(postre) }) {
                                        Text("Agregar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Carrito (derecha)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // ocupa todo el espacio disponible
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Título del carrito
                    item {
                        Text("Carrito de compras", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Items de pizzas
                    items(cartItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.pizza.nombre)
                                    Text(item.tamano.nombre)
                                }
                                Text("x${item.cantidad}")
                                Text("$${"%.2f".format(item.subtotal)}")
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { cartViewModel.removeFromCart(item.pizza, item.tamano) }) { Text("-") }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = { cartViewModel.addToCart(item.pizza, item.tamano) }) { Text("+") }
                            }
                        }
                    }

                    // Items de postres
                    items(dessertItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column { Text(item.postreOrExtra.nombre); Text("Postre") }
                                Text("x${item.cantidad}")
                                Text("$${"%.2f".format(item.subtotal)}")
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { cartViewModel.removeDessertFromCart(item.postreOrExtra) }) { Text("-") }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = { cartViewModel.addDessertToCart(item.postreOrExtra) }) { Text("+") }
                            }
                        }
                    }

                    // Total
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        val totalItems = cartItems.sumOf { it.cantidad } + dessertItems.sumOf { it.cantidad }
                        Text(
                            "Total: $${"%.2f".format(total)} ($totalItems items)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Formulario de cliente y entrega
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Nombre del cliente
                                    OutlinedTextField(
                                        value = nombre,
                                        onValueChange = { nombre = it },
                                        label = { Text("Nombre del cliente") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Servicio a domicilio
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Servicio a domicilio",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        ExposedDropdownMenuBox(
                                            expanded = deliveryMenuExpanded,
                                            onExpandedChange = { deliveryMenuExpanded = it },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TextField(
                                                value = selectedDelivery?.let { "${it.zona} - $${it.price}" } ?: "Sin entrega",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Precio de domicilio") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryMenuExpanded)
                                                },
                                                modifier = Modifier.menuAnchor().fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = deliveryMenuExpanded,
                                                onDismissRequest = { deliveryMenuExpanded = false }
                                            ) {
                                                deliveryOptions.forEach { deliveryOption ->
                                                    DropdownMenuItem(
                                                        text = { Text("${deliveryOption.zona} - $${deliveryOption.price}") },
                                                        onClick = {
                                                            cartViewModel.setDeliveryService(deliveryOption)
                                                            deliveryMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Caja de dirección solo si el precio > 0
                                if ((selectedDelivery?.price ?: 0) > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = deliveryAddress,
                                        onValueChange = { deliveryAddress = it },
                                        label = { Text("Dirección de entrega") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón de Guardar Orden siempre pegado abajo
                Button(
                    onClick = {
                        val user = User(
                            id = System.currentTimeMillis(),
                            nombre = nombre,
                            telefono = telefono
                        )
                        cartViewModel.saveOrder(user, deliveryAddress)
                        // Limpiar formulario después de guardar
                        nombre = ""
                        telefono = ""
                        deliveryAddress = ""
                        cartViewModel.setComentarios("")
                        cartViewModel.setDeliveryService(MenuData.deliveryOptions.first())
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Orden guardada exitosamente")
                        }
                    },
                    enabled = cartItems.isNotEmpty() || dessertItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Guardar orden")
                }
            }
        }
    }
}