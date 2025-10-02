package com.alos895.simplepos.ui.common

import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.sizeLabel
import java.util.Locale

object CartItemFormatter {

    private fun formatCurrency(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

    fun toCustomerLines(item: CartItem): List<String> {
        val lines = mutableListOf<String>()
        val size = item.sizeLabel.ifBlank { "" }
        val headerName = if (item.isCombo) {
            if (size.isBlank()) "Pizza combinada" else "Pizza $size combinada"
        } else {
            val baseName = item.pizza?.nombre ?: "Pizza"
            if (size.isBlank()) baseName else "$baseName $size"
        }
        lines.add("${item.cantidad}x $headerName   $${formatCurrency(item.subtotal)}")
        if (item.isGolden) {
            lines.add("   Doradita")
        }
        if (item.isCombo) {
            item.portions.forEach { portion ->
                lines.add("   ${portion.fraction.label} ${portion.pizzaName}")
            }
        }
        return lines
    }

    fun toKitchenLines(item: CartItem): List<String> {
        val lines = mutableListOf<String>()
        val size = item.sizeLabel.ifBlank { "" }
        val sizeUpper = if (size.isBlank()) size else size.uppercase(Locale.getDefault())
        val header = if (item.isCombo) {
            val descriptor = if (sizeUpper.isBlank()) "PIZZA COMBINADA" else "${sizeUpper} COMBINADA"
            "${item.cantidad}x $descriptor"
        } else {
            val baseName = item.pizza?.nombre ?: "Pizza"
            if (sizeUpper.isBlank()) "${item.cantidad}x ${baseName}" else "${item.cantidad}x ${baseName} ${sizeUpper}"
        }
        lines.add(header)

        if (item.isGolden) {
            lines.add("   * DORADITA")
        }

        if (item.isCombo) {
            item.portions.forEach { portion ->
                lines.add("   ${portion.fraction.label} ${portion.pizzaName}")
                val pizza = MenuData.pizzas.firstOrNull { it.nombre == portion.pizzaName }
                pizza?.ingredientesBaseIds?.forEach { ingredienteId ->
                    MenuData.ingredientes.find { it.id == ingredienteId }?.let { ingrediente ->
                        lines.add("      - ${ingrediente.nombre}")
                    }
                }
            }
        } else {
            val pizza = item.pizza
            if (pizza != null) {
                pizza.ingredientesBaseIds.forEach { ingredienteId ->
                    MenuData.ingredientes.find { it.id == ingredienteId }?.let { ingrediente ->
                        lines.add("   - ${ingrediente.nombre}")
                    }
                }
            }
        }
        return lines
    }
}
