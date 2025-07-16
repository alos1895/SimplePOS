package com.alos895.simplepos.model

data class CartItem(
    val pizza: Pizza,
    val cantidad: Int = 1
) {
    val subtotal: Double get() = pizza.precioBase * cantidad
} 