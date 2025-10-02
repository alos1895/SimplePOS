package com.alos895.simplepos.model

data class Pizza(
    val nombre: String,
    val ingredientesBaseIds: List<Int>,
    val tamanos: List<TamanoPizza>,
    val esCombinable: Boolean = true
)
