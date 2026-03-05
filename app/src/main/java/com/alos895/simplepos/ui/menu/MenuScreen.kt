package com.alos895.simplepos.ui.menu

import java.util.Locale
import com.alos895.simplepos.model.PizzaFractionType
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.sizeLabel
import com.alos895.simplepos.model.unitPriceSingle
import com.alos895.simplepos.model.displayName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val ingredientes by menuViewModel.ingredientes.collectAsState()
    val desserts by menuViewModel.postres.collectAsState()
    val extras by menuViewModel.extras.collectAsState()
    val combos by menuViewModel.combos.collectAsState()
    val bebidas by menuViewModel.bebidas.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val dessertItems by cartViewModel.dessertItems.collectAsState()
    val comentarios by cartViewModel.comentarios.collectAsState()
    val selectedDelivery by cartViewModel.selectedDelivery.collectAsState(initial = MenuData.deliveryOptions.first())

    val deliveryOptions = MenuData.deliveryOptions

    var deliveryMenuExpanded by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(MenuSection.PIZZAS) }
    
    val largeComboPatterns = remember {
        listOf(
            FractionPattern("halves", "2 mitades (1/2)", List(2) { PizzaFractionType.HALF }),
            FractionPattern("quarters", "4 cuartos (1/4)", List(4) { PizzaFractionType.QUARTER }),
            FractionPattern("thirds", "3 tercios (1/3)", List(3) { PizzaFractionType.THIRD })
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
            cartItems.sumOf { it.subtotal } + dessertItems.sumOf { it.subtotal } + (selectedDelivery?.price ?: 0)
        }
    }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var priceEditorItem by remember { mutableStateOf<CartItem?>(null) }
    var priceEditorInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        cartViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Modal de edición de precio (estilizado)
    if (priceEditorItem != null) {
        val normalizedInput = priceEditorInput.replace(",", ".")
        val parsedPrice = normalizedInput.toDoubleOrNull()
        AlertDialog(
            onDismissRequest = {
                priceEditorItem = null
                priceEditorInput = ""
            },
            title = { Text("Editar Precio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(priceEditorItem?.displayName ?: "Producto", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = priceEditorInput,
                        onValueChange = { priceEditorInput = it },
                        label = { Text("Precio unitario") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = priceEditorItem
                        if (item != null && parsedPrice != null && parsedPrice >= 0) {
                            cartViewModel.updateItemPrice(item.id, parsedPrice)
                        }
                        priceEditorItem = null
                        priceEditorInput = ""
                    },
                    enabled = parsedPrice != null && parsedPrice >= 0,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    priceEditorItem = null
                    priceEditorInput = ""
                }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Punto de Venta", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            // --- COLUMNA IZQUIERDA: MENÚ ---
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                // Selector de secciones (Segmented Buttons)
                val sections = remember { MenuSection.values().toList() }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    sections.forEachIndexed { index, section ->
                        SegmentedButton(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            shape = SegmentedButtonDefaults.itemShape(index, sections.size),
                            icon = { Icon(section.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text(section.label, fontSize = 12.sp, maxLines = 1) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    when (selectedSection) {
                        MenuSection.PIZZAS -> {
                            item {
                                PizzaSelectionCard(
                                    pizzas = pizzas,
                                    ingredientes = ingredientes,
                                    onAdd = { p, t -> cartViewModel.addToCart(p, t) }
                                )
                            }
                        }
                        MenuSection.PIZZAS_COMBINADAS -> {
                            item {
                                CombinedPizzaCard(
                                    combinablePizzas = combinablePizzas,
                                    onOpenLarge = {
                                        comboDialogConfig = ComboDialogConfig(
                                            title = "Pizza Grande Combinada",
                                            sizeName = "Extra Grande",
                                            patterns = largeComboPatterns
                                        )
                                    },
                                    onOpenMedium = {
                                        comboDialogConfig = ComboDialogConfig(
                                            title = "Pizza Mediana 1/2 y 1/2",
                                            sizeName = "Mediana",
                                            patterns = mediumComboPatterns
                                        )
                                    }
                                )
                            }
                        }
                        else -> {
                            // Renderizado genérico para Bebidas, Postres, Combos, Extras
                            val itemsToShow = when (selectedSection) {
                                MenuSection.POSTRES -> desserts
                                MenuSection.COMBOS -> combos
                                MenuSection.BEBIDAS -> bebidas
                                MenuSection.EXTRAS -> extras
                                else -> emptyList()
                            }
                            
                            if (itemsToShow.isEmpty() && selectedSection != MenuSection.COMENTARIOS) {
                                item {
                                    EmptySectionPlaceholder(selectedSection.label)
                                }
                            } else if (selectedSection == MenuSection.COMENTARIOS) {
                                item {
                                    CommentsCard(comentarios) { cartViewModel.setComentarios(it) }
                                }
                            } else {
                                items(itemsToShow) { item ->
                                    MenuProductItem(
                                        name = item.nombre,
                                        price = item.precio,
                                        icon = selectedSection.icon,
                                        onAdd = { cartViewModel.addDessertToCart(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- COLUMNA DERECHA: CARRITO ---
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Resumen de Orden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Lista de Pizzas en el Carrito
                        cartItems.forEach { item ->
                            CartPizzaItem(
                                item = item,
                                ingredientes = ingredientes,
                                onIncrement = { cartViewModel.incrementItem(item.id) },
                                onDecrement = { cartViewModel.decrementItem(item.id) },
                                onToggleGolden = { cartViewModel.toggleGolden(item.id) },
                                onEditPrice = {
                                    priceEditorItem = item
                                    priceEditorInput = String.format(Locale.getDefault(), "%.2f", item.unitPriceSingle)
                                }
                            )
                        }

                        // Lista de Otros Productos (Postres, etc)
                        dessertItems.forEach { item ->
                            CartGenericItem(
                                item = item,
                                onIncrement = { cartViewModel.addDessertToCart(item.postreOrExtra) },
                                onDecrement = { cartViewModel.removeDessertFromCart(item.postreOrExtra) }
                            )
                        }

                        if (cartItems.isEmpty() && dessertItems.isEmpty()) {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Carrito vacío", color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Datos del Cliente
                        CustomerDataSection(
                            nombre = nombre,
                            onNombreChange = { nombre = it },
                            deliveryAddress = deliveryAddress,
                            onAddressChange = { deliveryAddress = it },
                            selectedDelivery = selectedDelivery,
                            deliveryOptions = deliveryOptions,
                            onDeliverySelect = { cartViewModel.setDeliveryService(it) }
                        )
                    }

                    // BOTÓN DE PAGO / GUARDAR
                    Spacer(Modifier.height(16.dp))
                    OrderFooter(
                        total = total,
                        totalItems = cartItems.sumOf { it.cantidad } + dessertItems.sumOf { it.cantidad },
                        onSave = {
                            coroutineScope.launch {
                                val orderTimestamp = System.currentTimeMillis()
                                val user = User(id = orderTimestamp, nombre = nombre, telefono = telefono)
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
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }

                                // Reset UI
                                cartViewModel.clearCart()
                                nombre = ""; telefono = ""; deliveryAddress = ""
                                cartViewModel.setComentarios("")
                                cartViewModel.setDeliveryService(MenuData.deliveryOptions.first())
                                snackbarHostState.showSnackbar("Orden guardada exitosamente")
                            }
                        },
                        enabled = cartItems.isNotEmpty() || dessertItems.isNotEmpty()
                    )
                }
            }
        }
    }

    // Dialogs de combinación
    comboDialogConfig?.let { config ->
        ComboPizzaDialog(
            title = config.title,
            sizeName = config.sizeName,
            patterns = config.patterns,
            pizzas = combinablePizzas,
            onDismiss = { comboDialogConfig = null },
            onConfirm = { portions -> cartViewModel.addComboToCart(config.sizeName, portions) }
        )
    }
}

// --- SUB-COMPONENTES ESTILIZADOS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PizzaSelectionCard(
    pizzas: List<com.alos895.simplepos.model.Pizza>,
    ingredientes: List<com.alos895.simplepos.model.Ingrediente>,
    onAdd: (com.alos895.simplepos.model.Pizza, com.alos895.simplepos.model.TamanoPizza) -> Unit
) {
    var pizzaMenuExpanded by remember { mutableStateOf(false) }
    var selectedPizza by remember(pizzas) { mutableStateOf(pizzas.firstOrNull()) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    var selectedTamano by remember(selectedPizza) { mutableStateOf(selectedPizza?.tamanos?.firstOrNull()) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Selecciona Especialidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            ExposedDropdownMenuBox(expanded = pizzaMenuExpanded, onExpandedChange = { pizzaMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedPizza?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pizza") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pizzaMenuExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = pizzaMenuExpanded, onDismissRequest = { pizzaMenuExpanded = false }) {
                    pizzas.forEach { pizza ->
                        DropdownMenuItem(
                            text = { Text(pizza.nombre) },
                            onClick = {
                                selectedPizza = pizza
                                selectedTamano = pizza.tamanos.firstOrNull()
                                pizzaMenuExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = sizeMenuExpanded, onExpandedChange = { sizeMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedTamano?.let { "${it.nombre} ($${it.precioBase})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tamaño") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeMenuExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = sizeMenuExpanded, onDismissRequest = { sizeMenuExpanded = false }) {
                    selectedPizza?.tamanos?.forEach { tamano ->
                        DropdownMenuItem(
                            text = { Text("${tamano.nombre} - $${tamano.precioBase}") },
                            onClick = {
                                selectedTamano = tamano
                                sizeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            selectedPizza?.let { pizza ->
                val names = pizza.ingredientesBaseIds.mapNotNull { id -> ingredientes.find { it.id == id }?.nombre }
                Text("Incluye: ${names.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Button(
                onClick = { selectedPizza?.let { p -> selectedTamano?.let { t -> onAdd(p, t) } } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedPizza != null && selectedTamano != null
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar al Carrito")
            }
        }
    }
}

@Composable
private fun CombinedPizzaCard(
    combinablePizzas: List<com.alos895.simplepos.model.Pizza>,
    onOpenLarge: () -> Unit,
    onOpenMedium: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Armar Combinada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Crea una pizza con diferentes especialidades por mitad o cuartos.", style = MaterialTheme.typography.bodySmall)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenMedium, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Mediana 1/2", fontSize = 12.sp)
                }
                Button(onClick = onOpenLarge, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Grande Multi", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuProductItem(name: String, price: Double, icon: ImageVector, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
            supportingContent = { Text("$${String.format("%.2f", price)}", color = MaterialTheme.colorScheme.primary) },
            leadingContent = {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            },
            trailingContent = {
                IconButton(onClick = onAdd, colors = IconButtonDefaults.filledIconButtonColors()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        )
    }
}

@Composable
private fun CartPizzaItem(
    item: CartItem,
    ingredientes: List<com.alos895.simplepos.model.Ingrediente>,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleGolden: () -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(item.sizeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text("$${String.format("%.2f", item.unitPriceSingle)}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onEditPrice) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
            }
            
            if (item.isCombo) {
                item.portions.forEach { Text("• ${it.fraction.label} ${it.pizzaName}", style = MaterialTheme.typography.labelSmall) }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = item.isGolden,
                    onClick = onToggleGolden,
                    label = { Text("Doradita", fontSize = 10.sp) },
                    leadingIcon = if (item.isGolden) { { Icon(Icons.Default.Check, null, Modifier.size(12.dp)) } } else null,
                    shape = RoundedCornerShape(8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.RemoveCircleOutline, null) }
                    Text("${item.cantidad}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun CartGenericItem(item: com.alos895.simplepos.model.CartItemPostre, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.postreOrExtra.nombre, fontWeight = FontWeight.Bold)
                Text("$${String.format("%.2f", item.postreOrExtra.precio)} c/u", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.RemoveCircleOutline, null) }
                Text("${item.cantidad}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDataSection(
    nombre: String, onNombreChange: (String) -> Unit,
    deliveryAddress: String, onAddressChange: (String) -> Unit,
    selectedDelivery: com.alos895.simplepos.model.DeliveryService?,
    deliveryOptions: List<com.alos895.simplepos.model.DeliveryService>,
    onDeliverySelect: (com.alos895.simplepos.model.DeliveryService) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nombre, onValueChange = onNombreChange,
            label = { Text("Nombre del Cliente") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) }
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedDelivery?.let { "${it.zona} ($${it.price})" } ?: "Seleccionar Entrega",
                onValueChange = {}, readOnly = true,
                label = { Text("Método de Entrega") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                deliveryOptions.forEach { option ->
                    DropdownMenuItem(text = { Text("${option.zona} - $${option.price}") }, onClick = { onDeliverySelect(option); expanded = false })
                }
            }
        }

        if (selectedDelivery?.type == DeliveryType.DOMICILIO || selectedDelivery?.type == DeliveryType.CAMINANDO) {
            OutlinedTextField(
                value = deliveryAddress, onValueChange = onAddressChange,
                label = { Text("Dirección de Entrega") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )
        }
    }
}

@Composable
private fun CommentsCard(current: String, onValueChange: (String) -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Notas de la Orden", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = current, onValueChange = onValueChange,
                placeholder = { Text("Ej: Sin cebolla, tocar timbre fuerte...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun OrderFooter(total: Double, totalItems: Int, onSave: () -> Unit, enabled: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total a Pagar", style = MaterialTheme.typography.labelSmall)
                Text("$${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("$totalItems productos", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = onSave,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("CONFIRMAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(label: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text("No hay $label disponibles", color = MaterialTheme.colorScheme.outline)
    }
}

private enum class MenuSection(val label: String, val icon: ImageVector) {
    PIZZAS("Pizzas", Icons.Default.LocalPizza),
    PIZZAS_COMBINADAS("Combinadas", Icons.Default.PieChart),
    COMBOS("Combos", Icons.Default.Fastfood),
    BEBIDAS("Bebidas", Icons.Default.LocalDrink),
    POSTRES("Postres", Icons.Default.Icecream),
    EXTRAS("Extras", Icons.Default.AddCircle),
    COMENTARIOS("Notas", Icons.Default.Comment)
}

private data class ComboDialogConfig(val title: String, val sizeName: String, val patterns: List<FractionPattern>)
