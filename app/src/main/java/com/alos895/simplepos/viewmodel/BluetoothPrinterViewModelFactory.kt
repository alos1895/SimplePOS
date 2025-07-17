package com.alos895.simplepos.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BluetoothPrinterViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothPrinterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothPrinterViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}