package com.alos895.simplepos.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

        AdminOption.MENU -> AdminMenuScreen()
        AdminOption.INVENTORY -> InventoryScreen()
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
            subtitle = "Nueva vista de inventario (en construcción).",
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
private fun InventoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Inventario",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Aquí trabajaremos la administración de inventario próximamente.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
