package com.alos895.simplepos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alos895.simplepos.ui.menu.MenuScreen
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModelFactory
import com.alos895.simplepos.ui.theme.SimplePOSTheme
import com.alos895.simplepos.ui.print.BluetoothPrinterScreen
import com.alos895.simplepos.ui.orders.OrderListScreen
import com.alos895.simplepos.viewmodel.OrderViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Menu : BottomNavItem("menu", Icons.Filled.Home, "Menú")
    object Print : BottomNavItem("print", Icons.Filled.Print, "Impresión")
    object Orders : BottomNavItem("orders", Icons.Filled.Home, "Órdenes") // Puedes cambiar el icono
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solicitar permisos Bluetooth en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
            }
        }
        setContent {
            SimplePOSTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomNavItem.Menu,
                    BottomNavItem.Print,
                    BottomNavItem.Orders
                )
                // ViewModel compartido a nivel de actividad
                val bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel(factory = BluetoothPrinterViewModelFactory(application))
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            items.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Menu.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(BottomNavItem.Menu.route) {
                            MenuScreen(onPrintRequested = { navController.navigate(BottomNavItem.Print.route) }, bluetoothPrinterViewModel = bluetoothPrinterViewModel)
                        }
                        composable(BottomNavItem.Print.route) @androidx.annotation.RequiresPermission(
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) {
                            val printTicketViewModel: com.alos895.simplepos.viewmodel.PrintTicketViewModel = viewModel()
                            val isConnected by bluetoothPrinterViewModel.isConnected.collectAsState(initial = false)
                            val selectedDevice by bluetoothPrinterViewModel.selectedDevice.collectAsState(initial = null)
                            val pairedDevices = bluetoothPrinterViewModel.pairedDevices
                            val snackbarHostState = remember { SnackbarHostState() }
                            var lastMessage by remember { mutableStateOf("") }
                            val ticket by printTicketViewModel.ticket.collectAsState()
                            BluetoothPrinterScreen(
                                isConnected = isConnected,
                                selectedDevice = selectedDevice,
                                pairedDevices = pairedDevices,
                                onSelectDevice = { bluetoothPrinterViewModel.selectDevice(it) },
                                onPrint = { ticketToPrint ->
                                    bluetoothPrinterViewModel.print(ticketToPrint) { success, message ->
                                        lastMessage = message
                                    }
                                },
                                snackbarHostState = snackbarHostState,
                                lastMessage = lastMessage,
                                initialTicket = ticket
                            )
                        }
                        composable(BottomNavItem.Orders.route) {
                            val orderViewModel: OrderViewModel = viewModel()
                            OrderListScreen(orderViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permisos concedidos
            } else {
                // Permisos denegados, puedes mostrar un mensaje o cerrar la funcionalidad
            }
        }
    }
}
