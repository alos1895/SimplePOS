package com.alos895.simplepos.model

data class Pizza(
    val nombre: String,
    val ingredientesBase: List<Ingrediente>,
    val tamanos: List<TamanoPizza>
) 