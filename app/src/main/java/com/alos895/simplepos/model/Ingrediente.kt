package com.alos895.simplepos.model

data class Ingrediente(
    val id: Int,
    val nombre: String,
    val precioExtra: Double = 0.0,
    val precioBase: Double = 0.0,
    val preciExtraChica: Double = 0.0,
    val precioExtraMediana: Double = 0.0,
    val precioExtraGrande: Double = 0.0
)
