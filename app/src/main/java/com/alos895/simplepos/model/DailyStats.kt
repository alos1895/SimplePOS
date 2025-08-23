package com.alos895.simplepos.model

data class DailyStats(
    val pizzas: Int,
    val pizzasChicas: Int,
    val pizzasMedianas: Int,
    val pizzasGrandes: Int,
    val postres: Int,
    val ordenes: Int,
    val envios: Int,
    val ingresos: Double,
    val extras: Int,
    val ingresosPizzas: Double,
    val ingresosPostres: Double,
    val ingresosExtras: Double,
    val ingresosEnvios: Double
)
