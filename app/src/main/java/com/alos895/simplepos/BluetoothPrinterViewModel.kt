package com.alos895.simplepos

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class BluetoothPrinterViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }.toTypedArray()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    fun checkPermissions() {
        _hasPermissions.value = permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadPairedDevices() {
        if (bluetoothAdapter == null) {
            _message.value = "Bluetooth not supported on this device."
            return
        }
        val devices = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
        _pairedDevices.value = devices
        if (devices.isEmpty()) {
            _message.value = "No paired Bluetooth devices found."
        } else {
            _message.value = ""
        }
    }

    fun selectDevice(device: BluetoothDevice) {
        _selectedDevice.value = device
    }

    fun centerTextForPrinter(text: String, lineWidth: Int = 32, reverseLines: Boolean = false): String {
        val lines = mutableListOf<String>()
        var currentLine = ""

        val words = text.split("\\s+".toRegex())
        for (word in words) {
            // Si la palabra es más larga que el ancho, la partimos
            if (word.length > lineWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }
                var start = 0
                while (start < word.length) {
                    val end = (start + lineWidth).coerceAtMost(word.length)
                    lines.add(word.substring(start, end))
                    start = end
                }
            } else if (currentLine.isEmpty()) {
                currentLine = word
            } else if (currentLine.length + 1 + word.length <= lineWidth) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        val processedLines = if (reverseLines) lines.reversed() else lines

        // Centrar cada línea
        return processedLines.joinToString("\n") { line ->
            val totalPadding = lineWidth - line.length
            val leftPadding = totalPadding / 2
            val rightPadding = totalPadding - leftPadding
            " ".repeat(leftPadding.coerceAtLeast(0)) + line + " ".repeat(rightPadding.coerceAtLeast(0))
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun printText(text: String, reverseLines: Boolean = false) {
        val device = _selectedDevice.value
        if (device == null) {
            _message.value = "Selecciona un dispositivo."
            return
        }
        _isPrinting.value = true
        _message.value = ""
        // Prepara el ticket con saltos de línea extra y formato Windows
        val ticket = text.trimEnd() + "\r\n\r\n\r\n"
        val lines = ticket.replace("\n", "\r\n").split("\r\n")
        viewModelScope.launch(Dispatchers.IO) {
            val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            try {
                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                val outputStream = socket.outputStream
                // Comando ESC/POS para cambiar charset a PC850 (Latino 1)
                val selectCharset = byteArrayOf(0x1B, 0x74, 0x02) // 0x02 = PC850
                outputStream.write(selectCharset)
                for (line in lines) {
                    outputStream.write((line + "\r\n").toByteArray(Charsets.ISO_8859_1))
                    outputStream.flush()
                }
                outputStream.close()
                socket.close()
                _message.value = "¡Impresión exitosa!"
            } catch (e: IOException) {
                _message.value = "Error al imprimir: ${e.localizedMessage}"
            } finally {
                _isPrinting.value = false
            }
        }
    }
} 