package com.alos895.simplepos.ui.print

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.alos895.simplepos.bluetooth.BluetoothPrinterManager
import kotlinx.coroutines.flow.StateFlow

class BluetoothPrinterViewModel(application: Application) : AndroidViewModel(application) {
    init {
        BluetoothPrinterManager.init(application.applicationContext)
    }

    val isConnected: StateFlow<Boolean> = BluetoothPrinterManager.isConnected
    val selectedDevice: StateFlow<BluetoothDevice?> = BluetoothPrinterManager.selectedDevice
    val pairedDevices: List<BluetoothDevice> get() = BluetoothPrinterManager.getPairedDevices()

    fun selectDevice(device: BluetoothDevice) {
        BluetoothPrinterManager.selectDevice(device)
    }

    fun print(text: String, onResult: (Boolean, String) -> Unit) {
        BluetoothPrinterManager.print(text, onResult)
    }
}