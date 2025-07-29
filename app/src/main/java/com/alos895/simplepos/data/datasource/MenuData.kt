package com.alos895.simplepos.data.datasource

import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.TamanoPizza

object MenuData {
    val pizzas = listOf(
        Pizza(
            nombre = "Pepperoni",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pepperoni", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 69.0),
                TamanoPizza("Mediana", 139.0),
                TamanoPizza("Extra Grande", 179.0)
            )
        ),
        Pizza(
            nombre = "Pepperoni Champiñones",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pepperoni", 0.0, 0.0),
                Ingrediente("Champiñones", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Hawaiana",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Jamón", 0.0, 0.0),
                Ingrediente("Piña", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Red Hawaiana",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pepperoni", 0.0, 0.0),
                Ingrediente("Piña", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Mexicana",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Chorizo", 0.0, 0.0),
                Ingrediente("Jitomate", 0.0, 0.0),
                Ingrediente("Pimiento", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Jalapeño", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 169.0),
                TamanoPizza("Extra Grande", 209.0)
            )
        ),
        Pizza(
            nombre = "Vegetariana",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pimiento", 0.0, 0.0),
                Ingrediente("Champiñón", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Jalapeño", 0.0, 0.0),
                Ingrediente("Piña", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 169.0),
                TamanoPizza("Extra Grande", 209.0)
            )
        ),
        Pizza(
            nombre = "Margarita",
            ingredientesBase = listOf(
                Ingrediente("Queso extra", 0.0, 0.0),
                Ingrediente("Jitomate cherry", 0.0, 0.0),
                Ingrediente("Albahaca", 0.0, 0.0),
                Ingrediente("Queso parmesano", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 179.0),
                TamanoPizza("Extra Grande", 219.0)
            )
        ),
        // Mamma-Mía
        Pizza(
            nombre = "Mamma-Mía",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Jitomate deshidratado", 0.0, 0.0),
                Ingrediente("Jitomate cherry", 0.0, 0.0),
                Ingrediente("Champiñón", 0.0, 0.0),
                Ingrediente("Albahaca fresca", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        // Diávola
        Pizza(
            nombre = "Diávola",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pepperoni", 0.0, 0.0),
                Ingrediente("Chorizo", 0.0, 0.0),
                Ingrediente("Jalapeño", 0.0, 0.0),
                Ingrediente("Pimiento", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        // Exótica
        Pizza(
            nombre = "Exótica",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Jamón", 0.0, 0.0),
                Ingrediente("Salami", 0.0, 0.0),
                Ingrediente("Aceitunas negras", 0.0, 0.0),
                Ingrediente("Pimiento", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        // Frijoleña
        Pizza(
            nombre = "Frijoleña",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Frijoles refritos", 0.0, 0.0),
                Ingrediente("Chorizo", 0.0, 0.0),
                Ingrediente("Tocino", 0.0, 0.0),
                Ingrediente("Jalapeño", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        // Carroñera
        Pizza(
            nombre = "Carroñera",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Pepperoni", 0.0, 0.0),
                Ingrediente("Salami", 0.0, 0.0),
                Ingrediente("Jamón", 0.0, 0.0),
                Ingrediente("Tocino", 0.0, 0.0),
                Ingrediente("Chorizo", 0.0, 0.0),
                Ingrediente("Salchicha", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        // Wera
        Pizza(
            nombre = "Wera",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Frijoles fritos", 0.0, 0.0),
                Ingrediente("Chicharrón de cerdo", 0.0, 0.0),
                Ingrediente("Chorizo", 0.0, 0.0),
                Ingrediente("Chile güero", 0.0, 0.0),
                Ingrediente("Jalapeño", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        // Boneless-Búfalo
        Pizza(
            nombre = "Boneless-Búfalo",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Búfalo boneless", 0.0, 0.0),
                Ingrediente("Apio", 0.0, 0.0),
                Ingrediente("Aderezo ranch", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        // Boneless-BBQ
        Pizza(
            nombre = "Boneless-BBQ",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("BBQ boneless", 0.0, 0.0),
                Ingrediente("Champiñón", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        // Pizza-Dogo
        Pizza(
            nombre = "Pizza-Dogo",
            ingredientesBase = listOf(
                Ingrediente("Queso", 0.0, 0.0),
                Ingrediente("Salchicha", 0.0, 0.0),
                Ingrediente("Salami", 0.0, 0.0),
                Ingrediente("Tocino", 0.0, 0.0),
                Ingrediente("Cebolla", 0.0, 0.0),
                Ingrediente("Champiñón", 0.0, 0.0),
                Ingrediente("Aderezo ranch", 0.0, 0.0),
                Ingrediente("Salsa", 0.0, 0.0)
            ),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        )
    )

    val deliveryOptions = listOf(
        DeliveryService(
            price = 0,
            zona = "Sin entrega",
            description = "Recoge tu pedido en la pizzería."
        ),
        DeliveryService(
            price = 15,
            zona = "Zona 1",
            description = ""
        ),
        DeliveryService(
            price = 20,
            zona = "Zona 2",
            description = ""
        ),
        DeliveryService(
            price = 25,
            zona = "Zona 3",
            description = ""
        ),
        DeliveryService(
            price = 30,
            zona = "Zona 4",
            description = ""
        ),
        DeliveryService(
            price = 35,
            zona = "Zona 5",
            description = ""
        )
    )
}