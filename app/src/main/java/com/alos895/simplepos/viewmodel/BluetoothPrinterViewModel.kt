package com.alos895.simplepos.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class BluetoothPrinterViewModel(application: Application) : AndroidViewModel(application) {
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

    fun loadPairedDevices() {
        if (bluetoothAdapter == null) {
            _message.value = "Bluetooth no soportado en este dispositivo."
            return
        }
        val devices = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
        _pairedDevices.value = devices
        if (devices.isEmpty()) {
            _message.value = "No hay dispositivos Bluetooth emparejados."
        } else {
            _message.value = ""
        }
    }

    fun selectDevice(device: BluetoothDevice) {
        _selectedDevice.value = device
    }

    fun printText(text: String) {
        val device = _selectedDevice.value
        if (device == null) {
            _message.value = "Selecciona un dispositivo."
            return
        }
        _isPrinting.value = true
        _message.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            try {
                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                val outputStream = socket.outputStream
                val selectCharset = byteArrayOf(0x1B, 0x74, 0x02) // PC850
                outputStream.write(selectCharset)
                outputStream.write(text.toByteArray(Charsets.ISO_8859_1))
                outputStream.flush()
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