package com.alos895.simplepos.ui.caja

import android.app.DatePickerDialog // Importado
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Importado
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import com.alos895.simplepos.viewmodel.CajaViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun CajaScreen(
    cajaViewModel: CajaViewModel,
    bluetoothPrinterViewModel: BluetoothPrinterViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val dailyStats by cajaViewModel.dailyStats.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val selectedDate by cajaViewModel.selectedDate.collectAsState() // This is a State<Date>
    val context = LocalContext.current

    // LaunchedEffect to observe selectedDate changes from ViewModel is no longer needed here,
    // as CajaViewModel's setSelectedDate internally triggers data loading.
    // The initial load happens in CajaViewModel's init block.

    val calendar = Calendar.getInstance()
    // selectedDate is State<Date>, so access its value. It's not nullable in CajaViewModel.
    calendar.time = selectedDate 

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCal = Calendar.getInstance()
            pickedCal.set(year, month, dayOfMonth, 0, 0, 0)
            cajaViewModel.setSelectedDate(pickedCal.time)
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

            // Selector de Fecha y Botón de Refresco
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    // selectedDate.value is Date, not nullable.
                    text = "Mostrando datos de: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Elegir día")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { cajaViewModel.setSelectedDate(CajaViewModel.getToday()) }) { // Corrected
                        Text("Hoy")
                    }
                    Spacer(modifier = Modifier.width(8.dp)) // Espacio antes del nuevo botón
                    Button(onClick = { cajaViewModel.refreshCajaData() }) { // Corrected
                        Text("Refrescar Datos")
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
                    Text("Gastos e Ingresos capturados", style = MaterialTheme.typography.titleMedium)
                    Text("Gastos: $${"%.2f".format(dailyStats.egresosCapturados)}")
                    Text("Ingresos: $${"%.2f".format(dailyStats.ingresosCapturados)}")
                }
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
                        "Total efectivo: $${"%.2f".format(dailyStats.totalOrdenesEfectivo)}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Total transferencias: $${"%.2f".format(dailyStats.totalOrdenesTarjeta)}")
                    Text(
                        "TOTAL EN CAJA: $${"%.2f".format(dailyStats.totalCaja)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        // dailyStats is already a State, so access its value
                        val cajaReport = cajaViewModel.buildCajaReport(dailyStats)
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
