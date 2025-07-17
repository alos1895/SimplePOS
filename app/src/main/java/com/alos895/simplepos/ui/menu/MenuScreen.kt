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
import androidx.compose.material3.MenuAnchorType
import com.alos895.simplepos.data.PizzeriaData

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
    val total = cartViewModel.total
    // Elimina referencias a isPrinting y message del ViewModel
    var isPrinting by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf("") }
    
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun buildTicket(): String {
        val info = PizzeriaData.info
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
        sb.appendLine("TOTAL:                $${"%.2f".format(total)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }

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
                            Button(onClick = { cartViewModel.removeFromCart(item.pizza, item.tamano) }) {
                                Text("-")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = { cartViewModel.addToCart(item.pizza, item.tamano) }) {
                                Text("+")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isPrinting = true
                    onPrintRequested?.invoke(buildTicket())
                    isPrinting = false
                },
                enabled = cartItems.isNotEmpty() && isPrinting,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text(if (isPrinting) "Imprimiendo..." else "Finalizar e imprimir ticket")
            }
            if (lastMessage.isNotEmpty()) {
                Text(lastMessage)
            }
        }
    }
} 