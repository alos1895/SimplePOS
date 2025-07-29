package com.alos895.simplepos.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alos895.simplepos.viewmodel.UserViewModel
import com.alos895.simplepos.model.User

@Composable
fun ClientListScreen(userViewModel: UserViewModel = viewModel()) {
    val users by userViewModel.users.collectAsState()
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Clientes", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (nombre.isNotBlank() && telefono.isNotBlank()) {
                    userViewModel.addUser(User(nombre = nombre, telefono = telefono))
                    nombre = ""
                    telefono = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar cliente")
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn {
            items(users) { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(user.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(user.telefono, style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = { userViewModel.removeUser(user.id) }) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }
}

// Para ver la lista de clientes, navega a ClientListScreen desde tu actividad principal o desde el NavHost.
// Ejemplo de uso en tu NavHost:
// navController.navigate("clientes") // donde "clientes" es la ruta asociada a ClientListScreen

// Si quieres probar directamente, puedes llamar a ClientListScreen() en tu MainActivity o donde lo necesites:
// setContent { ClientListScreen() }

// La pantalla ClientListScreen ya muestra la lista de clientes y permite agregar/eliminar clientes.
