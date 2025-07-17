package com.alos895.simplepos.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
fun BluetoothPrinterScreenMVVM(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    pairedDevices: List<BluetoothDevice>,
    onLoadPairedDevices: () -> Unit,
    selectedDevice: BluetoothDevice?,
    onSelectDevice: (BluetoothDevice) -> Unit,
    isPrinting: Boolean,
    onPrint: () -> Unit,
    message: String
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bluetooth Printer Demo") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!hasPermissions) {
                    Text("Se requieren permisos de Bluetooth.")
                    Button(onClick = onRequestPermissions) {
                        Text("Conceder permisos")
                    }
                    return@Column
                }

                Button(onClick = onLoadPairedDevices) {
                    Text("Mostrar dispositivos emparejados")
                }

                if (pairedDevices.isNotEmpty()) {
                    Text("Selecciona un dispositivo:")
                    pairedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDevice(device) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(device.name ?: "Desconocido")
                            if (selectedDevice == device) {
                                Text("(Seleccionado)")
                            }
                        }
                    }
                }

                Button(
                    onClick = onPrint,
                    enabled = selectedDevice != null && !isPrinting
                ) {
                    Text(if (isPrinting) "Imprimiendo..." else "Conectar e Imprimir")
                }

                if (message.isNotEmpty()) {
                    Text(message)
                }
            }
        }
    )
} 