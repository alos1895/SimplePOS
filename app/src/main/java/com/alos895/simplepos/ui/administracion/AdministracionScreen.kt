package com.alos895.simplepos.ui.administracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministracionScreen(viewModel: AdministracionViewModel) {
    val totals by viewModel.totals.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var chicasInput by remember { mutableStateOf("") }
    var medianasInput by remember { mutableStateOf("") }
    var grandesInput by remember { mutableStateOf("") }
    var isSubtractMode by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Administración", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Agregar cantidad de bases hechas", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row {
                            RadioButton(
                                selected = !isSubtractMode,
                                onClick = { isSubtractMode = false }
                            )
                            Text("Sumar")
                        }
                        Row {
                            RadioButton(
                                selected = isSubtractMode,
                                onClick = { isSubtractMode = true }
                            )
                            Text("Restar")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chicasInput,
                        onValueChange = { chicasInput = it },
                        label = { Text("Chicas (número)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = medianasInput,
                        onValueChange = { medianasInput = it },
                        label = { Text("Medianas (número)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = grandesInput,
                        onValueChange = { grandesInput = it },
                        label = { Text("Grandes (número)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val chicas = chicasInput.toIntOrNull()
                            val medianas = medianasInput.toIntOrNull()
                            val grandes = grandesInput.toIntOrNull()
                            if (chicas != null && medianas != null && grandes != null) {
                                val multiplier = if (isSubtractMode) -1 else 1
                                viewModel.addProduction(
                                    chicas * multiplier,
                                    medianas * multiplier,
                                    grandes * multiplier
                                )
                                chicasInput = ""
                                medianasInput = ""
                                grandesInput = ""
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Movimiento guardado correctamente.")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ingresa valores numéricos válidos.")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSubtractMode) "Restar" else "Agregar")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Totales", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Chicas: ${totals.totalChicas}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Medianas: ${totals.totalMedianas}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Grandes: ${totals.totalGrandes}")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Historial", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (entries.isEmpty()) {
                        Text("No hay movimientos registrados.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(entries) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Fecha: ${dateFormatter.format(Date(entry.timestamp))}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Eliminar"
                                                )
                                            }
                                        }
                                        Text("Chicas: ${formatSigned(entry.chicas)}")
                                        Text("Medianas: ${formatSigned(entry.medianas)}")
                                        Text("Grandes: ${formatSigned(entry.grandes)}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSigned(value: Int): String {
    return if (value >= 0) "+$value" else value.toString()
}
