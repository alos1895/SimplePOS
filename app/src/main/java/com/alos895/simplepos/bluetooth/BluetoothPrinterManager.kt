package com.alos895.simplepos.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.*

object BluetoothPrinterManager {
    private const val PREFS_NAME = "printer_prefs"
    private const val KEY_DEVICE_ADDRESS = "device_address"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private lateinit var prefs: SharedPreferences
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectAttempts = 0

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreSelectedDevice()
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun selectDevice(device: BluetoothDevice) {
        this.device = device
        _selectedDevice.value = device
        prefs.edit().putString(KEY_DEVICE_ADDRESS, device.address).apply()
        connect()
    }

    private fun restoreSelectedDevice() {
        val address = prefs.getString(KEY_DEVICE_ADDRESS, null)
        if (address != null && bluetoothAdapter != null) {
            val dev = bluetoothAdapter.bondedDevices?.firstOrNull { it.address == address }
            if (dev != null) {
                device = dev
                _selectedDevice.value = dev
                connect()
            }
        }
    }

    fun connect() {
        device?.let { dev ->
            scope.launch {
                try {
                    val uuid = dev.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    socket = dev.createRfcommSocketToServiceRecord(uuid)
                    bluetoothAdapter?.cancelDiscovery()
                    socket?.connect()
                    _isConnected.value = true
                    reconnectAttempts = 0
                } catch (e: IOException) {
                    _isConnected.value = false
                    reconnect()
                }
            }
        }
    }

    private fun reconnect() {
        if (reconnectAttempts < 3) {
            reconnectAttempts++
            scope.launch {
                delay(3000)
                connect()
            }
        } else {
            _isConnected.value = false
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) {}
        _isConnected.value = false
    }

    fun print(text: String, onResult: (Boolean, String) -> Unit) {
        if (_isConnected.value && socket != null) {
            scope.launch {
                try {
                    val outputStream = socket!!.outputStream
                    outputStream.write(text.toByteArray(Charsets.ISO_8859_1))
                    outputStream.write("\n\n".toByteArray())
                    outputStream.flush()
                    onResult(true, "¡Impresión de prueba enviada con 4 charsets!")
                } catch (e: IOException) {
                    _isConnected.value = false
                    onResult(false, "Error al imprimir: ${e.localizedMessage}")
                    reconnect()
                }
            }
        } else {
            onResult(false, "Impresora no conectada")
            connect()
        }
    }
} 