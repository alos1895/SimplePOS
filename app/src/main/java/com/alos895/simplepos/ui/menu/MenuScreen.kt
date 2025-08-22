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
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import com.alos895.simplepos.viewmodel.MenuViewModel
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
    val deliveryOptions = MenuData.deliveryOptions
    // Si el ViewModel no tiene un valor inicial, inicialízalo aquí
    val selectedDelivery by cartViewModel.selectedDelivery.collectAsState(initial = deliveryOptions.first())
    var deliveryMenuExpanded by remember { mutableStateOf(false) }
    
    // Estado para alternar entre vista de pizzas y postres
    var showPizzas by remember { mutableStateOf(true) }
    
    // Estado para manejar vista de comentarios
    var showComments by remember { mutableStateOf(false) }

    // Calcula el total reactivo (pizzas + postres + servicio a domicilio)
    val total by remember(cartItems, dessertItems, selectedDelivery) {
        derivedStateOf {
            cartItems.sumOf { it.subtotal } + dessertItems.sumOf { it.subtotal } + (selectedDelivery?.price ?: 0)
        }
    }
    var isPrinting by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Menú (izquierda)
            Column(
                modifier = Modifier.weight(0.8f).padding(8.dp)
            ) {
                Text("Menú", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            showPizzas = true 
                            showComments = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showPizzas && !showComments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Pizzas")
                    }
                    Button(
                        onClick = { 
                            showPizzas = false 
                            showComments = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showPizzas && !showComments) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Postres & Extras")
                    }
                    Button(
                        onClick = { 
                            showPizzas = false
                            showComments = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showComments) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Comentarios")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showPizzas) {
                        // Vista de Pizzas
                        items(pizzas) { pizza ->
                            var expanded by remember { mutableStateOf(false) }
                            var selectedTamano by remember { mutableStateOf(pizza.tamanos.first()) }
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = pizza.nombre, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pizza.ingredientesBaseIds
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
                                                value = selectedTamano.nombre,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Tamaño") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.menuAnchor().fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                pizza.tamanos.forEach { tamano ->
                                                    DropdownMenuItem(
                                                        text = { Text(tamano.nombre + " ($${"%.2f".format(tamano.precioBase)})") },
                                                        onClick = {
                                                            selectedTamano = tamano
                                                            expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Button(onClick = { cartViewModel.addToCart(pizza, selectedTamano) }) {
                                            Text("Agregar")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (showComments) {
                        // Vista de Comentarios
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Comentarios de la Orden", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Campo de comentarios
                                    OutlinedTextField(
                                        value = cartViewModel.comentarios.collectAsState().value,
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
                                    
                                    if (cartViewModel.comentarios.collectAsState().value.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Comentarios actuales:",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            cartViewModel.comentarios.collectAsState().value,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Vista de Postres
                        items(MenuData.postreOrExtras) { postre ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = postre.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(text = "$${"%.2f".format(postre.precio)}", style = MaterialTheme.typography.bodyMedium)
                                    }
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
            Box(
                modifier = Modifier.weight(1.2f).padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), // Reserve space for the button
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Título del carrito
                    item {
                        Text("Carrito de compras", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Items de pizzas en el carrito
                    items(cartItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                                Button(onClick = {
                                    cartViewModel.removeFromCart(item.pizza, item.tamano)
                                }) {
                                    Text("-")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = {
                                    cartViewModel.addToCart(item.pizza, item.tamano)
                                }) {
                                    Text("+")
                                }
                            }
                        }
                    }
                    
                    // Items de postres en el carrito
                    items(dessertItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.postreOrExtra.nombre)
                                    Text("Postre")
                                }
                                Text("x${item.cantidad}")
                                Text("$${"%.2f".format(item.subtotal)}")
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    cartViewModel.removeDessertFromCart(item.postreOrExtra)
                                }) {
                                    Text("-")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(onClick = {
                                    cartViewModel.addDessertToCart(item.postreOrExtra)
                                }) {
                                    Text("+")
                                }
                            }
                        }
                    }
                    
                    // Total
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        val totalPizzas = cartItems.sumOf { it.cantidad }
                        val totalDesserts = dessertItems.sumOf { it.cantidad }
                        val totalItems = totalPizzas + totalDesserts
                        Text("Total: $${"%.2f".format(total)} (${totalItems} items)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Nombre del cliente
                    item {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre del cliente") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Servicio a domicilio
                    item {
                        Text("Servicio a domicilio", style = MaterialTheme.typography.titleMedium)
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryMenuExpanded) },
                                modifier = Modifier.menuAnchor()
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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Botón de guardar orden (siempre visible en la parte inferior)
                Button(
                    onClick = {
                        val user = User(
                            id = System.currentTimeMillis(),
                            nombre = nombre,
                            telefono = telefono
                        )
                        cartViewModel.saveOrder(user)
                        lastMessage = "Orden guardada exitosamente"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(lastMessage)
                        }
                    },
                    enabled = cartItems.isNotEmpty() || dessertItems.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Guardar orden")
                }
            }
        }
    }
}
