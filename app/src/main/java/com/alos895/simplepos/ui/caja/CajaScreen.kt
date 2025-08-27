package com.alos895.simplepos.ui.caja

import android.app.DatePickerDialog // Importado
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Importado
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.OrderViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import java.text.SimpleDateFormat
import java.util.Calendar // Importado
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CajaScreen(
    orderViewModel: OrderViewModel,
    bluetoothPrinterViewModel: BluetoothPrinterViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val selectedDate by orderViewModel.selectedDate.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(selectedDate) {
        orderViewModel.loadOrders()
    }

    val orders by orderViewModel.orders.collectAsState()
    val dailyStats = remember(orders, selectedDate) {
        orderViewModel.getDailyStats(selectedDate)
    }

    val calendar = Calendar.getInstance()
    selectedDate?.let { calendar.time = it }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCal = Calendar.getInstance()
            pickedCal.set(year, month, dayOfMonth, 0, 0, 0)
            orderViewModel.setSelectedDate(pickedCal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
            Spacer(modifier = Modifier.height(8.dp))

            // Selector de Fecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedDate?.let {
                        "Mostrando datos de: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)}"
                    } ?: "Mostrando datos de hoy",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Elegir día")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { orderViewModel.setSelectedDate(OrderViewModel.getToday()) }) {
                        Text("Hoy")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

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
        }
    }
}
