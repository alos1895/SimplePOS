package com.alos895.simplepos.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alos895.simplepos.ui.menu.MenuScreen
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModel
import com.alos895.simplepos.viewmodel.BluetoothPrinterViewModelFactory
import com.alos895.simplepos.ui.theme.SimplePOSTheme

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Menu : BottomNavItem("menu", Icons.Filled.Home, "Menú")
    object Print : BottomNavItem("print", Icons.Filled.Print, "Impresión")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimplePOSTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomNavItem.Menu,
                    BottomNavItem.Print
                )
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
                            MenuScreen(onPrintRequested = { navController.navigate(BottomNavItem.Print.route) })
                        }
                        composable(BottomNavItem.Print.route) {
                            val bluetoothPrinterViewModel: BluetoothPrinterViewModel = viewModel(factory = BluetoothPrinterViewModelFactory(application))
                            val hasPermissions by bluetoothPrinterViewModel.hasPermissions.collectAsState(initial = false)
                            val pairedDevices by bluetoothPrinterViewModel.pairedDevices.collectAsState(initial = emptyList())
                            val selectedDevice by bluetoothPrinterViewModel.selectedDevice.collectAsState(initial = null)
                            val isPrinting by bluetoothPrinterViewModel.isPrinting.collectAsState(initial = false)
                            val message by bluetoothPrinterViewModel.message.collectAsState(initial = "")
                            val context = LocalContext.current
                            val requestPermissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestMultiplePermissions()
                            ) { perms ->
                                bluetoothPrinterViewModel.checkPermissions()
                            }
                            BluetoothPrinterScreenMVVM(
                                hasPermissions = hasPermissions,
                                onRequestPermissions = { requestPermissionLauncher.launch(bluetoothPrinterViewModel.permissions) },
                                pairedDevices = pairedDevices,
                                onLoadPairedDevices = { bluetoothPrinterViewModel.loadPairedDevices() },
                                selectedDevice = selectedDevice,
                                onSelectDevice = { bluetoothPrinterViewModel.selectDevice(it) },
                                isPrinting = isPrinting,
                                onPrint = { bluetoothPrinterViewModel.printText("Ticket de prueba") },
                                message = message
                            )
                        }
                    }
                }
            }
        }
    }
} 