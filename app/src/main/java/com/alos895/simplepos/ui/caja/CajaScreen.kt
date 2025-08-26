package com.alos895.simplepos.ui.caja

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Import added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import added
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import com.alos895.simplepos.model.Transaction
import com.alos895.simplepos.model.TransactionType
import java.text.SimpleDateFormat // Import added
import java.util.Date
import java.util.Locale // Import added
import kotlinx.coroutines.launch

@Composable
fun CajaScreen(
    orderViewModel: OrderViewModel,
    bluetoothPrinterViewModel: BluetoothPrinterViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val selectedDate by orderViewModel.selectedDate.collectAsState()
    val dailyStats = orderViewModel.getDailyStats(selectedDate)

    var concept by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(TransactionType.INCOME) }
    val transactions = remember { mutableStateListOf<Transaction>() }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("CAJA", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pizzas", style = MaterialTheme.typography.titleMedium)
                    Text("Chicas: ${dailyStats.pizzasChicas}")
                    Text("Medianas: ${dailyStats.pizzasMedianas}")
                    Text("Grandes: ${dailyStats.pizzasGrandes}")
                    Text("Total: ${dailyStats.pizzas}")
                }
                Column {
                    Text("Postres y Extras", style = MaterialTheme.typography.titleMedium)
                    Text("Postres: ${dailyStats.postres}")
                    Text("Extras: ${dailyStats.extras}")
                }
                Column {
                    Text("Órdenes y Envíos", style = MaterialTheme.typography.titleMedium)
                    Text("Órdenes: ${dailyStats.ordenes}")
                    Text("Envíos: ${dailyStats.envios}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Ingresos", style = MaterialTheme.typography.titleMedium)
                    Text("Pizzas: $${"%.2f".format(dailyStats.ingresosPizzas)}")
                    Text("Postres: $${"%.2f".format(dailyStats.ingresosPostres)}")
                    Text("Extras: $${"%.2f".format(dailyStats.ingresosExtras)}")
                    Text("Envíos: $${"%.2f".format(dailyStats.ingresosEnvios)}")
                }
                Column {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ingresos Totales: $${"%.2f".format(dailyStats.ingresos)}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        orderViewModel.loadOrders() // Refrescar datos antes de imprimir
                        val refreshedStats = orderViewModel.getDailyStats(selectedDate)
                        val cajaReport = orderViewModel.buildCajaReport(refreshedStats)
                        bluetoothPrinterViewModel.print(cajaReport) { success, message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimir CAJA")
            }

            // Formulario para agregar transacciones
            Spacer(modifier = Modifier.height(16.dp))
            Text("Agregar Transacción", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = concept,
                onValueChange = { concept = it },
                label = { Text("Concepto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("Total") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly // Changed for better spacing
            ) {
                Button(
                    onClick = { transactionType = TransactionType.INCOME },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transactionType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Ingreso")
                }
                Button(
                    onClick = { transactionType = TransactionType.EXPENSE },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transactionType == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Gasto")
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // Added spacer
            Button(
                onClick = {
                    val totalDouble = total.toDoubleOrNull()
                    if (concept.isNotBlank() && totalDouble != null) {
                        transactions.add(
                            Transaction(
                                id = System.currentTimeMillis(),
                                date = Date(), // Current date for new transactions
                                concept = concept,
                                description = description,
                                total = totalDouble,
                                type = transactionType
                            )
                        )
                        // Clear fields
                        concept = ""
                        description = ""
                        total = ""
                        // Reset transaction type to INCOME or ask user for preference
                        transactionType = TransactionType.INCOME
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Concepto y Total son requeridos. Total debe ser un número.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Transacción")
            }

            // Mostrar transacciones
            Spacer(modifier = Modifier.height(16.dp))
            Text("Transacciones Registradas", style = MaterialTheme.typography.titleMedium) // Changed title
            LazyColumn(
                modifier = Modifier.weight(1f) // Added weight to fill available space
            ) {
                items(transactions.sortedByDescending { it.date }) { transaction -> // Iterate and sort by date
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(transaction.concept, style = MaterialTheme.typography.titleSmall)
                                if (transaction.description.isNotBlank()) {
                                    Text(transaction.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(dateFormat.format(transaction.date), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(
                                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else ""}$${"%.2f".format(transaction.total)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
