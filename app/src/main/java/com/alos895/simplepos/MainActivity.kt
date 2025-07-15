package com.alos895.simplepos

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.ui.theme.SimplePOSTheme
import java.io.IOException
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimplePOSTheme {
                val viewModel: BluetoothPrinterViewModel = viewModel(
                    factory = BluetoothPrinterViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
                )
                val context = LocalContext.current
                val hasPermissions by viewModel.hasPermissions.collectAsState()
                val pairedDevices by viewModel.pairedDevices.collectAsState()
                val selectedDevice by viewModel.selectedDevice.collectAsState()
                val isPrinting by viewModel.isPrinting.collectAsState()
                val message by viewModel.message.collectAsState()

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    viewModel.checkPermissions()
                }

                LaunchedEffect(Unit) {
                    viewModel.checkPermissions()
                }

                val ticket = """
PIZZERIA LA ITALIANA
Ticket: 12345
MESA: 7
--------------------------------
1x Pizza Margarita   $120.00
2x Refresco         $ 40.00
1x Cafe             $ 30.00
--------------------------------
TOTAL:              $230.00
Gracias por su compra
2024-05-18 20:45
""".trimIndent().replace("\n", "\r\n")

                BluetoothPrinterScreenMVVM(
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        requestPermissionLauncher.launch(viewModel.permissions)
                    },
                    pairedDevices = pairedDevices,
                    onLoadPairedDevices = { viewModel.loadPairedDevices() },
                    selectedDevice = selectedDevice,
                    onSelectDevice = { viewModel.selectDevice(it) },
                    isPrinting = isPrinting,
                    onPrint = {
                        viewModel.printText(ticket)
                    },
                    message = message
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterScreenMVVM(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    pairedDevices: List<android.bluetooth.BluetoothDevice>,
    onLoadPairedDevices: () -> Unit,
    selectedDevice: android.bluetooth.BluetoothDevice?,
    onSelectDevice: (android.bluetooth.BluetoothDevice) -> Unit,
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
                    Text("Bluetooth permissions are required.")
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                    return@Column
                }

                Button(onClick = onLoadPairedDevices) {
                    Text("Show Paired Devices")
                }

                if (pairedDevices.isNotEmpty()) {
                    Text("Select a device:")
                    pairedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDevice(device) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(device.name ?: "Unknown")
                            if (selectedDevice == device) {
                                Text("(Selected)")
                            }
                        }
                    }
                }

                Button(
                    onClick = onPrint,
                    enabled = selectedDevice != null && !isPrinting
                ) {
                    Text(if (isPrinting) "Printing..." else "Connect & Print")
                }

                if (message.isNotEmpty()) {
                    Text(message)
                }
            }
        }
    )
}

fun sendTextToBluetoothPrinter(
    device: BluetoothDevice,
    text: String,
    onResult: (String) -> Unit
) {
    Thread {
        val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            val outputStream = socket.outputStream
            outputStream.write(text.toByteArray())
            outputStream.flush()
            outputStream.close()
            socket.close()
            onResult("Printed successfully!")
        } catch (e: IOException) {
            onResult("Failed to print: ${e.localizedMessage}")
        }
    }.start()
}

fun centerLine(line: String, lineWidth: Int = 32): String {
    val trimmed = line.trim()
    if (trimmed.length >= lineWidth) {
        return trimmed
    }
    val totalPadding = lineWidth - trimmed.length
    val leftPadding = totalPadding / 2
    val rightPadding = totalPadding - leftPadding
    return " ".repeat(leftPadding.coerceAtLeast(0)) + trimmed + " ".repeat(rightPadding.coerceAtLeast(0))
}