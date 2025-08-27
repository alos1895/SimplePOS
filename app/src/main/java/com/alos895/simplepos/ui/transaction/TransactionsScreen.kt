package com.alos895.simplepos.ui.transaction

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
import com.alos895.simplepos.db.entity.CashTransactionEntity
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
    val transactionTypes = remember { TransactionType.values().toList() }
    var selectedTransactionType by remember { mutableStateOf(transactionTypes[0]) }
    var expandedTransactionType by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp) // Padding general para la pantalla
        ) {
            if (isLoading && transactions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Row( // Fila principal para dividir izquierda y derecha
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre las dos columnas
                ) {
                    // Columna Izquierda: Formulario para Nueva Transacción
                    Column(
                        modifier = Modifier
                            .weight(1f) // Ocupa la mitad del espacio
                            .fillMaxHeight() // Ocupa toda la altura disponible
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(), // El Card ocupa todo el ancho de esta columna
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nueva Transacción Manual", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp)) // Aumentado espacio
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
                                            selectedTransactionType = transactionTypes[0]
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
                            .weight(1f) // Ocupa la otra mitad del espacio
                            .fillMaxHeight() // Ocupa toda la altura disponible
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
                            LazyColumn(modifier = Modifier.weight(1f)) { // weight(1f) para que ocupe el espacio restante en esta columna
                                items(transactions) { transaction ->
                                    TransactionItem(transaction, dateFormatter)
                                    Divider()
                                }
                            }
                        }

                        if (isLoading && transactions.isNotEmpty()) { // Indicador de carga al final de la lista
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: CashTransactionEntity, dateFormatter: SimpleDateFormat) {
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
                    text = "Concepto: $it", // Cambiado "Descripción" a "Concepto" para consistencia
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}