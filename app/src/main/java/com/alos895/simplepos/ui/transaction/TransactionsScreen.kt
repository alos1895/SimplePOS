package com.alos895.simplepos.ui.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.TransactionType
import com.alos895.simplepos.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: TransactionViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var transactionConcept by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }
    val transactionTypes = remember { TransactionType.values().toList() } // Using .toList() as per previous correct versions
    var selectedTransactionType by remember { mutableStateOf(transactionTypes.firstOrNull() ?: TransactionType.INGRESO) } // Ensure a default
    var expandedTransactionType by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionIdToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading && transactions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Columna Izquierda: Formulario para Nueva Transacción
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nueva Transacción Manual", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = transactionConcept,
                                    onValueChange = { transactionConcept = it },
                                    label = { Text("Concepto") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = transactionAmount,
                                    onValueChange = { transactionAmount = it },
                                    label = { Text("Monto") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ExposedDropdownMenuBox(
                                    expanded = expandedTransactionType,
                                    onExpandedChange = { expandedTransactionType = !expandedTransactionType },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedTransactionType.name,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo de Transacción") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTransactionType) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedTransactionType,
                                        onDismissRequest = { expandedTransactionType = false }
                                    ) {
                                        transactionTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.name) },
                                                onClick = {
                                                    selectedTransactionType = type
                                                    expandedTransactionType = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val amount = transactionAmount.toDoubleOrNull()
                                        if (transactionConcept.isNotBlank() && amount != null && amount > 0) {
                                            viewModel.addTransaction(
                                                concept = transactionConcept,
                                                amount = amount,
                                                type = selectedTransactionType
                                            )
                                            transactionConcept = ""
                                            transactionAmount = ""
                                            // Reset to the first type or a sensible default
                                            selectedTransactionType = transactionTypes.firstOrNull() ?: TransactionType.INGRESO
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Transacción guardada")
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Por favor, complete todos los campos correctamente.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Guardar Transacción")
                                }
                            }
                        }
                    }

                    // Columna Derecha: Historial de Transacciones
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text("Historial de Transacciones", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        error?.let {
                            Text("Error: $it", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.clearError() }) {
                                Text("Intentar de nuevo")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (transactions.isEmpty() && error == null && !isLoading) {
                            Text("No hay transacciones registradas.")
                        } else if (transactions.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(transactions) { transaction ->
                                    TransactionItem(
                                        transaction = transaction,
                                        dateFormatter = dateFormatter,
                                        onDeleteClicked = { id ->
                                            transactionIdToDelete = id
                                            showDeleteDialog = true
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }

                        if (isLoading && transactions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    transactionIdToDelete = null
                },
                title = { Text("Confirmar Borrado") },
                text = { Text("¿Estás seguro de que deseas eliminar esta transacción?") },
                confirmButton = {
                    Button(
                        onClick = {
                            transactionIdToDelete?.let { id ->
                                viewModel.deleteTransaction(id)
                                coroutineScope.launch { // <-- AÑADIDO AQUÍ
                                    snackbarHostState.showSnackbar("Transacción eliminada")
                                }
                            }
                            showDeleteDialog = false
                            transactionIdToDelete = null
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        transactionIdToDelete = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    dateFormatter: SimpleDateFormat,
    onDeleteClicked: (Long) -> Unit // Parámetro para manejar el clic de borrado
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ID: ${transaction.id} - Tipo: ${transaction.type.name}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Monto: ${"%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Fecha: ${dateFormatter.format(Date(transaction.date))}",
                style = MaterialTheme.typography.bodySmall
            )
            transaction.concept?.let {
                Text(
                    text = "Concepto: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // Botón de eliminar
        IconButton(onClick = {
            transaction.id?.let { // Asegurarse de que el ID no es null
                onDeleteClicked(it.toLong())
            }
        }) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar transacción")
        }
    }
}
