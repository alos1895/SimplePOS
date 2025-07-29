package com.alos895.simplepos.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
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
    val deliveryOptions = MenuData.deliveryOptions
    // Si el ViewModel no tiene un valor inicial, inicialízalo aquí
    val selectedDelivery by cartViewModel.selectedDelivery.collectAsState(initial = deliveryOptions.first())
    var deliveryMenuExpanded by remember { mutableStateOf(false) }
    // Calcula el total reactivo (pizzas + servicio a domicilio)
    val total by remember(cartItems, selectedDelivery) {
        derivedStateOf {
            cartItems.sumOf { it.subtotal } + (selectedDelivery?.price ?: 0)
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
                modifier = Modifier.weight(1f).padding(8.dp)
            ) {
                Text("Menú de Pizzas", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded },
                                    modifier = Modifier.navigationBarsPadding()
                                ) {
                                    TextField(
                                        value = selectedTamano.nombre,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tamaño") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier.menuAnchor()
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { cartViewModel.addToCart(pizza, selectedTamano) }) {
                                    Text("Agregar al carrito")
                                }
                            }
                        }
                    }
                }
            }
            // Carrito (derecha)
            Column(
                modifier = Modifier.weight(1f).padding(8.dp)
            ) {
                Text("Carrito de compras", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
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
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del cliente") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text("Guardar orden")
                }
                if (lastMessage.isNotEmpty()) {
                    Text(lastMessage)
                }
            }
        }
    }
}
