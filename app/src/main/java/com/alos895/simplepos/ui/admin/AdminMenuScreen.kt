package com.alos895.simplepos.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Tapas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.data.repository.AdminPizza
import com.alos895.simplepos.data.repository.PizzaUpsert
import com.alos895.simplepos.model.ExtraType
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.PostreOrExtra
import com.alos895.simplepos.model.TamanoPizza
import androidx.compose.runtime.collectAsState

private enum class AdminSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PIZZAS("Pizzas", Icons.Filled.LocalPizza),
    INGREDIENTES("Ingredientes", Icons.Filled.MenuBook),
    POSTRES("Postres", Icons.Filled.SoupKitchen),
    EXTRAS("Extras", Icons.Filled.Tapas),
    COMBOS("Combos", Icons.Filled.Fastfood)
}

private data class SizeEditorState(
    var name: String,
    var price: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(viewModel: AdminMenuViewModel = viewModel()) {
    val ingredientes by viewModel.ingredientes.collectAsState()
    val pizzas by viewModel.pizzas.collectAsState()
    val postres by viewModel.postres.collectAsState()
    val extras by viewModel.extras.collectAsState()
    val combos by viewModel.combos.collectAsState()

    var selectedSection by remember { mutableStateOf(AdminSection.PIZZAS) }
    var ingredientEditor by remember { mutableStateOf<Ingrediente?>(null) }
    var extraEditor by remember { mutableStateOf<PostreOrExtra?>(null) }
    var pizzaEditor by remember { mutableStateOf<AdminPizza?>(null) }
    var extraTypeEditor by remember { mutableStateOf(ExtraType.POSTRE) }

    val sections = remember { AdminSection.values().toList() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedSection) {
                        AdminSection.INGREDIENTES -> ingredientEditor = Ingrediente(0, "", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0)
                        AdminSection.PIZZAS -> pizzaEditor = AdminPizza(0, "", emptyList(), emptyList(), true)
                        AdminSection.POSTRES -> {
                            extraTypeEditor = ExtraType.POSTRE
                            extraEditor = PostreOrExtra(0, "", 0.0, esPostre = true)
                        }
                        AdminSection.EXTRAS -> {
                            extraTypeEditor = ExtraType.EXTRA
                            extraEditor = PostreOrExtra(0, "", 0.0, esPostre = false)
                        }
                        AdminSection.COMBOS -> {
                            extraTypeEditor = ExtraType.COMBO
                            extraEditor = PostreOrExtra(0, "", 0.0, esPostre = false, esCombo = true)
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Administración del menú", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                sections.forEachIndexed { index, section ->
                    SegmentedButton(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        shape = SegmentedButtonDefaults.itemShape(index, sections.size),
                        icon = { Icon(section.icon, contentDescription = null) },
                        label = { Text(section.label) }
                    )
                }
            }

            when (selectedSection) {
                AdminSection.PIZZAS -> PizzaList(
                    pizzas = pizzas,
                    onEdit = { pizzaEditor = it },
                    onDelete = { viewModel.deletePizza(it) }
                )
                AdminSection.INGREDIENTES -> IngredientList(
                    ingredientes = ingredientes,
                    onEdit = { ingredientEditor = it },
                    onDelete = { viewModel.deleteIngredient(it) }
                )
                AdminSection.POSTRES -> ExtraList(
                    title = "Postres",
                    extras = postres,
                    onEdit = {
                        extraTypeEditor = ExtraType.POSTRE
                        extraEditor = it
                    },
                    onDelete = { viewModel.deleteExtra(it, ExtraType.POSTRE) }
                )
                AdminSection.EXTRAS -> ExtraList(
                    title = "Extras",
                    extras = extras,
                    onEdit = {
                        extraTypeEditor = ExtraType.EXTRA
                        extraEditor = it
                    },
                    onDelete = { viewModel.deleteExtra(it, ExtraType.EXTRA) }
                )
                AdminSection.COMBOS -> ExtraList(
                    title = "Combos",
                    extras = combos,
                    onEdit = {
                        extraTypeEditor = ExtraType.COMBO
                        extraEditor = it
                    },
                    onDelete = { viewModel.deleteExtra(it, ExtraType.COMBO) }
                )
            }
        }
    }

    ingredientEditor?.let { ingrediente ->
        IngredientDialog(
            ingrediente = ingrediente,
            onSave = { viewModel.saveIngredient(it) },
            onDismiss = { ingredientEditor = null }
        )
    }

    extraEditor?.let { extra ->
        ExtraDialog(
            extra = extra,
            type = extraTypeEditor,
            onSave = { viewModel.saveExtra(it, extraTypeEditor) },
            onDismiss = { extraEditor = null }
        )
    }

    pizzaEditor?.let { pizza ->
        PizzaDialog(
            pizza = pizza,
            ingredientes = ingredientes,
            onSave = { viewModel.savePizza(it) },
            onDismiss = { pizzaEditor = null }
        )
    }
}

