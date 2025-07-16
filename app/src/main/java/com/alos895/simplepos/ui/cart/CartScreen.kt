package com.alos895.simplepos.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.viewmodel.CartViewModel

@Composable
fun CartScreen(onFinishOrder: () -> Unit) {
    val viewModel: CartViewModel = viewModel()
    val cartItems by viewModel.cartItems.collectAsState()
    val total = cartItems.sumOf { it.subtotal }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            Text("Tamaño: ${item.pizza.tamano}")
                        }
                        Text("x${item.cantidad}")
                        Text("$${"%.2f".format(item.subtotal)}")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onFinishOrder, enabled = cartItems.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Finalizar e imprimir ticket")
        }
    }
} 