package com.alos895.simplepos.data

import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.Ingrediente

object MenuData {
    val pizzas = listOf(
        Pizza(
            nombre = "Margarita",
            tamano = "Individual",
            precioBase = 18.55,
            ingredientes = listOf(
                Ingrediente("Queso Mozzarella"),
                Ingrediente("Salsa de tomate")
            )
        ),
        Pizza(
            nombre = "Pepperoni",
            tamano = "Mediana",
            precioBase = 39.01,
            ingredientes = listOf(
                Ingrediente("Queso Mozzarella"),
                Ingrediente("Pepperoni", 0.03, 5.61)
            )
        ),
        Pizza(
            nombre = "Hawaiana",
            tamano = "Grande",
            precioBase = 72.32,
            ingredientes = listOf(
                Ingrediente("Queso Mozzarella"),
                Ingrediente("Jamon", 0.1, 7.41),
                Ingrediente("Piña", 0.1, 7.41)
            )
        )
    )
} 