@Composable
private fun IngredientList(
    ingredientes: List<Ingrediente>,
    onEdit: (Ingrediente) -> Unit,
    onDelete: (Ingrediente) -> Unit
) {
    if (ingredientes.isEmpty()) {
        Text("No hay ingredientes registrados.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ingredientes, key = { it.id }) { ingrediente ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ingrediente.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Extra chica: ${ingrediente.preciExtraChica} | Mediana: ${ingrediente.precioExtraMediana} | Grande: ${ingrediente.precioExtraGrande}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        IconButton(onClick = { onEdit(ingrediente) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(ingrediente) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraList(
    title: String,
    extras: List<PostreOrExtra>,
    onEdit: (PostreOrExtra) -> Unit,
    onDelete: (PostreOrExtra) -> Unit
) {
    if (extras.isEmpty()) {
        Text("No hay $title registrados.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(extras, key = { it.id }) { extra ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(extra.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("Precio: ${extra.precio}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { onEdit(extra) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(extra) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PizzaList(
    pizzas: List<AdminPizza>,
    onEdit: (AdminPizza) -> Unit,
    onDelete: (AdminPizza) -> Unit
) {
    if (pizzas.isEmpty()) {
        Text("No hay pizzas registradas.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pizzas, key = { it.id }) { pizza ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pizza.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ingredientes: ${pizza.ingredientesBaseIds.size} | Tamaños: ${pizza.tamanos.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        IconButton(onClick = { onEdit(pizza) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(pizza) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientDialog(
    ingrediente: Ingrediente,
    onSave: (Ingrediente) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember(ingrediente) { mutableStateOf(ingrediente.nombre) }
    var chica by remember(ingrediente) { mutableStateOf(ingrediente.preciExtraChica.toString()) }
    var mediana by remember(ingrediente) { mutableStateOf(ingrediente.precioExtraMediana.toString()) }
    var grande by remember(ingrediente) { mutableStateOf(ingrediente.precioExtraGrande.toString()) }

    val parsedChica = chica.toDoubleOrNull()
    val parsedMediana = mediana.toDoubleOrNull()
    val parsedGrande = grande.toDoubleOrNull()
    val canSave = nombre.isNotBlank() && parsedChica != null && parsedMediana != null && parsedGrande != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ingrediente.id == 0) "Nuevo ingrediente" else "Editar ingrediente") },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ingrediente.copy(
                            nombre = nombre.trim(),
                            preciExtraChica = parsedChica ?: 0.0,
                            precioExtraMediana = parsedMediana ?: 0.0,
                            precioExtraGrande = parsedGrande ?: 0.0
                        )
                    )
                    onDismiss()
                },
                enabled = canSave
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chica,
                    onValueChange = { chica = it },
                    label = { Text("Precio extra chica") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mediana,
                    onValueChange = { mediana = it },
                    label = { Text("Precio extra mediana") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grande,
                    onValueChange = { grande = it },
                    label = { Text("Precio extra grande") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun ExtraDialog(
    extra: PostreOrExtra,
    type: ExtraType,
    onSave: (PostreOrExtra) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember(extra) { mutableStateOf(extra.nombre) }
    var precio by remember(extra) { mutableStateOf(extra.precio.toString()) }

    val parsedPrecio = precio.toDoubleOrNull()
    val canSave = nombre.isNotBlank() && parsedPrecio != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (type) {
                    ExtraType.POSTRE -> if (extra.id == 0) "Nuevo postre" else "Editar postre"
                    ExtraType.EXTRA -> if (extra.id == 0) "Nuevo extra" else "Editar extra"
                    ExtraType.COMBO -> if (extra.id == 0) "Nuevo combo" else "Editar combo"
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        extra.copy(
                            nombre = nombre.trim(),
                            precio = parsedPrecio ?: 0.0
                        )
                    )
                    onDismiss()
                },
                enabled = canSave
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun PizzaDialog(
    pizza: AdminPizza,
    ingredientes: List<Ingrediente>,
    onSave: (PizzaUpsert) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember(pizza) { mutableStateOf(pizza.nombre) }
    var esCombinable by remember(pizza) { mutableStateOf(pizza.esCombinable) }
    val selectedIngredients = remember(pizza) { mutableStateListOf<Int>().apply { addAll(pizza.ingredientesBaseIds) } }
    val sizeStates = remember(pizza) {
        mutableStateListOf<SizeEditorState>().apply {
            if (pizza.tamanos.isNotEmpty()) {
                pizza.tamanos.forEach { size ->
                    add(SizeEditorState(size.nombre, size.precioBase.toString()))
                }
            } else {
                add(SizeEditorState("", ""))
            }
        }
    }
    val scrollState = rememberScrollState()

    val canSave = nombre.isNotBlank() && sizeStates.all { it.name.isNotBlank() && it.price.toDoubleOrNull() != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pizza.id == 0L) "Nueva pizza" else "Editar pizza") },
        confirmButton = {
            Button(
                onClick = {
                    val sizes = sizeStates.mapNotNull { size ->
                        val parsedPrice = size.price.toDoubleOrNull() ?: return@mapNotNull null
                        TamanoPizza(size.name.trim(), parsedPrice)
                    }
                    onSave(
                        PizzaUpsert(
                            id = pizza.id.takeIf { it != 0L },
                            nombre = nombre.trim(),
                            ingredientesBaseIds = selectedIngredients.toList(),
                            tamanos = sizes,
                            esCombinable = esCombinable
                        )
                    )
                    onDismiss()
                },
                enabled = canSave
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la pizza") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = esCombinable, onCheckedChange = { esCombinable = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permite combinaciones")
                }
                Text("Ingredientes base", style = MaterialTheme.typography.titleSmall)
                if (ingredientes.isEmpty()) {
                    Text("No hay ingredientes disponibles para seleccionar.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ingredientes.forEach { ingrediente ->
                            val checked = ingrediente.id in selectedIngredients
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Switch(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedIngredients.add(ingrediente.id)
                                        } else {
                                            selectedIngredients.remove(ingrediente.id)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ingrediente.nombre)
                            }
                        }
                    }
                }
                Text("Tamaños y precios", style = MaterialTheme.typography.titleSmall)
                sizeStates.forEachIndexed { index, size ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = size.name,
                            onValueChange = { sizeStates[index] = size.copy(name = it) },
                            label = { Text("Tamaño") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = size.price,
                            onValueChange = { sizeStates[index] = size.copy(price = it) },
                            label = { Text("Precio") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (sizeStates.size > 1) {
                                    sizeStates.removeAt(index)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Quitar tamaño")
                        }
                    }
                }
                Button(
                    onClick = { sizeStates.add(SizeEditorState("", "")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar tamaño")
                }
            }
        }
    )
}
