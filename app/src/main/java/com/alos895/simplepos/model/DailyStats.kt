package com.alos895.simplepos.model

data class DailyStats(
    val pizzas: Int,
    val postres: Int,
    val ordenes: Int,
    val envios: Int,
    val ingresos: Double
)
