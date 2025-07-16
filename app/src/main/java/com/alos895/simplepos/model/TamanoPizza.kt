package com.alos895.simplepos.model

data class TamanoPizza(
    val nombre: String,
    val precioBase: Double,
    val ingredientesBase: List<Ingrediente>,
    val ingredientesOpcionales: List<IngredienteOpcional>
) 