package com.alos895.simplepos.data

import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.TamanoPizza

object MenuData {
    val pizzas = listOf(
        Pizza(
            nombre = "Mexicana",
            tamanos = listOf(
                TamanoPizza(
                    nombre = "Individual",
                    precioBase = 18.55,
                    ingredientesBase = listOf(
                        Ingrediente("Chorizo", 0.015, 2.78),
                        Ingrediente("Jitomate", 0.025, 0.58),
                        Ingrediente("Cebolla morada", 0.01, 0.31),
                        Ingrediente("Pimiento morron", 0.01, 1.50),
                        Ingrediente("Chile jalapeño", 0.01, 0.38)
                    ),
                    ingredientesOpcionales = listOf()
                ),
                TamanoPizza(
                    nombre = "Mediana",
                    precioBase = 39.01,
                    ingredientesBase = listOf(
                        Ingrediente("Chorizo", 0.03, 5.04),
                        Ingrediente("Jitomate", 0.0, 0.0),
                        Ingrediente("Cebolla morada", 0.0, 0.0),
                        Ingrediente("Pimiento morron", 0.0, 0.0),
                        Ingrediente("Chile jalapeño", 0.0, 0.0)
                    ),
                    ingredientesOpcionales = listOf()
                ),
                TamanoPizza(
                    nombre = "Grande",
                    precioBase = 72.32,
                    ingredientesBase = listOf(
                        Ingrediente("Chorizo", 0.0, 0.0),
                        Ingrediente("Jitomate", 0.0, 0.0),
                        Ingrediente("Cebolla morada", 0.0, 0.0),
                        Ingrediente("Pimiento morron", 0.0, 0.0),
                        Ingrediente("Chile jalapeño", 0.0, 0.0)
                    ),
                    ingredientesOpcionales = listOf()
                )
            )
        ),
        Pizza(
            nombre = "Carroñera",
            tamanos = listOf(
                TamanoPizza(
                    nombre = "Individual",
                    precioBase = 18.55,
                    ingredientesBase = listOf(
                        Ingrediente("Pepperoni", 0.0, 0.0),
                        Ingrediente("Salami", 0.0, 0.0),
                        Ingrediente("Jamón", 0.0, 0.0),
                        Ingrediente("Tocino", 0.0, 0.0),
                        Ingrediente("Chorizo", 0.0, 0.0),
                        Ingrediente("Salchicha", 0.0, 0.0)
                    ),
                    ingredientesOpcionales = listOf()
                ),
                TamanoPizza(
                    nombre = "Mediana",
                    precioBase = 39.01,
                    ingredientesBase = listOf(
                        Ingrediente("Pepperoni", 0.032, 5.98),
                        Ingrediente("Salami", 0.036, 12.18),
                        Ingrediente("Jamón", 0.0, 0.0),
                        Ingrediente("Tocino", 0.03, 3.62),
                        Ingrediente("Chorizo", 0.03, 5.04),
                        Ingrediente("Salchicha", 0.03, 1.65)
                    ),
                    ingredientesOpcionales = listOf()
                ),
                TamanoPizza(
                    nombre = "Grande",
                    precioBase = 72.32,
                    ingredientesBase = listOf(
                        Ingrediente("Pepperoni", 0.082, 15.31),
                        Ingrediente("Salami", 0.082, 27.78),
                        Ingrediente("Jamón", 0.0, 0.0),
                        Ingrediente("Tocino", 0.04, 5.76),
                        Ingrediente("Chorizo", 0.04, 6.76),
                        Ingrediente("Salchicha", 0.04, 4.40)
                    ),
                    ingredientesOpcionales = listOf()
                )
            )
        )
    )
} 