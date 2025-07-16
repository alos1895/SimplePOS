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

@Composable
fun MenuScreen() {
    val menuViewModel: MenuViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val pizzas by menuViewModel.pizzas.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val total = cartViewModel.total

    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Menú (izquierda)
        Column(
            modifier = Modifier.weight(1f).padding(8.dp)
        ) {
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
                            // Aquí podrías mostrar tamaños e ingredientes si lo deseas
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { cartViewModel.addToCart(pizza) }) {
                                Text("Agregar al carrito")
                            }
                        }
                    }
                }
            }
        }
        // Carrito (derecha)
        Column(
            modifier = Modifier.weight(1f).padding(8.dp)
        ) {
            Text("Carrito de compras", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.pizza.nombre)
                                // Aquí podrías mostrar tamaño si lo deseas
                            }
                            Text("x${item.cantidad}")
                            Text("$${"%.2f".format(item.subtotal)}")
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { cartViewModel.removeFromCart(item.pizza) }) {
                                Text("-")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Acción de finalizar e imprimir */ }, enabled = cartItems.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Finalizar e imprimir ticket")
            }
        }
    }
} 