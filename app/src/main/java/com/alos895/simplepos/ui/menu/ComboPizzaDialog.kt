package com.alos895.simplepos.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alos895.simplepos.model.CartItemPortion
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PizzaFractionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboPizzaDialog(
    title: String,
    sizeName: String,
    patterns: List<FractionPattern>,
    pizzas: List<Pizza>,
    onDismiss: () -> Unit,
    onConfirm: (List<CartItemPortion>) -> Unit
) {
    if (patterns.isEmpty()) return

    val combinablePizzas = remember(pizzas) { pizzas.filter { it.esCombinable } }
    val hasCombinables = combinablePizzas.isNotEmpty()

    var selectedPattern by remember { mutableStateOf(patterns.first()) }
    var selections by remember { mutableStateOf(List(selectedPattern.fractions.size) { null as String? }) }

    LaunchedEffect(selectedPattern) {
        selections = List(selectedPattern.fractions.size) { null }
    }

    val confirmEnabled = hasCombinables && selections.all { !it.isNullOrBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Selecciona la combinacion para la pizza $sizeName")
                Spacer(modifier = Modifier.height(12.dp))
                if (!hasCombinables) {
                    Text("No hay pizzas combinables disponibles en este momento.")
                } else {
                    patterns.forEach { pattern ->
                        val selected = pattern.id == selectedPattern.id
                        TextButton(
                            onClick = { selectedPattern = pattern },
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            RadioButton(selected = selected, onClick = { selectedPattern = pattern })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pattern.label)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    selectedPattern.fractions.forEachIndexed { index, fraction ->
                        var expanded by remember(selectedPattern.id, index) { mutableStateOf(false) }
                        val value = selections.getOrNull(index)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = value ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("${fraction.label} de la pizza") },
                                placeholder = { Text("Selecciona especialidad") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                combinablePizzas.forEach { pizza ->
                                    DropdownMenuItem(
                                        text = { Text(pizza.nombre) },
                                        onClick = {
                                            selections = selections.toMutableList().also { list ->
                                                list[index] = pizza.nombre
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val portions = selectedPattern.fractions.mapIndexed { idx, fraction ->
                    CartItemPortion(
                        pizzaName = selections[idx]!!.trim(),
                        fraction = fraction
                    )
                }
                onConfirm(portions)
                onDismiss()
            }, enabled = confirmEnabled) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class FractionPattern(
    val id: String,
    val label: String,
    val fractions: List<PizzaFractionType>
)
