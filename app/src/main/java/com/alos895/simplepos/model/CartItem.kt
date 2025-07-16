package com.alos895.simplepos.model

data class CartItem(
    val pizza: Pizza,
    val tamano: TamanoPizza,
    val cantidad: Int = 1
) {
    val subtotal: Double get() = tamano.precioBase * cantidad
} 