package com.alos895.simplepos.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.db.entity.PizzaBaseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminOption {
    HOME,
    MENU,
    INVENTORY
}

@Composable
fun AdminScreen() {
    var selectedOption by remember { mutableStateOf(AdminOption.HOME) }

    when (selectedOption) {
        AdminOption.HOME -> AdminHome(
            onOpenMenuAdmin = { selectedOption = AdminOption.MENU },
            onOpenInventory = { selectedOption = AdminOption.INVENTORY }
        )

        AdminOption.MENU -> AdminMenuContainer(onBack = { selectedOption = AdminOption.HOME })
        AdminOption.INVENTORY -> InventoryScreen(onBack = { selectedOption = AdminOption.HOME })
    }
}

@Composable
private fun AdminHome(
    onOpenMenuAdmin: () -> Unit,
    onOpenInventory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Panel de administración",
            style = MaterialTheme.typography.titleLarge
        )

        AdminOptionCard(
            title = "Administración de menú",
            subtitle = "Abrir la vista actual para editar pizzas, ingredientes y extras.",
            onClick = onOpenMenuAdmin
        )

        AdminOptionCard(
            title = "Inventario",
            subtitle = "Registrar bases de pizza y controlar su uso.",
            onClick = onOpenInventory
        )
    }
}

@Composable
private fun AdminOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdminMenuContainer(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        BackButton(onBack = onBack)
        AdminMenuScreen()
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Regresar")
        Text(text = "Regresar")
    }
}

@Composable
private fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: AdminInventoryViewModel = viewModel()
) {
    val bases by viewModel.pizzaBases.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminInventoryEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AdminInventoryEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton(onBack = onBack)
            Text(
                text = "Inventario de bases",
                style = MaterialTheme.typography.titleLarge
            )

            AddPizzaBaseForm(onAdd = viewModel::addPizzaBase)

            Text(
                text = "Bases registradas",
                style = MaterialTheme.typography.titleMedium
            )

            if (bases.isEmpty()) {
                Text(
                    text = "No hay bases registradas todavía.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bases, key = { it.id }) { base ->
                        PizzaBaseItem(
                            base = base,
                            onMarkUsed = { viewModel.markAsUsed(base.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPizzaBaseForm(onAdd: (String) -> Unit) {
    val sizeOptions = listOf("chica", "mediana", "grande")
    var selectedSize by remember { mutableStateOf(sizeOptions.first()) }
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedSize,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tamaño de base") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    sizeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedSize = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { onAdd(selectedSize) }) {
                Text("Guardar base")
            }
        }
    }
}

@Composable
private fun PizzaBaseItem(
    base: PizzaBaseEntity,
    onMarkUsed: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Base ${base.size.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Creación: ${base.createdAt.toUiDate()}"
            )
            Text(
                text = "Uso: ${base.usedAt?.toUiDate() ?: "Pendiente"}"
            )

            if (base.usedAt == null) {
                Row {
                    Button(onClick = onMarkUsed) {
                        Text("Marcar como usada")
                    }
                }
            }
        }
    }
}

private fun Long.toUiDate(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}
