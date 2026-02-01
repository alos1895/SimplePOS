package com.alos895.simplepos.ui.print

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.bluetooth.BluetoothPrinterForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterScreen(
    isConnected: Boolean,
    selectedDevice: BluetoothDevice?,
    pairedDevices: List<BluetoothDevice>,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onPrint: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    lastMessage: String,
    initialTicket: String,
    baseInventoryState: BaseInventoryUiState,
    onDateInputChange: (String) -> Unit,
    onLoadDate: () -> Unit,
    onBaseGrandesChange: (String) -> Unit,
    onBaseMedianasChange: (String) -> Unit,
    onBaseChicasChange: (String) -> Unit,
    onSaveBaseCounts: () -> Unit
) {
    var ticketText by remember { mutableStateOf(initialTicket) }
    var expanded by remember { mutableStateOf(false) }
    var serviceActive by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(lastMessage) {
        if (lastMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(lastMessage)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SnackbarHost(hostState = snackbarHostState)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Administración", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bases por día", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(baseInventoryState.dateLabel, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = baseInventoryState.dateInput,
                        onValueChange = onDateInputChange,
                        label = { Text("Fecha (dd/MM/yyyy)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onLoadDate) {
                        Text("Cargar")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = baseInventoryState.baseGrandesInput,
                        onValueChange = onBaseGrandesChange,
                        label = { Text("Grandes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = baseInventoryState.baseMedianasInput,
                        onValueChange = onBaseMedianasChange,
                        label = { Text("Medianas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = baseInventoryState.baseChicasInput,
                        onValueChange = onBaseChicasChange,
                        label = { Text("Chicas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Vendidas: Grandes ${baseInventoryState.soldGrandes}, Medianas ${baseInventoryState.soldMedianas}, Chicas ${baseInventoryState.soldChicas}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Disponibles: Grandes ${baseInventoryState.remainingGrandes}, Medianas ${baseInventoryState.remainingMedianas}, Chicas ${baseInventoryState.remainingChicas}",
                    style = MaterialTheme.typography.bodyMedium
                )
                baseInventoryState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSaveBaseCounts,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar bases")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Impresión", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Impresora Bluetooth", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color.Green else Color.Red)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedDevice?.name ?: "Seleccionar impresora",
                        color = if (isConnected) Color.Green else Color.Red,
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        pairedDevices.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.name ?: device.address) },
                                onClick = {
                                    onSelectDevice(device)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (!serviceActive) {
                                context.startForegroundService(Intent(context, BluetoothPrinterForegroundService::class.java))
                            } else {
                                context.stopService(Intent(context, BluetoothPrinterForegroundService::class.java))
                            }
                            serviceActive = !serviceActive
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serviceActive) Color.Green else Color.Gray
                        )
                    ) {
                        Text(if (serviceActive) "Desconectar impresora" else "Conectar impresora")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (serviceActive) "Servicio activo" else "Servicio detenido")
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = ticketText,
                    onValueChange = { ticketText = it },
                    label = { Text("Texto a imprimir") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isPrinting = true
                        onPrint(ticketText)
                        isPrinting = false
                    },
                    enabled = isConnected && !isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isPrinting) "Imprimiendo..." else "Imprimir ticket")
                }
            }
        }
    }
}
