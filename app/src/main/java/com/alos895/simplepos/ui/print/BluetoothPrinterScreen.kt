package com.alos895.simplepos.ui.print

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    initialTicket: String
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Impresora Bluetooth", style = MaterialTheme.typography.titleLarge)
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