package com.alos895.simplepos.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alos895.simplepos.R

class BluetoothPrinterForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        // Aquí puedes iniciar la conexión Bluetooth y escuchar comandos
    }

    override fun onDestroy() {
        super.onDestroy()
        // Aquí puedes cerrar la conexión Bluetooth
    }

    private fun createNotification(): Notification {
        val channelId = "bluetooth_printer_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Estado de impresora Bluetooth",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("POS - Impresora Bluetooth")
            .setContentText("Conexión activa con la impresora")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
} 