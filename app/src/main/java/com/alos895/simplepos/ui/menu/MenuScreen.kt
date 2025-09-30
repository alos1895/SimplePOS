package com.alos895.simplepos.ui.menu

import java.util.Locale
import com.alos895.simplepos.model.PizzaFractionType
import com.alos895.simplepos.model.sizeLabel
import com.alos895.simplepos.model.unitPriceSingle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
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
    var selectedSection by remember { mutableStateOf(MenuSection.PIZZAS) }
    val largeComboPatterns = remember {
        listOf(
            FractionPattern("quarters", "4 cuartos (1/4)", List(4) { PizzaFractionType.QUARTER }),
            FractionPattern("thirds", "3 tercios (1/3)", List(3) { PizzaFractionType.THIRD }),
            FractionPattern("halves", "2 mitades (1/2)", List(2) { PizzaFractionType.HALF })
        )
    }
    val mediumComboPatterns = remember {
        listOf(
            FractionPattern("halves", "2 mitades (1/2)", List(2) { PizzaFractionType.HALF })
        )
    }
    var comboDialogConfig by remember { mutableStateOf<ComboDialogConfig?>(null) }
    val combinablePizzas = remember(pizzas) { pizzas.filter { it.esCombinable } }

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
                        onClick = { selectedSection = MenuSection.PIZZAS },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == MenuSection.PIZZAS) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Pizzas") }

                    Button(
                        onClick = { selectedSection = MenuSection.POSTRES_EXTRAS },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == MenuSection.POSTRES_EXTRAS) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Postres & Extras") }

                    Button(
                        onClick = { selectedSection = MenuSection.COMENTARIOS },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == MenuSection.COMENTARIOS) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Comentarios") }

                    Button(
                        onClick = { selectedSection = MenuSection.PIZZAS_COMBINADAS },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSection == MenuSection.PIZZAS_COMBINADAS) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Pizzas combinadas") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedSection) {
                        MenuSection.PIZZAS -> {
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
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ExposedDropdownMenuBox(
                                                expanded = expanded,
                                                onExpandedChange = { expanded = !expanded },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                OutlinedTextField(
                                                    value = "${selectedTamano.nombre} ($${"%.2f".format(selectedTamano.precioBase) })",
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
                                                                    "${tamano.nombre} ($${"%.2f".format(tamano.precioBase)})"
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
                                            Button(
                                                onClick = {
                                                    cartViewModel.addToCart(
                                                        pizza,
                                                        selectedTamano
                                                    )
                                                },
                                                modifier = Modifier.widthIn(min = 120.dp)
                                            ) {
                                                Text("Agregar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        MenuSection.COMENTARIOS -> {
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
                                            label = { Text("Escribe aqu? los comentarios de la orden") },
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
                        }
                        MenuSection.POSTRES_EXTRAS -> {
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
                        MenuSection.PIZZAS_COMBINADAS -> {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Pizzas combinadas", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Arma pizzas grandes o medianas con varias especialidades.")
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (combinablePizzas.isEmpty()) {
                                            Text(
                                                "No hay pizzas combinables disponibles en este momento.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            Button(
                                                onClick = {
                                                    comboDialogConfig = ComboDialogConfig(
                                                        title = "Arma tu pizza grande combinada",
                                                        sizeName = "Extra Grande",
                                                        patterns = largeComboPatterns
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = combinablePizzas.isNotEmpty()
                                            ) {
                                                Text("Pizza grande combinada")
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    comboDialogConfig = ComboDialogConfig(
                                                        title = "Arma tu pizza mediana 1/2 y 1/2",
                                                        sizeName = "Mediana",
                                                        patterns = mediumComboPatterns
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = combinablePizzas.isNotEmpty()
                                            ) {
                                                Text("Pizza mediana 1/2 y 1/2")
                                            }
                                        }
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
                    items(items = cartItems, key = { it.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val sizeLabel = item.sizeLabel
                                val title = if (item.isCombo) {
                                    if (sizeLabel.isBlank()) "Pizza combinada" else "Pizza ${sizeLabel} combinada"
                                } else {
                                    item.pizza?.nombre ?: "Pizza"
                                }
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                if (!item.isCombo && sizeLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(sizeLabel, style = MaterialTheme.typography.bodyMedium)
                                }
                                if (item.isCombo) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    item.portions.forEach { portion ->
                                        Text("- ${portion.fraction.label} ${portion.pizzaName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FilterChip(
                                    selected = item.isGolden,
                                    onClick = { cartViewModel.toggleGolden(item.id) },
                                    label = { Text("Doradita") },
                                    leadingIcon = if (item.isGolden) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else {
                                        null
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(onClick = { cartViewModel.decrementItem(item.id) }) { Text("-") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("x${item.cantidad}")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { cartViewModel.incrementItem(item.id) }) { Text("+") }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("$${String.format(Locale.getDefault(), "%.2f", item.unitPriceSingle)}")
                                }
                            }
                        }
                    }
                    // Items de postres
                    items(dessertItems) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.postreOrExtra.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("$${String.format(Locale.getDefault(), "%.2f", item.postreOrExtra.precio)}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { cartViewModel.removeDessertFromCart(item.postreOrExtra) }) { Text("-") }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("x${item.cantidad}")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { cartViewModel.addDessertToCart(item.postreOrExtra) }) { Text("+") }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("$${String.format(Locale.getDefault(), "%.2f", item.subtotal)}")
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
                        coroutineScope.launch {
                            val orderTimestamp = System.currentTimeMillis()
                            val user = User(
                                id = orderTimestamp,
                                nombre = nombre,
                                telefono = telefono
                            )
                            val deliveryForTicket = selectedDelivery
                            val savedOrder = cartViewModel.saveOrder(user, deliveryAddress, orderTimestamp)
                            val cocinaTicket = cartViewModel.buildCocinaTicket(
                                user = user,
                                deliveryAddress = deliveryAddress,
                                deliveryService = deliveryForTicket,
                                timestamp = orderTimestamp,
                                dailyOrderNumber = savedOrder.dailyOrderNumber
                            )

                            bluetoothPrinterViewModel.print(cocinaTicket) { _, message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }

                            cartViewModel.clearCart()
                            nombre = ""
                            telefono = ""
                            deliveryAddress = ""
                            cartViewModel.setComentarios("")
                            cartViewModel.setDeliveryService(MenuData.deliveryOptions.first())
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
    comboDialogConfig?.let { config ->
        ComboPizzaDialog(
            title = config.title,
            sizeName = config.sizeName,
            patterns = config.patterns,
            pizzas = combinablePizzas,
            onDismiss = { comboDialogConfig = null },
            onConfirm = { portions ->
                cartViewModel.addComboToCart(config.sizeName, portions)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Pizza combinada agregada al carrito")
                }
            }
        )
    }
}

private enum class MenuSection {
    PIZZAS,
    POSTRES_EXTRAS,
    COMENTARIOS,
    PIZZAS_COMBINADAS
}

private data class ComboDialogConfig(
    val title: String,
    val sizeName: String,
    val patterns: List<FractionPattern>
)

