package com.alos895.simplepos.data.datasource

import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.TamanoPizza

object MenuData {
    val pizzas = listOf(
        Pizza(
            nombre = "Pepperoni",
            tamanos = listOf(
                TamanoPizza("Chica", 69.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0)), listOf()),
                TamanoPizza("Mediana", 139.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0)), listOf()),
                TamanoPizza("Extra Grande", 179.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0)), listOf())
            )
        ),
        Pizza(
            nombre = "Pepperoni Champiñones",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Champiñones", 0.0, 0.0)), listOf()),
                TamanoPizza("Mediana", 159.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Champiñones", 0.0, 0.0)), listOf()),
                TamanoPizza("Extra Grande", 199.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Champiñones", 0.0, 0.0)), listOf())
            )
        ),
        Pizza(
            nombre = "Hawaiana",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(Ingrediente("Jamón", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf()),
                TamanoPizza("Mediana", 159.0, listOf(Ingrediente("Jamón", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf()),
                TamanoPizza("Extra Grande", 199.0, listOf(Ingrediente("Jamón", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf())
            )
        ),
        Pizza(
            nombre = "Red Hawaiana",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf()),
                TamanoPizza("Mediana", 159.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf()),
                TamanoPizza("Extra Grande", 199.0, listOf(Ingrediente("Pepperoni", 0.0, 0.0), Ingrediente("Piña", 0.0, 0.0)), listOf())
            )
        ),
        Pizza(
            nombre = "Mexicana",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jitomate", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 169.0, listOf(
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jitomate", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 209.0, listOf(
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jitomate", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf())
            )
        ),
        Pizza(
            nombre = "Vegetariana",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Piña", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 169.0, listOf(
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Piña", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 209.0, listOf(
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Piña", 0.0, 0.0)
                ), listOf())
            )
        ),
        Pizza(
            nombre = "Margarita",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Queso extra", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Albahaca", 0.0, 0.0),
                    Ingrediente("Queso parmesano", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 179.0, listOf(
                    Ingrediente("Queso extra", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Albahaca", 0.0, 0.0),
                    Ingrediente("Queso parmesano", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 219.0, listOf(
                    Ingrediente("Queso extra", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Albahaca", 0.0, 0.0),
                    Ingrediente("Queso parmesano", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Mamma-Mía
        Pizza(
            nombre = "Mamma-Mía",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Jitomate deshidratado", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Albahaca fresca", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 189.0, listOf(
                    Ingrediente("Jitomate deshidratado", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Albahaca fresca", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 229.0, listOf(
                    Ingrediente("Jitomate deshidratado", 0.0, 0.0),
                    Ingrediente("Jitomate cherry", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Albahaca fresca", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Diávola
        Pizza(
            nombre = "Diávola",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 189.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 229.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Exótica
        Pizza(
            nombre = "Exótica",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Aceitunas negras", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 189.0, listOf(
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Aceitunas negras", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 229.0, listOf(
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Aceitunas negras", 0.0, 0.0),
                    Ingrediente("Pimiento", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Frijoleña
        Pizza(
            nombre = "Frijoleña",
            tamanos = listOf(
                TamanoPizza("Chica", 79.0, listOf(
                    Ingrediente("Frijoles refritos", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 189.0, listOf(
                    Ingrediente("Frijoles refritos", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 229.0, listOf(
                    Ingrediente("Frijoles refritos", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Carroñera
        Pizza(
            nombre = "Carroñera",
            tamanos = listOf(
                TamanoPizza("Chica", 89.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Salchicha", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 199.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Salchicha", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 249.0, listOf(
                    Ingrediente("Pepperoni", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Jamón", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Salchicha", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Wera
        Pizza(
            nombre = "Wera",
            tamanos = listOf(
                TamanoPizza("Chica", 89.0, listOf(
                    Ingrediente("Frijoles fritos", 0.0, 0.0),
                    Ingrediente("Chicharrón de cerdo", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Chile güero", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 199.0, listOf(
                    Ingrediente("Frijoles fritos", 0.0, 0.0),
                    Ingrediente("Chicharrón de cerdo", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Chile güero", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 249.0, listOf(
                    Ingrediente("Frijoles fritos", 0.0, 0.0),
                    Ingrediente("Chicharrón de cerdo", 0.0, 0.0),
                    Ingrediente("Chorizo", 0.0, 0.0),
                    Ingrediente("Chile güero", 0.0, 0.0),
                    Ingrediente("Jalapeño", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Boneless-Búfalo
        Pizza(
            nombre = "Boneless-Búfalo",
            tamanos = listOf(
                TamanoPizza("Chica", 89.0, listOf(
                    Ingrediente("Búfalo boneless", 0.0, 0.0),
                    Ingrediente("Apio", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 199.0, listOf(
                    Ingrediente("Búfalo boneless", 0.0, 0.0),
                    Ingrediente("Apio", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 249.0, listOf(
                    Ingrediente("Búfalo boneless", 0.0, 0.0),
                    Ingrediente("Apio", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Boneless-BBQ
        Pizza(
            nombre = "Boneless-BBQ",
            tamanos = listOf(
                TamanoPizza("Chica", 89.0, listOf(
                    Ingrediente("BBQ boneless", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 199.0, listOf(
                    Ingrediente("BBQ boneless", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 249.0, listOf(
                    Ingrediente("BBQ boneless", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0)
                ), listOf())
            )
        ),
        // Pizza-Dogo
        Pizza(
            nombre = "Pizza-Dogo",
            tamanos = listOf(
                TamanoPizza("Chica", 89.0, listOf(
                    Ingrediente("Salchicha", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Mediana", 199.0, listOf(
                    Ingrediente("Salchicha", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf()),
                TamanoPizza("Extra Grande", 249.0, listOf(
                    Ingrediente("Salchicha", 0.0, 0.0),
                    Ingrediente("Salami", 0.0, 0.0),
                    Ingrediente("Tocino", 0.0, 0.0),
                    Ingrediente("Cebolla", 0.0, 0.0),
                    Ingrediente("Champiñón", 0.0, 0.0),
                    Ingrediente("Aderezo ranch", 0.0, 0.0)
                ), listOf())
            )
        )
    )
}