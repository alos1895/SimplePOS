package com.alos895.simplepos.ui.caja

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Import added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import added
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
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

    // Cargar órdenes cuando la pantalla se muestra por primera vez
    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }

    // Observar los cambios en las órdenes y recalcular dailyStats
    val orders by orderViewModel.orders.collectAsState()
    val dailyStats = remember(orders, selectedDate) { // Recalcular si orders o selectedDate cambian
        orderViewModel.getDailyStats(selectedDate)
    }

    var concept by remember { mutableStateOf("") }
    // ... (resto del código igual)

    var description by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
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

            // TODO Formulario para agregar transacciones

        }
    }
}
