package com.alos895.simplepos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alos895.simplepos.ui.menu.MenuScreen
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModel
import com.alos895.simplepos.ui.print.BluetoothPrinterViewModelFactory
import com.alos895.simplepos.ui.theme.SimplePOSTheme
import com.alos895.simplepos.ui.print.BluetoothPrinterScreen
import com.alos895.simplepos.ui.orders.OrderListScreen
import com.alos895.simplepos.ui.orders.OrderViewModel
import com.alos895.simplepos.ui.caja.CajaScreen
import com.alos895.simplepos.ui.transaction.TransactionsScreen
import com.alos895.simplepos.ui.caja.CajaViewModel
import com.alos895.simplepos.ui.print.PrintTicketViewModel
import com.alos895.simplepos.ui.transaction.TransactionViewModel
import com.alos895.simplepos.ui.admin.AdminScreen

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Menu : BottomNavItem("menu", Icons.Filled.RestaurantMenu, "Menú")
    object Orders : BottomNavItem("orders", Icons.Filled.ReceiptLong, "Órdenes")
    object Transactions : BottomNavItem("transactions", Icons.Filled.Paid, "Movimientos")
    object Caja : BottomNavItem("caja", Icons.Filled.PointOfSale, "Caja")
    object Print : BottomNavItem("print", Icons.Filled.Bluetooth, "Impresora")
    object Admin : BottomNavItem("admin", Icons.Filled.AdminPanelSettings, "Admin")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                    BottomNavItem.Orders,
                    BottomNavItem.Transactions,
                    BottomNavItem.Caja,
                    BottomNavItem.Print,
                    BottomNavItem.Admin
                )
                
                val bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel(factory = BluetoothPrinterViewModelFactory(application))
                
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // NAVEGACIÓN LATERAL (Navigation Rail) para Tablets
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                Icon(
                                    imageVector = Icons.Filled.LocalPizza,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp).padding(vertical = 12.dp)
                                )
                            },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            
                            Spacer(Modifier.weight(1f))
                            
                            items.forEach { item ->
                                NavigationRailItem(
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
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                    alwaysShowLabel = true
                                )
                            }
                            
                            Spacer(Modifier.weight(1f))
                        }

                        // CONTENIDO PRINCIPAL
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            NavHost(
                                navController = navController,
                                startDestination = BottomNavItem.Menu.route
                            ) {
                                composable(BottomNavItem.Menu.route) {
                                    MenuScreen(
                                        onPrintRequested = { navController.navigate(BottomNavItem.Print.route) }, 
                                        bluetoothPrinterViewModel = bluetoothPrinterViewModel
                                    )
                                }
                                composable(BottomNavItem.Orders.route) {
                                    val orderViewModel: OrderViewModel = viewModel()
                                    OrderListScreen(orderViewModel)
                                }
                                composable(BottomNavItem.Transactions.route) {
                                    val transactionViewModel: TransactionViewModel = viewModel()
                                    TransactionsScreen(transactionViewModel)
                                }
                                composable(BottomNavItem.Caja.route) {
                                    val cajaViewModel: CajaViewModel = viewModel()
                                    CajaScreen(cajaViewModel, bluetoothPrinterViewModel)
                                }
                                composable(BottomNavItem.Print.route) @androidx.annotation.RequiresPermission(
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                ) {
                                    val printTicketViewModel: PrintTicketViewModel = viewModel()
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
                                            bluetoothPrinterViewModel.print(ticketToPrint) { _, message ->
                                                lastMessage = message
                                            }
                                        },
                                        snackbarHostState = snackbarHostState,
                                        lastMessage = lastMessage,
                                        initialTicket = ticket
                                    )
                                }
                                composable(BottomNavItem.Admin.route) {
                                    AdminScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            // Manejar resultados de permisos si es necesario
        }
    }
}
