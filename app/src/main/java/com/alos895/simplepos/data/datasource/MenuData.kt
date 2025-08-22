package com.alos895.simplepos.data.datasource

import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.model.PostreOrExtra

object MenuData {
    val ingredientes = listOf(
        Ingrediente(1, "Queso", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(2, "Pepperoni", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(3, "Salsa", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(4, "Champiñones", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(5, "Jamón", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(6, "Piña", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(7, "Chorizo", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(8, "Jitomate", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(9, "Pimiento", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(10, "Cebolla", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(11, "Jalapeño", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(12, "Queso extra", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(13, "Jitomate cherry", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(14, "Albahaca", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(15, "Queso parmesano", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(16, "Jitomate deshidratado", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(17, "Albahaca fresca", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(18, "Salami", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(19, "Aceitunas negras", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(20, "Frijoles refritos", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(21, "Tocino", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(22, "Salchicha", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(23, "Frijoles fritos", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(24, "Chicharrón de cerdo", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(25, "Chile güero", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(26, "Búfalo boneless", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(27, "Apio", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(28, "Aderezo ranch", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0),
        Ingrediente(29, "BBQ boneless", preciExtraChica = 0.0, precioExtraMediana = 0.0, precioExtraGrande = 0.0)
    )

    val pizzas = listOf(
        Pizza(
            nombre = "Pepperoni",
            ingredientesBaseIds = listOf(1, 2, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 69.0),
                TamanoPizza("Mediana", 139.0),
                TamanoPizza("Extra Grande", 179.0)
            )
        ),
        Pizza(
            nombre = "Pepperoni Champiñones",
            ingredientesBaseIds = listOf(1, 2, 4, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Hawaiana",
            ingredientesBaseIds = listOf(1, 5, 6, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Red Hawaiana",
            ingredientesBaseIds = listOf(1, 2, 6, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 159.0),
                TamanoPizza("Extra Grande", 199.0)
            )
        ),
        Pizza(
            nombre = "Mexicana",
            ingredientesBaseIds = listOf(1, 7, 8, 9, 10, 11, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 169.0),
                TamanoPizza("Extra Grande", 209.0)
            )
        ),
        Pizza(
            nombre = "Vegetariana",
            ingredientesBaseIds = listOf(1, 9, 4, 10, 11, 6, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 169.0),
                TamanoPizza("Extra Grande", 209.0)
            )
        ),
        Pizza(
            nombre = "Margarita",
            ingredientesBaseIds = listOf(12, 13, 14, 15, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 179.0),
                TamanoPizza("Extra Grande", 219.0)
            )
        ),
        Pizza(
            nombre = "Mamma-Mía",
            ingredientesBaseIds = listOf(1, 16, 13, 4, 17, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        Pizza(
            nombre = "Diávola",
            ingredientesBaseIds = listOf(1, 2, 7, 11, 9, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        Pizza(
            nombre = "Exótica",
            ingredientesBaseIds = listOf(1, 5, 18, 19, 9, 10, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        Pizza(
            nombre = "Frijoleña",
            ingredientesBaseIds = listOf(1, 20, 7, 21, 11, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 79.0),
                TamanoPizza("Mediana", 189.0),
                TamanoPizza("Extra Grande", 229.0)
            )
        ),
        Pizza(
            nombre = "Carroñera",
            ingredientesBaseIds = listOf(1, 2, 18, 5, 21, 7, 22, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        Pizza(
            nombre = "Wera",
            ingredientesBaseIds = listOf(1, 23, 24, 7, 25, 11, 10, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        Pizza(
            nombre = "Boneless-Búfalo",
            ingredientesBaseIds = listOf(1, 26, 27, 28, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        Pizza(
            nombre = "Boneless-BBQ",
            ingredientesBaseIds = listOf(1, 29, 4, 10, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        ),
        Pizza(
            nombre = "Pizza-Dogo",
            ingredientesBaseIds = listOf(1, 22, 18, 21, 10, 4, 28, 3),
            tamanos = listOf(
                TamanoPizza("Chica", 89.0),
                TamanoPizza("Mediana", 199.0),
                TamanoPizza("Extra Grande", 249.0)
            )
        )
    )

    val postreOrExtras = listOf(
        PostreOrExtra(1, "Postre 1", 20.0),
        PostreOrExtra(2, "Postre 2", 30.0),
        PostreOrExtra(3, "Postre 3", 45.0),
        PostreOrExtra(4, "Chimi Mediano", 10.0),
        PostreOrExtra(5, "Chimi Grande", 20.0),
        PostreOrExtra(6, "Aderezo Ranch", 15.0),
        PostreOrExtra(7, "Salsa Habanera", 5.0)
    )

    val deliveryOptions = listOf(
        DeliveryService(
            price = 0,
            zona = "Sin entrega",
            description = "Recoge tu pedido en la pizzería."
        ),
        DeliveryService(
            price = 25,
            zona = "Zona 1",
            description = ""
        ),
        DeliveryService(
            price = 30,
            zona = "Zona 2",
            description = ""
        ),
        DeliveryService(
            price = 35,
            zona = "Zona 3",
            description = ""
        ),
        DeliveryService(
            price = 40,
            zona = "Zona 4",
            description = ""
        )
    )
}