package com.alos895.simplepos.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.viewmodel.MenuViewModel
import com.alos895.simplepos.viewmodel.CartViewModel
import com.alos895.simplepos.ui.cart.CartScreen

@Composable
fun MenuScreen() {
    var showCart by remember { mutableStateOf(false) }
    val menuViewModel: MenuViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val pizzas by menuViewModel.pizzas.collectAsState()

    if (showCart) {
        CartScreen(onFinishOrder = { showCart = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Menú de Pizzas", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pizzas) { pizza ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = pizza.nombre, style = MaterialTheme.typography.titleLarge)
                            Text(text = "Tamaño: ${pizza.tamano}")
                            Text(text = "Precio base: $${"%.2f".format(pizza.precioBase)}")
                            Text(text = "Ingredientes: ${pizza.ingredientes.joinToString { it.nombre }}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { cartViewModel.addToCart(pizza) }) {
                                Text("Agregar al carrito")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showCart = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver carrito")
            }
        }
    }
} 