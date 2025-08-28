package com.alos895.simplepos.model

data class DailyStats(
    val pizzas: Int = 0,
    val pizzasChicas: Int = 0,
    val pizzasMedianas: Int = 0,
    val pizzasGrandes: Int = 0,
    val postres: Int = 0,
    val ordenes: Int = 0,
    val envios: Int = 0,
    val totalCaja: Double = 0.0,
    val extras: Int = 0,
    val ingresosPizzas: Double = 0.0,
    val ingresosPostres: Double = 0.0,
    val ingresosExtras: Double = 0.0,
    val ingresosEnvios: Double = 0.0,
    val ingresosCapturados: Double = 0.0,
    val egresosCapturados: Double = 0.0,
    val totalOrdenesEfectivo: Double = 0.0,
    val totalOrdenesTarjeta: Double = 0.0
)
