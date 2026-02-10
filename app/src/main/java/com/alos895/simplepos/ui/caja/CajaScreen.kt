package com.alos895.simplepos.ui.caja

import android.app.DatePickerDialog // Importado
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Importado
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
import com.alos895.simplepos.ui.caja.CajaViewModel
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

    val orders by cajaViewModel.ordersForDate.collectAsState()
    val transactions by cajaViewModel.transactionsForDate.collectAsState()

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
                    Button(onClick = { cajaViewModel.setSelectedDate(CajaViewModel.getToday()) }) {
                        Text("Refrescar Datos de Hoy")
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
                    Text("Postres, Combos, Bebidas y Extras", style = MaterialTheme.typography.titleMedium)
                    Text("Postres: ${dailyStats.postres}")
                    Text("Combos: ${dailyStats.combos}")
                    Text("Bebidas: ${dailyStats.bebidas}")
                    Text("Extras: ${dailyStats.extras}")
                }
                Column {
                    Text("Órdenes y Envíos", style = MaterialTheme.typography.titleMedium)
                    Text("Órdenes: ${dailyStats.ordenes}")
                    Text("Envíos: ${dailyStats.envios}")
                    Text("TOTODOS: ${dailyStats.deliverysTOTODO}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bloque 1: Órdenes y métodos de pago
                Column {
                    Text("RESUMEN ÓRDENES", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Órdenes totales: ${dailyStats.ordenes}")
                    Text("Órdenes no pagadas: $${"%.2f".format(dailyStats.ordenesNoPagadas)}")
                    Text("Órdenes efectivo: $${"%.2f".format(dailyStats.totalOrdenesEfectivo)}")
                    Text("Órdenes tarjeta: $${"%.2f".format(dailyStats.totalOrdenesTarjeta)}")
                    Text("Descuentos TOTODO: $${"%.2f".format(dailyStats.totalDescuentosTOTODO)}")
                }

                // Bloque 2: Ingresos por tipo de venta
                Column {
                    Text("INGRESOS POR VENTAS", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Pizzas: $${"%.2f".format(dailyStats.ingresosPizzas)}")
                    Text("Postres: $${"%.2f".format(dailyStats.ingresosPostres)}")
                    Text("Combos: $${"%.2f".format(dailyStats.ingresosCombos)}")
                    Text("Bebidas: $${"%.2f".format(dailyStats.ingresosBebidas)}")
                    Text("Extras: $${"%.2f".format(dailyStats.ingresosExtras)}")
                    Text("Envíos: $${"%.2f".format(dailyStats.ingresosEnvios)}")
                }

                // Bloque 3: Totales y movimientos manuales
                Column {
                    Text("TOTALES", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Ingresos manuales: $${"%.2f".format(dailyStats.ingresosCapturados)}")
                    Text("Gastos manuales: $${"%.2f".format(dailyStats.egresosCapturados)}")
                    //TODO: Mover este calculo al viewmodel
                    val totalEfectivoCaja = dailyStats.totalOrdenesEfectivo + dailyStats.ingresosCapturados - dailyStats.egresosCapturados - dailyStats.totalDescuentosTOTODO
                    Text(
                        "TOTAL EFECTIVO: $${"%.2f".format(totalEfectivoCaja)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "TOTAL EN CAJA: $${"%.2f".format(dailyStats.totalCaja)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // espacio entre botones
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val cajaReport = cajaViewModel.buildCajaReport(dailyStats)
                            bluetoothPrinterViewModel.print(cajaReport) { success, message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Imprimir CAJA")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val csv = cajaViewModel.generateCsvDetailed(orders, transactions)
                            val file = cajaViewModel.saveCsvToFile(context, csv)
                            cajaViewModel.shareCsvFile(context, file)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exportar CSV")
                }
            }
        }
    }
}
