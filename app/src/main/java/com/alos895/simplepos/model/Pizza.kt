package com.alos895.simplepos.model

data class Pizza(
    val nombre: String,
    val tamano: String,
    val precioBase: Double,
    val ingredientes: List<Ingrediente>
) 