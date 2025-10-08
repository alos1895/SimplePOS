package com.alos895.simplepos.ui.menu

import java.util.Locale
import com.alos895.simplepos.model.PizzaFractionType
import com.alos895.simplepos.model.sizeLabel
import com.alos895.simplepos.model.unitPriceSingle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.User
import com.alos895.simplepos.model.DeliveryType
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
                val sections = remember { MenuSection.values().toList() }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sections.forEachIndexed { index, section ->
                        SegmentedButton(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            shape = SegmentedButtonDefaults.itemShape(index, sections.size),
                            icon = {
                                Icon(section.icon, contentDescription = section.label)
                            },
                            label = {
                                Text(section.label)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedSection) {
                        MenuSection.PIZZAS -> {
                            items(pizzas) { pizza ->
                                var selectedTamano by remember { mutableStateOf(pizza.tamanos.first()) }
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalPizza,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pizza.nombre, style = MaterialTheme.typography.titleLarge)
                                                Text(
                                                    pizza.ingredientesBaseIds
                                                        .mapNotNull { id -> MenuData.ingredientes.find { it.id == id }?.nombre }
                                                        .joinToString(", "),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            AssistChip(
                                                onClick = {},
                                                enabled = false,
                                                label = {
                                                    Text("$${"%.2f".format(selectedTamano.precioBase)}")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Filled.AttachMoney, contentDescription = null)
                                                }
                                            )
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(pizza.tamanos) { tamano ->
                                                FilterChip(
                                                    selected = selectedTamano == tamano,
                                                    onClick = { selectedTamano = tamano },
                                                    label = { Text(tamano.nombre) },
                                                    leadingIcon = if (selectedTamano == tamano) {
                                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                                    } else null
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                cartViewModel.addToCart(
                                                    pizza,
                                                    selectedTamano
                                                )
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Agregar")
                                        }
                                    }
                                }
                            }
                        }
                        MenuSection.COMENTARIOS -> {
                            item {
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Comment,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "Comentarios de la orden",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }

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
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    "Comentarios actuales:",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    comentarios,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        MenuSection.POSTRES_EXTRAS -> {
                            items(MenuData.postreOrExtras) { postre ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    ListItem(
                                        headlineText = { Text(postre.nombre) },
                                        supportingText = {
                                            Text("$${"%.2f".format(postre.precio)}")
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Filled.Icecream,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingContent = {
                                            Button(onClick = { cartViewModel.addDessertToCart(postre) }) {
                                                Text("Agregar")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        MenuSection.PIZZAS_COMBINADAS -> {
                            item {
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.PieChart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Pizzas combinadas", style = MaterialTheme.typography.titleLarge)
                                        }
                                        Text(
                                            "Arma pizzas grandes o medianas con varias especialidades.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (combinablePizzas.isEmpty()) {
                                            Text(
                                                "No hay pizzas combinables disponibles en este momento.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            FilledTonalButton(
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
                                            FilledTonalButton(
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
                var cartExpanded by remember { mutableStateOf(true) }
                var customerExpanded by remember { mutableStateOf(true) }
                val totalItems = cartItems.sumOf { it.cantidad } + dessertItems.sumOf { it.cantidad }
                val rightScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rightScrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard {
                        Column {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                headlineText = { Text("Resumen del carrito") },
                                trailingContent = {
                                    IconButton(onClick = { cartExpanded = !cartExpanded }) {
                                        Icon(
                                            imageVector = if (cartExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (cartExpanded) "Contraer" else "Expandir"
                                        )
                                    }
                                }
                            )
                            AnimatedVisibility(visible = cartExpanded) {
                                if (cartItems.isEmpty() && dessertItems.isEmpty()) {
                                    Text(
                                        "Tu carrito está vacío",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        cartItems.forEach { item ->
                                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                if (item.isCombo) {
                                                                    if (item.sizeLabel.isBlank()) "Pizza combinada"
                                                                    else "Pizza ${item.sizeLabel} combinada"
                                                                } else {
                                                                    item.pizza?.nombre ?: "Pizza"
                                                                },
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                            if (!item.isCombo && item.sizeLabel.isNotBlank()) {
                                                                Text(item.sizeLabel, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                            if (item.isCombo) {
                                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                    item.portions.forEach { portion ->
                                                                        Text(
                                                                            "- ${portion.fraction.label}: ${portion.pizzaName}",
                                                                            style = MaterialTheme.typography.bodySmall
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                Text(
                                                                    item.pizza?.ingredientesBaseIds
                                                                        ?.mapNotNull { id -> MenuData.ingredientes.find { it.id == id }?.nombre }
                                                                        ?.joinToString(", ") ?: "",
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            "$${String.format(Locale.getDefault(), "%.2f", item.unitPriceSingle)}",
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        FilterChip(
                                                            selected = item.isGolden,
                                                            onClick = { cartViewModel.toggleGolden(item.id) },
                                                            label = { Text("Doradita") },
                                                            leadingIcon = if (item.isGolden) {
                                                                { Icon(Icons.Filled.Check, contentDescription = null) }
                                                            } else null
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            OutlinedIconButton(onClick = { cartViewModel.decrementItem(item.id) }) {
                                                                Icon(Icons.Filled.Remove, contentDescription = "Disminuir")
                                                            }
                                                            Text(
                                                                "x${item.cantidad}",
                                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                            FilledIconButton(onClick = { cartViewModel.incrementItem(item.id) }) {
                                                                Icon(Icons.Filled.Add, contentDescription = "Incrementar")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        dessertItems.forEach { item ->
                                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                                ListItem(
                                                    headlineText = { Text(item.postreOrExtra.nombre) },
                                                    supportingText = {
                                                        Text("$${String.format(Locale.getDefault(), "%.2f", item.postreOrExtra.precio)} c/u")
                                                    },
                                                    trailingContent = {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                OutlinedIconButton(onClick = { cartViewModel.removeDessertFromCart(item.postreOrExtra) }) {
                                                                    Icon(Icons.Filled.Remove, contentDescription = "Restar postre")
                                                                }
                                                                Text("x${item.cantidad}", modifier = Modifier.padding(horizontal = 12.dp))
                                                                FilledIconButton(onClick = { cartViewModel.addDessertToCart(item.postreOrExtra) }) {
                                                                    Icon(Icons.Filled.Add, contentDescription = "Agregar postre")
                                                                }
                                                            }
                                                            Text(
                                                                "$${String.format(Locale.getDefault(), "%.2f", item.subtotal)}",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ElevatedCard {
                        Column {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                headlineText = { Text("Datos del cliente") },
                                trailingContent = {
                                    IconButton(onClick = { customerExpanded = !customerExpanded }) {
                                        Icon(
                                            imageVector = if (customerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (customerExpanded) "Contraer" else "Expandir"
                                        )
                                    }
                                }
                            )
                            AnimatedVisibility(visible = customerExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = nombre,
                                        onValueChange = { nombre = it },
                                        label = { Text("Nombre del cliente") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = telefono,
                                        onValueChange = { telefono = it },
                                        label = { Text("Teléfono de contacto") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                                    val requiresAddress = selectedDelivery?.type == DeliveryType.DOMICILIO ||
                                        selectedDelivery?.type == DeliveryType.CAMINANDO
                                    if (requiresAddress) {
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total a pagar", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "$${"%.2f".format(total)} · $totalItems ${if (totalItems == 1) "artículo" else "artículos"}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
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
                            enabled = cartItems.isNotEmpty() || dessertItems.isNotEmpty()
                        ) {
                            Text("Guardar orden")
                        }
                    }
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

private enum class MenuSection(val label: String, val icon: ImageVector) {
    PIZZAS("Pizzas", Icons.Filled.LocalPizza),
    POSTRES_EXTRAS("Postres & Extras", Icons.Filled.Icecream),
    COMENTARIOS("Comentarios", Icons.Filled.Comment),
    PIZZAS_COMBINADAS("Pizzas combinadas", Icons.Filled.PieChart)
}

private data class ComboDialogConfig(
    val title: String,
    val sizeName: String,
    val patterns: List<FractionPattern>
)

